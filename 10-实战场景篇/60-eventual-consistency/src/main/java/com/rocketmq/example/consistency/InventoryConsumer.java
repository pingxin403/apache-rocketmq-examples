package com.rocketmq.example.consistency;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 库存服务 —— 消费库存扣减消息，保证最终一致性
 *
 * 核心保证：
 *   1. 幂等消费：同一个 orderId 只扣减一次库存
 *   2. 本地事务：扣减库存 + 记录消费日志在同一个事务中
 *   3. 失败重试：消费失败返回 RECONSUME_LATER，RocketMQ 自动重试
 *
 * 幂等实现方案（生产环境推荐）：
 *   方案A：数据库唯一键 —— INSERT INTO consume_log(order_id) 去重
 *   方案B：Redis SETNX —— SETNX consume:orderId 1 EX 86400
 *   方案C：状态机 —— 检查订单状态是否已扣减
 *
 * 本示例使用内存 ConcurrentHashMap 模拟方案A
 */
public class InventoryConsumer {

    /** 模拟商品库存表：productId -> 库存数量 */
    private static final ConcurrentHashMap<String, AtomicInteger> STOCK_TABLE = new ConcurrentHashMap<>();

    /** 模拟消费日志表（去重表）：orderId -> 是否已处理 */
    private static final ConcurrentHashMap<String, Boolean> CONSUME_LOG = new ConcurrentHashMap<>();

    static {
        // 初始化库存
        STOCK_TABLE.put("SKU_001", new AtomicInteger(100));
        STOCK_TABLE.put("SKU_002", new AtomicInteger(50));
        STOCK_TABLE.put("SKU_003", new AtomicInteger(200));
    }

    public static void main(String[] args) throws Exception {
        // ========== 1. 创建消费者 ==========
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("inventory_consumer_group");
        consumer.setNamesrvAddr("localhost:9876");

        // 订阅库存扣减 Topic
        consumer.subscribe("InventoryDeductTopic", "Deduct");

        // 设置最大重试次数
        consumer.setMaxReconsumeTimes(5);

        // ========== 2. 注册消息监听器 ==========
        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            for (MessageExt msg : msgs) {
                String orderId = msg.getKeys();
                String body = new String(msg.getBody());

                System.out.println("\n========================================");
                System.out.printf("[收到消息] orderId=%s, msgId=%s, 重试=%d%n",
                        orderId, msg.getMsgId(), msg.getReconsumeTimes());
                System.out.printf("[消息内容] %s%n", body);

                // ========== 幂等检查 ==========
                if (CONSUME_LOG.putIfAbsent(orderId, true) != null) {
                    System.out.printf("[幂等] 订单 %s 已处理过，跳过%n", orderId);
                    continue;
                }

                try {
                    // ========== 模拟本地事务：扣减库存 ==========
                    // 实际场景中应该是：
                    // BEGIN TRANSACTION
                    //   UPDATE stock SET qty = qty - #{quantity} WHERE product_id = #{productId} AND qty >= #{quantity}
                    //   INSERT INTO consume_log(order_id) VALUES(#{orderId})
                    // COMMIT

                    String productId = parseField(body, "productId");
                    int quantity = Integer.parseInt(parseField(body, "quantity"));

                    boolean success = deductStock(productId, quantity);

                    if (success) {
                        AtomicInteger remaining = STOCK_TABLE.get(productId);
                        System.out.printf("[扣减成功] orderId=%s, product=%s, qty=-%d, 剩余=%d%n",
                                orderId, productId, quantity,
                                remaining != null ? remaining.get() : 0);
                    } else {
                        // 库存不足，记录异常，人工介入或触发补偿
                        CONSUME_LOG.remove(orderId); // 允许重试
                        System.err.printf("[扣减失败] orderId=%s, product=%s, 库存不足%n",
                                orderId, productId);
                        // 实际场景中可以发送补偿消息通知订单服务取消订单
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }

                } catch (Exception e) {
                    CONSUME_LOG.remove(orderId); // 异常时移除去重标记，允许重试
                    System.err.printf("[异常] orderId=%s, error=%s%n", orderId, e.getMessage());
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        // ========== 3. 启动消费者 ==========
        consumer.start();
        System.out.println("库存服务（消费者）已启动");
        System.out.println("初始库存: " + STOCK_TABLE);
        System.out.println("等待库存扣减消息...\n");

        // 保持进程运行
        Thread.sleep(Long.MAX_VALUE);
    }

    /**
     * 扣减库存（CAS 操作，线程安全）
     *
     * @param productId 商品ID
     * @param quantity  扣减数量
     * @return true-扣减成功，false-库存不足
     */
    private static boolean deductStock(String productId, int quantity) {
        AtomicInteger stock = STOCK_TABLE.get(productId);
        if (stock == null) {
            System.err.printf("[错误] 商品不存在: %s%n", productId);
            return false;
        }

        // CAS 扣减，防止超卖
        while (true) {
            int current = stock.get();
            if (current < quantity) {
                return false; // 库存不足
            }
            if (stock.compareAndSet(current, current - quantity)) {
                return true; // 扣减成功
            }
            // CAS 失败，重试
        }
    }

    /**
     * 简单 JSON 字段解析
     * 从 {"key":"value"} 或 {"key":123} 中提取值
     */
    private static String parseField(String json, String field) {
        String search = "\"" + field + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return "";

        String sub = json.substring(idx + search.length()).trim();
        if (sub.startsWith("\"")) {
            // 字符串值
            int end = sub.indexOf("\"", 1);
            return sub.substring(1, end);
        } else {
            // 数字值
            int end = sub.indexOf(",");
            if (end < 0) end = sub.indexOf("}");
            return sub.substring(0, end).trim();
        }
    }
}

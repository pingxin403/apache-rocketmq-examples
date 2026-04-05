package com.rocketmq.example.consistency;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 订单服务 —— 事务消息保证下单与库存扣减的最终一致性
 *
 * 业务场景：
 *   电商系统中，订单服务和库存服务是两个独立的微服务。
 *   下单时需要保证：「创建订单」和「扣减库存」要么都成功，要么都失败。
 *
 * 最终一致性方案（基于事务消息）：
 *   1. 订单服务发送半消息（库存扣减指令）
 *   2. 执行本地事务：创建订单 + 记录事务日志
 *   3. 本地事务成功 -> COMMIT 消息 -> 库存服务消费并扣减
 *   4. 本地事务失败 -> ROLLBACK 消息 -> 库存不受影响
 *   5. 超时未确认 -> Broker 回查本地事务表
 *
 * 与强一致性（2PC）的区别：
 *   - 不锁定库存资源，性能更高
 *   - 允许短暂不一致，最终达到一致
 *   - 通过消息重试 + 幂等消费保证可靠性
 */
public class OrderProducer {

    /** 模拟订单表：orderId -> 订单状态 */
    private static final ConcurrentHashMap<String, String> ORDER_TABLE = new ConcurrentHashMap<>();

    /** 模拟本地事务日志表：orderId -> 事务状态 */
    private static final ConcurrentHashMap<String, String> TX_LOG_TABLE = new ConcurrentHashMap<>();

    /** 订单号生成器 */
    private static final AtomicLong ORDER_SEQ = new AtomicLong(100000);

    public static void main(String[] args) throws Exception {
        // ========== 1. 创建事务消息生产者 ==========
        TransactionMQProducer producer = new TransactionMQProducer("order_tx_producer_group");
        producer.setNamesrvAddr("localhost:9876");

        // ========== 2. 注册事务监听器 ==========
        producer.setTransactionListener(new TransactionListener() {

            /**
             * 执行本地事务：创建订单
             */
            @Override
            public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
                String orderId = msg.getKeys();
                String productId = msg.getUserProperty("productId");
                int quantity = Integer.parseInt(msg.getUserProperty("quantity"));

                System.out.printf("[本地事务] 开始创建订单: orderId=%s, productId=%s, qty=%d%n",
                        orderId, productId, quantity);

                try {
                    // ---------- 模拟本地事务（实际场景中是数据库事务） ----------
                    // BEGIN TRANSACTION

                    // 1. 插入订单记录
                    ORDER_TABLE.put(orderId, "CREATED");
                    System.out.printf("[本地事务] 订单已创建: orderId=%s%n", orderId);

                    // 2. 记录事务日志（用于回查）
                    TX_LOG_TABLE.put(orderId, "COMMIT");
                    System.out.printf("[本地事务] 事务日志已记录: orderId=%s%n", orderId);

                    // COMMIT TRANSACTION

                    // 模拟第3笔订单异常（用于演示回查机制）
                    if (orderId.endsWith("3")) {
                        System.out.printf("[本地事务] 模拟超时，返回 UNKNOWN: orderId=%s%n", orderId);
                        return LocalTransactionState.UNKNOW;
                    }

                    System.out.printf("[本地事务] 提交成功: orderId=%s%n", orderId);
                    return LocalTransactionState.COMMIT_MESSAGE;

                } catch (Exception e) {
                    // 本地事务失败，回滚
                    ORDER_TABLE.remove(orderId);
                    TX_LOG_TABLE.put(orderId, "ROLLBACK");
                    System.err.printf("[本地事务] 创建订单失败，回滚: orderId=%s, error=%s%n",
                            orderId, e.getMessage());
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
            }

            /**
             * 事务回查：查询本地事务日志表
             */
            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                String orderId = msg.getKeys();
                String txStatus = TX_LOG_TABLE.getOrDefault(orderId, "UNKNOWN");
                System.out.printf("[事务回查] orderId=%s, 事务日志状态=%s%n", orderId, txStatus);

                switch (txStatus) {
                    case "COMMIT":
                        return LocalTransactionState.COMMIT_MESSAGE;
                    case "ROLLBACK":
                        return LocalTransactionState.ROLLBACK_MESSAGE;
                    default:
                        // 事务日志中无记录，说明本地事务可能还在执行
                        // 返回 UNKNOW 等待下次回查
                        return LocalTransactionState.UNKNOW;
                }
            }
        });

        // ========== 3. 启动生产者 ==========
        producer.start();
        System.out.println("订单服务（事务消息生产者）已启动\n");

        // ========== 4. 模拟下单 ==========
        String[] products = {"SKU_001", "SKU_002", "SKU_003"};
        int[] quantities = {1, 2, 3};

        for (int i = 0; i < products.length; i++) {
            String orderId = "ORD_" + ORDER_SEQ.incrementAndGet();
            String productId = products[i];
            int quantity = quantities[i];

            // 构建库存扣减消息
            String body = String.format(
                    "{\"orderId\":\"%s\",\"productId\":\"%s\",\"quantity\":%d}",
                    orderId, productId, quantity);

            Message msg = new Message("InventoryDeductTopic", "Deduct",
                    orderId, body.getBytes());
            msg.putUserProperty("productId", productId);
            msg.putUserProperty("quantity", String.valueOf(quantity));

            System.out.println("========================================");
            System.out.printf("[下单] orderId=%s, product=%s, qty=%d%n",
                    orderId, productId, quantity);

            TransactionSendResult result = producer.sendMessageInTransaction(msg, null);
            System.out.printf("[发送结果] status=%s, localTx=%s%n",
                    result.getSendStatus(), result.getLocalTransactionState());

            Thread.sleep(1000);
        }

        // 保持进程运行，等待回查
        System.out.println("\n等待事务回查（可按 Ctrl+C 退出）...");
        Thread.sleep(120000);
        producer.shutdown();
    }
}

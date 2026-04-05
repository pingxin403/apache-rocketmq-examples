package com.rocketmq.example.flashsale;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 秒杀消费者 —— 串行扣减库存，展示削峰效果
 *
 * 核心思路：
 *   1. 从 MQ 逐条消费秒杀请求（削峰：将瞬时流量转为平稳消费）
 *   2. 使用 AtomicInteger 模拟库存扣减
 *   3. 库存为 0 后，后续请求直接返回"已售罄"
 *   4. 使用 userId 去重，防止同一用户重复抢购
 *
 * 削峰效果：
 *   - 生产端：100 个请求在 < 1秒 内全部入队
 *   - 消费端：逐条处理，每条模拟 50ms 业务耗时
 *   - 数据库压力从 100 QPS 降低到 ~20 QPS
 */
public class FlashSaleConsumer {

    /** 模拟库存：仅 10 件 */
    private static final AtomicInteger stock = new AtomicInteger(10);

    /** 抢购成功的用户记录 */
    private static final ConcurrentHashMap<String, Boolean> successUsers = new ConcurrentHashMap<>();

    /** 统计计数器 */
    private static final AtomicInteger totalProcessed = new AtomicInteger(0);
    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger soldOutCount = new AtomicInteger(0);
    private static final AtomicInteger duplicateCount = new AtomicInteger(0);

    public static void main(String[] args) throws Exception {
        // ========== 1. 创建消费者 ==========
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("flash_sale_consumer_group");
        consumer.setNamesrvAddr("localhost:9876");

        // 订阅秒杀 Topic
        consumer.subscribe("FlashSaleTopic", "SecKill");

        // 设置每次拉取 1 条消息，模拟串行处理
        consumer.setConsumeMessageBatchMaxSize(1);

        // ========== 2. 注册顺序消费监听器（串行消费，保证库存扣减安全） ==========
        consumer.registerMessageListener(new MessageListenerOrderly() {
            @Override
            public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs,
                                                       ConsumeOrderlyContext context) {
                for (MessageExt msg : msgs) {
                    String userId = msg.getKeys();
                    String body = new String(msg.getBody());
                    int processed = totalProcessed.incrementAndGet();

                    System.out.printf("%n[处理 #%d] userId=%s%n", processed, userId);

                    // ---------- 去重：同一用户只能抢一次 ----------
                    if (successUsers.containsKey(userId)) {
                        duplicateCount.incrementAndGet();
                        System.out.printf("[去重] 用户 %s 已抢购成功，忽略重复请求%n", userId);
                        continue;
                    }

                    // ---------- 库存检查 & 扣减 ----------
                    int remaining = stock.get();
                    if (remaining <= 0) {
                        soldOutCount.incrementAndGet();
                        System.out.printf("[售罄] 库存已空，用户 %s 抢购失败%n", userId);
                        continue;
                    }

                    // CAS 扣减库存
                    if (stock.decrementAndGet() >= 0) {
                        successUsers.put(userId, true);
                        successCount.incrementAndGet();
                        System.out.printf("[成功] 用户 %s 抢购成功！剩余库存: %d%n",
                                userId, stock.get());
                    } else {
                        // CAS 竞争失败，库存已被其他线程扣完
                        stock.incrementAndGet(); // 回补
                        soldOutCount.incrementAndGet();
                        System.out.printf("[售罄] 库存已空，用户 %s 抢购失败%n", userId);
                    }

                    // 模拟业务处理耗时（写数据库、生成订单等）
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                return ConsumeOrderlyStatus.SUCCESS;
            }
        });

        // ========== 3. 启动消费者 ==========
        consumer.start();
        System.out.println("秒杀消费者已启动");
        System.out.printf("初始库存: %d 件%n", stock.get());
        System.out.println("等待秒杀请求...\n");

        // ========== 4. 定时输出统计信息 ==========
        // 等待一段时间后输出最终统计
        Thread.sleep(30000);
        printStatistics();

        // 保持进程运行
        Thread.sleep(Long.MAX_VALUE);
    }

    /** 输出秒杀结果统计 */
    private static void printStatistics() {
        System.out.println("\n========== 秒杀结果统计 ===========");
        System.out.printf("总处理请求: %d%n", totalProcessed.get());
        System.out.printf("抢购成功:   %d%n", successCount.get());
        System.out.printf("库存售罄:   %d%n", soldOutCount.get());
        System.out.printf("重复请求:   %d%n", duplicateCount.get());
        System.out.printf("剩余库存:   %d%n", stock.get());
        System.out.println("====================================");
        System.out.println("成功用户列表: " + successUsers.keySet());
    }
}

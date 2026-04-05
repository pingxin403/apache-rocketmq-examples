package com.rocketmq.example.flashsale;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 秒杀请求生产者 —— 模拟高并发秒杀场景
 *
 * 业务场景：
 *   电商秒杀活动开始后，瞬间涌入大量请求。
 *   如果直接打到数据库，会导致数据库崩溃。
 *   使用 RocketMQ 做「削峰填谷」：
 *     1. 前端请求先写入 MQ（毫秒级响应）
 *     2. 后端消费者从 MQ 串行消费，逐条扣减库存
 *     3. 超出库存的请求直接返回"已售罄"
 *
 * 本示例模拟：
 *   - 100 个并发用户同时抢购
 *   - 库存仅 10 件
 *   - 通过 MQ 削峰，避免数据库被打爆
 */
public class FlashSaleProducer {

    /** 模拟并发用户数 */
    private static final int CONCURRENT_USERS = 100;

    /** 秒杀商品ID */
    private static final String PRODUCT_ID = "SKU_IPHONE_2024";

    public static void main(String[] args) throws Exception {
        // ========== 1. 创建生产者 ==========
        DefaultMQProducer producer = new DefaultMQProducer("flash_sale_producer_group");
        producer.setNamesrvAddr("localhost:9876");
        // 发送超时设置短一些，秒杀场景要求快速响应
        producer.setSendMsgTimeout(3000);
        producer.start();
        System.out.println("秒杀生产者已启动\n");

        // ========== 2. 模拟高并发秒杀请求 ==========
        ExecutorService threadPool = Executors.newFixedThreadPool(20);
        CountDownLatch startGate = new CountDownLatch(1);   // 模拟同时开抢
        CountDownLatch endGate = new CountDownLatch(CONCURRENT_USERS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();
        System.out.printf("========== 秒杀开始！%d 人同时抢购 %s ===========%n%n",
                CONCURRENT_USERS, PRODUCT_ID);

        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final String userId = "USER_" + String.format("%04d", i + 1);

            threadPool.submit(() -> {
                try {
                    // 所有线程在此等待，模拟同时开抢
                    startGate.await();

                    // 构建秒杀消息
                    String body = String.format(
                            "{\"userId\":\"%s\",\"productId\":\"%s\",\"timestamp\":%d}",
                            userId, PRODUCT_ID, System.currentTimeMillis());

                    Message msg = new Message(
                            "FlashSaleTopic",       // Topic
                            "SecKill",              // Tag
                            userId,                 // Key：用户ID，用于去重
                            body.getBytes()
                    );

                    // 发送消息（异步转同步，确保消息写入 MQ）
                    SendResult result = producer.send(msg);
                    successCount.incrementAndGet();
                    System.out.printf("[请求入队] userId=%s, msgId=%s%n",
                            userId, result.getMsgId());

                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.printf("[请求失败] userId=%s, error=%s%n",
                            userId, e.getMessage());
                } finally {
                    endGate.countDown();
                }
            });
        }

        // 发令枪：所有线程同时开始
        startGate.countDown();

        // 等待所有请求完成
        endGate.await();
        long costTime = System.currentTimeMillis() - startTime;

        // ========== 3. 输出统计 ==========
        System.out.println("\n========== 秒杀请求统计 ===========");
        System.out.printf("总请求数: %d%n", CONCURRENT_USERS);
        System.out.printf("入队成功: %d%n", successCount.get());
        System.out.printf("入队失败: %d%n", failCount.get());
        System.out.printf("总耗时:   %d ms%n", costTime);
        System.out.printf("平均 QPS: %.0f%n", CONCURRENT_USERS * 1000.0 / costTime);
        System.out.println("====================================");

        // ========== 4. 关闭 ==========
        threadPool.shutdown();
        producer.shutdown();
        System.out.println("\n秒杀生产者已关闭");
    }
}

package com.rocketmq.example.pop;

import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.SimpleConsumer;
import org.apache.rocketmq.client.apis.message.MessageView;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Pop 消费模式示例 —— 基于 RocketMQ 5.x SimpleConsumer
 *
 * ============================================================
 * Pop 消费的核心特点：
 * ============================================================
 * 1. 无状态（Stateless）
 *    - 消费者不再持有队列的独占锁，任何消费者实例都可以消费任意队列
 *    - 消费者宕机无需触发 Rebalance，其他实例自动接管
 *
 * 2. 服务端负载均衡
 *    - 传统 Push 模式由客户端 Rebalance 分配队列，Pop 模式由 Broker 端统一调度
 *    - 避免了客户端 Rebalance 带来的消费暂停和重复消费问题
 *
 * 3. 支持多语言
 *    - 基于 gRPC 协议，天然支持 Java / Go / C++ / Python / Rust 等多语言客户端
 *    - 不再依赖 Java 特有的 Remoting 协议
 *
 * 4. invisibleDuration（不可见时间）
 *    - 消息被 receive() 拉取后，在 invisibleDuration 内对其他消费者不可见
 *    - 若消费者在此时间内未 ack，消息将重新变为可见，可被其他消费者消费
 *    - 类似于 AWS SQS 的 VisibilityTimeout 机制
 *
 * ============================================================
 * 运行方式：
 * ============================================================
 * 1. 先运行 SimpleProducer 发送测试消息
 * 2. 再运行本类消费消息
 * 注意：需要 RocketMQ 5.x Broker 并开启 gRPC Proxy（默认端口 8081）
 */
public class PopConsumer {

    /** gRPC Proxy 地址（RocketMQ 5.x 新客户端通过 Proxy 连接） */
    private static final String ENDPOINT = "localhost:8081";

    /** 消费者组 */
    private static final String CONSUMER_GROUP = "PopConsumerGroup";

    /** 订阅的 Topic */
    private static final String TOPIC = "PopDemoTopic";

    public static void main(String[] args) throws Exception {
        // ========================================
        // 1. 获取客户端服务提供者（SPI 机制加载）
        // ========================================
        ClientServiceProvider provider = ClientServiceProvider.loadService();

        // ========================================
        // 2. 构建客户端配置（指定 Proxy 接入点）
        // ========================================
        ClientConfiguration clientConfig = ClientConfiguration.newBuilder()
                .setEndpoints(ENDPOINT)
                .build();

        // ========================================
        // 3. 构建过滤表达式（订阅所有 Tag）
        // ========================================
        FilterExpression filterExpression = new FilterExpression("*", FilterExpressionType.TAG);

        // ========================================
        // 4. 创建 SimpleConsumer（Pop 消费的核心）
        // ========================================
        // SimpleConsumer 是 RocketMQ 5.x 引入的新消费者类型
        // 与传统 PushConsumer 的区别：
        //   - 主动调用 receive() 拉取消息，而非被动回调
        //   - 消费者无状态，不绑定特定队列
        //   - 需要手动 ack，超时未 ack 的消息会重新投递
        SimpleConsumer consumer = provider.newSimpleConsumerBuilder()
                .setClientConfiguration(clientConfig)
                .setConsumerGroup(CONSUMER_GROUP)
                // 订阅 Topic 和过滤表达式
                .setSubscriptionExpressions(Collections.singletonMap(TOPIC, filterExpression))
                // 设置不可见时间：消息被拉取后，在此时间内对其他消费者不可见
                // 如果消费者在 30 秒内没有 ack，消息将重新变为可见
                .setAwaitDuration(Duration.ofSeconds(5))
                .build();

        System.out.println("=== Pop 消费者已启动（SimpleConsumer） ===");
        System.out.println("等待消息中...\n");

        // ========================================
        // 5. 循环拉取并消费消息
        // ========================================
        int emptyCount = 0;
        while (emptyCount < 10) {
            try {
                // receive() 主动拉取消息
                // 参数1：最大拉取条数
                // 参数2：不可见时间（invisibleDuration）
                //   - 消息被拉取后，在此时间内不会被其他消费者看到
                //   - 超过此时间未 ack，消息将重新变为可见
                List<MessageView> messages = consumer.receive(
                        16,                          // 每次最多拉取 16 条
                        Duration.ofSeconds(30)       // 不可见时间 30 秒
                );

                if (messages.isEmpty()) {
                    emptyCount++;
                    System.out.println("暂无消息，等待中... (" + emptyCount + "/10)");
                    continue;
                }

                emptyCount = 0; // 重置空轮询计数

                for (MessageView message : messages) {
                    // 打印消息详情
                    System.out.printf("[Pop消费] messageId=%s, topic=%s, tag=%s, body=%s%n",
                            message.getMessageId(),
                            message.getTopic(),
                            message.getTag().orElse("null"),
                            // 读取消息体
                            StandardCharsetsBytesReader.read(message.getBody()));

                    // ========================================
                    // 6. 消费成功后手动 ack
                    // ========================================
                    // ack 是 Pop 消费的关键步骤：
                    //   - ack 成功：Broker 标记消息已消费，不再投递
                    //   - 未 ack：超过 invisibleDuration 后消息重新可见
                    //   - ack 失败：消息会在不可见时间到期后重试
                    consumer.ack(message);
                    System.out.println("  → ack 成功\n");
                }
            } catch (Exception e) {
                System.err.println("消费异常: " + e.getMessage());
                Thread.sleep(1000);
            }
        }

        // ========================================
        // 7. 关闭消费者
        // ========================================
        consumer.close();
        System.out.println("=== Pop 消费者已关闭 ===");
    }

    /**
     * 辅助工具：读取 ByteBuffer 中的消息体
     */
    static class StandardCharsetsBytesReader {
        static String read(java.nio.ByteBuffer buffer) {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}

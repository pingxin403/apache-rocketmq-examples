package com.rocketmq.example.pop;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

import java.nio.charset.StandardCharsets;

/**
 * 配套生产者 —— 使用经典 DefaultMQProducer 发送测试消息
 *
 * 说明：
 * - 使用传统 Remoting 协议客户端发送消息到 PopDemoTopic
 * - Pop 消费模式对生产者透明，生产者无需任何改动
 * - 消息发送到 Broker 后，既可以被 Pop 消费者消费，也可以被 Push 消费者消费
 *
 * 运行顺序：
 * 1. 先运行本类发送消息
 * 2. 再运行 PopConsumer 或 PushConsumer 消费消息
 */
public class SimpleProducer {

    /** NameServer 地址 */
    private static final String NAME_SERVER = "localhost:9876";

    /** 生产者组 */
    private static final String PRODUCER_GROUP = "PopDemoProducerGroup";

    /** 目标 Topic */
    private static final String TOPIC = "PopDemoTopic";

    public static void main(String[] args) throws Exception {
        // ========================================
        // 1. 创建生产者并启动
        // ========================================
        DefaultMQProducer producer = new DefaultMQProducer(PRODUCER_GROUP);
        producer.setNamesrvAddr(NAME_SERVER);
        producer.start();

        System.out.println("=== 生产者已启动，开始发送消息 ===\n");

        // ========================================
        // 2. 发送 20 条测试消息
        // ========================================
        int messageCount = 20;
        for (int i = 0; i < messageCount; i++) {
            String body = "Pop消费测试消息-" + i + " [时间:" + System.currentTimeMillis() + "]";

            Message msg = new Message(
                    TOPIC,                                    // Topic
                    "TagPop",                                 // Tag
                    ("KEY_" + i),                             // Keys（用于消息检索）
                    body.getBytes(StandardCharsets.UTF_8)     // 消息体
            );

            SendResult result = producer.send(msg);
            System.out.printf("发送成功 [%d/%d]: msgId=%s, queue=%s%n",
                    i + 1, messageCount,
                    result.getMsgId(),
                    result.getMessageQueue());
        }

        System.out.println("\n=== 全部消息发送完成 ===");

        // ========================================
        // 3. 关闭生产者
        // ========================================
        producer.shutdown();
    }
}

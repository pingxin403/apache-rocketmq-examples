package com.rocketmq.example.retry;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import java.nio.charset.StandardCharsets;

/**
 * 重试消息生产者
 * 发送消息供 RetryConsumer 消费，模拟消费失败触发重试
 */
public class RetryProducer {
    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("RetryProducerGroup");
        producer.setNamesrvAddr("localhost:9876");
        producer.start();

        Message msg = new Message("RetryTopic", "TagA",
                "这条消息会触发消费重试".getBytes(StandardCharsets.UTF_8));
        SendResult result = producer.send(msg);
        System.out.printf("发送成功: msgId=%s%n", result.getMsgId());

        producer.shutdown();
    }
}

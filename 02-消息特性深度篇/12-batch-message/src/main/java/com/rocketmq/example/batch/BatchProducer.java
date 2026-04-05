package com.rocketmq.example.batch;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量消息发送示例
 * 注意：批量消息要求同一 Topic，且总大小不超过 4MB（可配置）
 */
public class BatchProducer {
    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("BatchProducerGroup");
        producer.setNamesrvAddr("localhost:9876");
        producer.start();

        // 构建批量消息列表
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            messages.add(new Message("BatchTopic", "TagA",
                    ("批量消息-" + i).getBytes(StandardCharsets.UTF_8)));
        }

        // 批量发送（同一 Topic，总大小 < 4MB）
        SendResult result = producer.send(messages);
        System.out.printf("批量发送 %d 条消息成功: status=%s%n",
                messages.size(), result.getSendStatus());

        producer.shutdown();
    }
}

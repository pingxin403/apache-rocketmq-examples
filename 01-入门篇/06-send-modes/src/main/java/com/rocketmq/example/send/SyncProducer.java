package com.rocketmq.example.send;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import java.nio.charset.StandardCharsets;

/**
 * 同步发送示例
 * 特点：发送后等待 Broker 确认，可靠性最高，适合重要业务消息
 */
public class SyncProducer {
    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("SyncProducerGroup");
        producer.setNamesrvAddr("localhost:9876");
        producer.start();

        for (int i = 0; i < 10; i++) {
            Message msg = new Message("SyncTopic", "TagA",
                    ("同步消息-" + i).getBytes(StandardCharsets.UTF_8));
            // 同步发送：阻塞等待 Broker 返回结果
            SendResult result = producer.send(msg);
            System.out.printf("发送结果: msgId=%s, status=%s, queue=%s%n",
                    result.getMsgId(), result.getSendStatus(), result.getMessageQueue());
        }
        producer.shutdown();
    }
}

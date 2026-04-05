package com.rocketmq.example.idempotent;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import java.nio.charset.StandardCharsets;

/**
 * 幂等消息生产者
 * 通过设置业务唯一键（Keys）实现消息去重的基础
 */
public class IdempotentProducer {
    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("IdempotentProducerGroup");
        producer.setNamesrvAddr("localhost:9876");
        producer.start();

        for (int i = 0; i < 5; i++) {
            String orderId = "ORDER_" + System.currentTimeMillis() + "_" + i;
            Message msg = new Message("IdempotentTopic", "TagA",
                    orderId,  // 设置 Keys 为业务唯一键
                    ("订单消息: " + orderId).getBytes(StandardCharsets.UTF_8));
            SendResult result = producer.send(msg);
            System.out.printf("发送成功: orderId=%s, msgId=%s%n", orderId, result.getMsgId());
        }
        producer.shutdown();
    }
}

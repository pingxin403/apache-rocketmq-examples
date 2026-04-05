package com.rocketmq.example.send;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import java.nio.charset.StandardCharsets;

/**
 * 单向发送示例
 * 特点：不等待 Broker 响应，速度最快，适合日志采集等允许少量丢失的场景
 */
public class OnewayProducer {
    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("OnewayProducerGroup");
        producer.setNamesrvAddr("localhost:9876");
        producer.start();

        for (int i = 0; i < 10; i++) {
            Message msg = new Message("OnewayTopic", "TagA",
                    ("单向消息-" + i).getBytes(StandardCharsets.UTF_8));
            // 单向发送：不等待任何响应
            producer.sendOneway(msg);
            System.out.printf("单向消息 %d 已发出（无返回结果）%n", i);
        }
        Thread.sleep(1000); // 等待发送完成
        producer.shutdown();
    }
}

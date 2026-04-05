package com.example.rocketmq;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

/**
 * RocketMQ 生产者示例
 * 演示如何发送简单消息
 */
public class HelloWorldProducer {
    
    public static void main(String[] args) throws Exception {
        // 1. 创建生产者实例，指定生产者组名
        DefaultMQProducer producer = new DefaultMQProducer("hello_world_producer_group");
        
        // 2. 设置 NameServer 地址
        producer.setNamesrvAddr("localhost:9876");
        
        // 3. 启动生产者
        producer.start();
        System.out.println("生产者启动成功");
        
        try {
            // 4. 创建消息
            // 参数：Topic名称、Tag标签、消息体
            Message message = new Message(
                "HelloWorldTopic",           // Topic
                "TagA",                      // Tag
                "Hello RocketMQ!".getBytes() // 消息体
            );
            
            // 5. 发送消息
            SendResult sendResult = producer.send(message);
            System.out.printf("消息发送成功: %s%n", sendResult);
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 6. 关闭生产者
            producer.shutdown();
            System.out.println("生产者已关闭");
        }
    }
}

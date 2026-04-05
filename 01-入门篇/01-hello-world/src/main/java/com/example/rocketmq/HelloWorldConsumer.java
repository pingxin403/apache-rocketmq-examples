package com.example.rocketmq;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * RocketMQ 消费者示例
 * 演示如何接收和处理消息
 */
public class HelloWorldConsumer {
    
    public static void main(String[] args) throws Exception {
        // 1. 创建消费者实例，指定消费者组名
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("hello_world_consumer_group");
        
        // 2. 设置 NameServer 地址
        consumer.setNamesrvAddr("localhost:9876");
        
        // 3. 订阅 Topic 和 Tag
        // 参数：Topic名称、Tag过滤表达式（* 表示订阅所有Tag）
        consumer.subscribe("HelloWorldTopic", "*");
        
        // 4. 注册消息监听器
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(
                    List<MessageExt> messages,
                    ConsumeConcurrentlyContext context) {
                
                // 处理接收到的消息
                for (MessageExt message : messages) {
                    System.out.printf("收到消息: Topic=%s, Tag=%s, MsgId=%s, Body=%s%n",
                        message.getTopic(),
                        message.getTags(),
                        message.getMsgId(),
                        new String(message.getBody())
                    );
                }
                
                // 返回消费成功状态
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        
        // 5. 启动消费者
        consumer.start();
        System.out.println("消费者启动成功，等待接收消息...");
        
        // 保持程序运行，持续接收消息
        // 实际应用中不需要这行，消费者会一直运行
        Thread.sleep(Long.MAX_VALUE);
    }
}

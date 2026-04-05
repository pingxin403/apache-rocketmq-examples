package com.example.rocketmq;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RocketMQ 集群消费模式示例
 * 演示集群消费模式的特点：负载均衡,一条消息只被消费者组内的一个实例消费
 * 
 * 注意：RocketMQ 5.x 默认使用集群消费模式
 */
public class ClusteringConsumer {
    
    // 消息计数器
    private static final AtomicInteger messageCount = new AtomicInteger(0);
    
    public static void main(String[] args) throws Exception {
        // 1. 创建消费者实例
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("clustering_consumer_group");
        
        // 2. 设置 NameServer 地址
        consumer.setNamesrvAddr("localhost:9876");
        
        // 3. RocketMQ 5.x 默认使用集群消费模式（CLUSTERING）
        // 集群消费特点：
        // - 一条消息只被消费者组内的一个实例消费
        // - 消费进度存储在 Broker 端
        // - 支持消息重试和死信队列
        // - 支持负载均衡
        
        // 4. 设置消费失败重试次数（默认 16 次）
        consumer.setMaxReconsumeTimes(3);
        
        // 5. 订阅 Topic
        consumer.subscribe("ClusteringTopic", "*");
        
        // 6. 注册消息监听器
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(
                    List<MessageExt> msgs, 
                    ConsumeConcurrentlyContext context) {
                
                for (MessageExt msg : msgs) {
                    try {
                        // 获取消费者实例标识
                        String consumerInstance = InetAddress.getLocalHost().getHostName() 
                            + "-" + Thread.currentThread().getName();
                        
                        // 消息计数
                        int count = messageCount.incrementAndGet();
                        
                        // 打印消息信息
                        System.out.printf("[集群消费] 消费者实例: %s%n", consumerInstance);
                        System.out.printf("  消息ID: %s%n", msg.getMsgId());
                        System.out.printf("  消息内容: %s%n", new String(msg.getBody()));
                        System.out.printf("  队列ID: %d%n", msg.getQueueId());
                        System.out.printf("  已消费消息数: %d%n", count);
                        System.out.println("  ---");
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                        // 消费失败，返回 RECONSUME_LATER 会触发重试
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
                
                // 返回消费成功
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        
        // 7. 启动消费者
        consumer.start();
        System.out.println("=== 集群消费者启动成功 ===");
        System.out.println("消费模式: CLUSTERING（集群消费，默认模式）");
        System.out.println("消费者组: clustering_consumer_group");
        System.out.println("订阅 Topic: ClusteringTopic");
        System.out.println("消费进度存储: Broker 端");
        System.out.println("等待接收消息...\n");
        
        // 保持运行
        Thread.sleep(Long.MAX_VALUE);
    }
}

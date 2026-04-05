package com.example.rocketmq;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

/**
 * RocketMQ 消费模式生产者示例
 * 演示如何发送消息到不同的 Topic，用于测试集群消费和广播消费
 */
public class ConsumeModesProducer {
    
    public static void main(String[] args) throws Exception {
        // 检查参数
        if (args.length < 1) {
            System.out.println("用法: java ConsumeModesProducer <clustering|broadcasting>");
            System.exit(1);
        }
        
        String mode = args[0];
        
        // 1. 创建生产者实例
        DefaultMQProducer producer = new DefaultMQProducer("consume_modes_producer_group");
        
        // 2. 设置 NameServer 地址
        producer.setNamesrvAddr("localhost:9876");
        
        // 3. 启动生产者
        producer.start();
        System.out.println("生产者启动成功");
        
        try {
            if ("clustering".equals(mode)) {
                // 集群消费模式：发送 9 条消息
                sendClusteringMessages(producer);
            } else if ("broadcasting".equals(mode)) {
                // 广播消费模式：发送 3 条消息
                sendBroadcastingMessages(producer);
            } else {
                System.out.println("无效的模式: " + mode);
                System.out.println("请使用 clustering 或 broadcasting");
            }
            
        } finally {
            // 4. 关闭生产者
            producer.shutdown();
            System.out.println("生产者已关闭");
        }
    }
    
    /**
     * 发送集群消费消息
     */
    private static void sendClusteringMessages(DefaultMQProducer producer) throws Exception {
        System.out.println("\n=== 发送集群消费消息 ===");
        
        for (int i = 1; i <= 9; i++) {
            Message message = new Message(
                "ClusteringTopic",                          // Topic
                "TagA",                                     // Tag
                ("集群消费消息-" + i).getBytes()             // 消息体
            );
            
            SendResult sendResult = producer.send(message);
            System.out.printf("消息 %d 发送成功: MsgId=%s, QueueId=%d%n",
                i,
                sendResult.getMsgId(),
                sendResult.getMessageQueue().getQueueId());
        }
        
        System.out.println("\n总共发送 9 条消息到 ClusteringTopic");
        System.out.println("预期结果：每个消费者大约消费 3 条消息（负载均衡）");
    }
    
    /**
     * 发送广播消费消息
     */
    private static void sendBroadcastingMessages(DefaultMQProducer producer) throws Exception {
        System.out.println("\n=== 发送广播消费消息 ===");
        
        for (int i = 1; i <= 3; i++) {
            Message message = new Message(
                "BroadcastingTopic",                        // Topic
                "TagA",                                     // Tag
                ("广播消费消息-" + i).getBytes()             // 消息体
            );
            
            SendResult sendResult = producer.send(message);
            System.out.printf("消息 %d 发送成功: MsgId=%s, QueueId=%d%n",
                i,
                sendResult.getMsgId(),
                sendResult.getMessageQueue().getQueueId());
        }
        
        System.out.println("\n总共发送 3 条消息到 BroadcastingTopic");
        System.out.println("预期结果：每个消费者都消费 3 条消息（广播）");
    }
}

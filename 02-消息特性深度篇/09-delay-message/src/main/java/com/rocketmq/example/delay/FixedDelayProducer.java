package com.rocketmq.example.delay;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

/**
 * 固定延迟生产者（RocketMQ 4.x 兼容模式）
 * 演示18级固定延迟的使用
 */
public class FixedDelayProducer {
    
    public static void main(String[] args) throws Exception {
        // 1. 创建生产者
        DefaultMQProducer producer = new DefaultMQProducer("delay_producer_group");
        producer.setNamesrvAddr("localhost:9876");
        producer.start();
        
        System.out.println("=== RocketMQ 固定延迟消息示例 ===\n");
        
        // 2. 演示不同的延迟级别
        sendDelayMessage(producer, 1, "1秒");
        sendDelayMessage(producer, 3, "10秒");
        sendDelayMessage(producer, 5, "1分钟");
        sendDelayMessage(producer, 16, "30分钟");
        
        System.out.println("\n所有延迟消息已发送，请观察消费者输出");
        System.out.println("提示：消息将按照延迟时间依次投递");
        
        // 保持进程运行，等待消息投递
        Thread.sleep(120000);
        producer.shutdown();
    }
    
    /**
     * 发送延迟消息
     * 
     * @param producer 生产者
     * @param delayLevel 延迟级别（1-18）
     * @param delayDesc 延迟时间描述
     */
    private static void sendDelayMessage(DefaultMQProducer producer, 
                                        int delayLevel, 
                                        String delayDesc) throws Exception {
        String orderId = "ORDER_" + System.currentTimeMillis() + "_" + delayLevel;
        
        // 创建延迟消息
        Message msg = new Message(
            "DelayTopic",
            "OrderTag",
            orderId,
            ("订单超时取消: " + orderId).getBytes()
        );
        
        // 设置延迟级别
        msg.setDelayTimeLevel(delayLevel);
        
        // 发送消息
        SendResult result = producer.send(msg);
        
        System.out.printf("发送延迟消息 - OrderId: %s, 延迟级别: %d (%s), MsgId: %s%n",
            orderId, delayLevel, delayDesc, result.getMsgId());
    }
}

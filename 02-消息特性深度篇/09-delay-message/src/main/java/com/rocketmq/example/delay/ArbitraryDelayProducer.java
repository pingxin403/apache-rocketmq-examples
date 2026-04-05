package com.rocketmq.example.delay;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 任意时间延迟生产者（RocketMQ 5.x）
 * 演示任意时间延迟的使用
 */
public class ArbitraryDelayProducer {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public static void main(String[] args) throws Exception {
        // 1. 创建生产者
        DefaultMQProducer producer = new DefaultMQProducer("delay_producer_group");
        producer.setNamesrvAddr("localhost:9876");
        producer.start();
        
        System.out.println("=== RocketMQ 任意时间延迟消息示例 ===\n");
        
        // 2. 演示不同的延迟时间
        sendArbitraryDelayMessage(producer, 37, TimeUnit.MINUTES, "37分钟");
        sendArbitraryDelayMessage(producer, 90, TimeUnit.MINUTES, "1.5小时");
        sendArbitraryDelayMessage(producer, 5, TimeUnit.HOURS, "5小时");
        sendArbitraryDelayMessage(producer, 1, TimeUnit.DAYS, "1天");
        
        System.out.println("\n所有延迟消息已发送，请观察消费者输出");
        System.out.println("提示：消息将按照延迟时间依次投递");
        
        // 保持进程运行
        Thread.sleep(120000);
        producer.shutdown();
    }
    
    /**
     * 发送任意时间延迟消息
     * 
     * @param producer 生产者
     * @param delay 延迟时间
     * @param unit 时间单位
     * @param delayDesc 延迟时间描述
     */
    private static void sendArbitraryDelayMessage(DefaultMQProducer producer,
                                                  long delay,
                                                  TimeUnit unit,
                                                  String delayDesc) throws Exception {
        String orderId = "ORDER_" + System.currentTimeMillis() + "_" + delay + unit.name();
        
        // 创建延迟消息
        Message msg = new Message(
            "DelayTopic",
            "OrderTag",
            orderId,
            ("订单超时取消: " + orderId).getBytes()
        );
        
        // 设置任意延迟时间（RocketMQ 5.x）
        long deliverTimeMs = System.currentTimeMillis() + unit.toMillis(delay);
        msg.setDeliverTimeMs(deliverTimeMs);
        
        // 发送消息
        SendResult result = producer.send(msg);
        
        System.out.printf("发送延迟消息 - OrderId: %s%n", orderId);
        System.out.printf("  延迟时间: %s%n", delayDesc);
        System.out.printf("  当前时间: %s%n", DATE_FORMAT.format(new Date()));
        System.out.printf("  投递时间: %s%n", DATE_FORMAT.format(new Date(deliverTimeMs)));
        System.out.printf("  MsgId: %s%n", result.getMsgId());
        System.out.println();
    }
}

package com.rocketmq.example.delay;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 订单超时场景生产者
 * 演示订单超时自动取消的完整流程
 */
public class OrderTimeoutProducer {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static DefaultMQProducer producer;
    
    public static void main(String[] args) throws Exception {
        // 初始化生产者
        producer = new DefaultMQProducer("order_producer_group");
        producer.setNamesrvAddr("localhost:9876");
        producer.start();
        
        System.out.println("=== 订单超时自动取消场景 ===\n");
        
        // 模拟用户下单流程
        for (int i = 0; i < 3; i++) {
            // 1. 创建订单
            String orderId = createOrder();
            
            // 2. 发送延迟消息：30分钟后检查订单状态
            sendOrderTimeoutMessage(orderId, 30, TimeUnit.MINUTES);
            
            System.out.println("订单创建成功: " + orderId);
            System.out.println("30分钟后将自动检查订单状态\n");
            
            // 间隔5秒
            Thread.sleep(5000);
        }
        
        System.out.println("所有订单已创建，延迟消息已发送");
        System.out.println("请观察 OrderTimeoutConsumer 的输出\n");
        
        // 保持进程运行
        Thread.sleep(60000);
        producer.shutdown();
    }
    
    /**
     * 创建订单
     */
    private static String createOrder() {
        String orderId = "ORDER_" + System.currentTimeMillis();
        
        // 实际场景中，这里应该是数据库操作
        // INSERT INTO orders (order_id, status, create_time) VALUES (?, 'UNPAID', NOW())
        System.out.println("----------------------------------------");
        System.out.println("创建订单: " + orderId);
        System.out.println("订单状态: UNPAID");
        System.out.println("创建时间: " + DATE_FORMAT.format(new Date()));
        
        return orderId;
    }
    
    /**
     * 发送订单超时检查消息
     */
    private static void sendOrderTimeoutMessage(String orderId, long delay, TimeUnit unit) 
            throws Exception {
        Message msg = new Message(
            "OrderTimeoutTopic",
            "TimeoutCheck",
            orderId,
            ("检查订单: " + orderId).getBytes()
        );
        
        // RocketMQ 5.x：设置任意延迟时间
        long deliverTimeMs = System.currentTimeMillis() + unit.toMillis(delay);
        msg.setDeliverTimeMs(deliverTimeMs);
        
        // RocketMQ 4.x：使用固定延迟级别
        // msg.setDelayTimeLevel(16);  // Level 16 = 30分钟
        
        SendResult result = producer.send(msg);
        
        System.out.println("延迟消息已发送");
        System.out.println("  投递时间: " + DATE_FORMAT.format(new Date(deliverTimeMs)));
        System.out.println("  MsgId: " + result.getMsgId());
        System.out.println("----------------------------------------");
    }
}

package com.rocketmq.example.filter;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

import java.util.Random;

/**
 * SQL92 过滤 - 生产者
 * 
 * 功能：发送带自定义属性的订单消息
 * 场景：电商订单系统，需要根据金额、VIP 等级、地区等多维度过滤
 */
public class SqlFilterProducer {
    
    public static void main(String[] args) throws Exception {
        // 1. 创建生产者
        DefaultMQProducer producer = new DefaultMQProducer("sql_filter_producer_group");
        producer.setNamesrvAddr("localhost:9876");
        producer.start();
        System.out.println("SQL92 过滤生产者已启动...");
        
        Random random = new Random();
        
        // 2. 发送不同属性的订单消息
        for (int i = 0; i < 20; i++) {
            int amount = (i + 1) * 50;  // 50, 100, 150, 200, ...
            boolean isVip = random.nextBoolean();
            String region = random.nextBoolean() ? "NORTH" : "SOUTH";
            
            Message msg = new Message(
                "OrderTopic",
                "ORDER",
                ("订单-" + i).getBytes()
            );
            
            // 3. 设置消息的自定义属性（User Properties）
            // 这些属性可以在 SQL 表达式中使用
            msg.putUserProperty("amount", String.valueOf(amount));
            msg.putUserProperty("vip", String.valueOf(isVip));
            msg.putUserProperty("region", region);
            msg.putUserProperty("orderId", "ORDER_" + i);
            
            SendResult result = producer.send(msg);
            System.out.printf("发送消息 - 金额: %d, VIP: %s, 地区: %s, MsgId: %s%n",
                amount, isVip, region, result.getMsgId());
            
            Thread.sleep(100);
        }
        
        System.out.println("消息发送完成，共发送 20 条消息");
        
        // 4. 关闭生产者
        Thread.sleep(1000);
        producer.shutdown();
    }
}

package com.rocketmq.example.filter;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

/**
 * Tag 过滤 - 生产者
 * 
 * 功能：发送不同 Tag 的订单消息
 * 场景：电商订单系统，不同类型的订单消息
 */
public class TagFilterProducer {
    
    public static void main(String[] args) throws Exception {
        // 1. 创建生产者
        DefaultMQProducer producer = new DefaultMQProducer("tag_filter_producer_group");
        producer.setNamesrvAddr("localhost:9876");
        producer.start();
        System.out.println("Tag 过滤生产者已启动...");
        
        // 2. 定义不同类型的订单 Tag
        String[] tags = {
            "ORDER_CREATED",   // 订单创建
            "ORDER_PAID",      // 订单支付
            "ORDER_SHIPPED",   // 订单发货
            "ORDER_REFUND"     // 订单退款
        };
        
        // 3. 发送不同类型的订单消息
        for (int i = 0; i < 20; i++) {
            String tag = tags[i % tags.length];
            
            Message msg = new Message(
                "OrderTopic",                              // Topic
                tag,                                       // Tag
                ("订单消息 - " + tag + " - " + i).getBytes()  // Body
            );
            
            // 设置消息 Key（用于消息追踪）
            msg.setKeys("ORDER_" + i);
            
            SendResult result = producer.send(msg);
            System.out.printf("发送消息 - Tag: %s, MsgId: %s, Status: %s%n", 
                tag, result.getMsgId(), result.getSendStatus());
            
            // 模拟发送间隔
            Thread.sleep(100);
        }
        
        System.out.println("消息发送完成，共发送 20 条消息");
        
        // 4. 关闭生产者
        Thread.sleep(1000);
        producer.shutdown();
    }
}

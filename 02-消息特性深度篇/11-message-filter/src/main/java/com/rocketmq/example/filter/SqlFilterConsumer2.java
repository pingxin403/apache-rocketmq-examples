package com.rocketmq.example.filter;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.MessageSelector;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * SQL92 过滤 - 消费者2（VIP 服务）
 * 
 * 功能：只消费 VIP 用户的订单
 * 场景：VIP 服务需要为 VIP 用户提供专属服务
 */
public class SqlFilterConsumer2 {
    
    public static void main(String[] args) throws Exception {
        // 1. 创建消费者
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("vip_service_consumer_group");
        consumer.setNamesrvAddr("localhost:9876");
        
        // 2. 使用 SQL 表达式订阅：VIP 用户的订单
        consumer.subscribe("OrderTopic", 
            MessageSelector.bySql("vip = 'true'"));
        
        // 3. 注册消息监听器
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                           ConsumeConcurrentlyContext context) {
                for (MessageExt msg : msgs) {
                    String amount = msg.getUserProperty("amount");
                    String region = msg.getUserProperty("region");
                    String content = new String(msg.getBody());
                    
                    System.out.printf("[VIP服务] 收到消息 - 内容: %s, 金额: %s, 地区: %s%n",
                        content, amount, region);
                    System.out.println("  -> 提供 VIP 专属服务");
                    
                    // 实际业务：为 VIP 用户提供专属服务
                    // vipService.provideExclusiveService(orderId);
                }
                
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        
        // 4. 启动消费者
        consumer.start();
        System.out.println("VIP 服务消费者已启动（SQL: vip = 'true'）...");
        System.out.println("按 Enter 键退出...");
        System.in.read();
    }
}

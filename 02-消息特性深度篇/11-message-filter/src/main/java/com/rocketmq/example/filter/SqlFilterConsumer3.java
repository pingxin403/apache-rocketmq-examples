package com.rocketmq.example.filter;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.MessageSelector;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * SQL92 过滤 - 消费者3（复杂条件过滤）
 * 
 * 功能：消费北方地区且金额在 100-500 之间的订单
 * 场景：区域营销活动，针对特定地区和金额范围的订单
 */
public class SqlFilterConsumer3 {
    
    public static void main(String[] args) throws Exception {
        // 1. 创建消费者
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("complex_filter_consumer_group");
        consumer.setNamesrvAddr("localhost:9876");
        
        // 2. 使用 SQL 表达式订阅：北方地区且金额在 100-500 之间的订单
        // 支持 AND、OR、BETWEEN、IN 等运算符
        consumer.subscribe("OrderTopic", 
            MessageSelector.bySql("region = 'NORTH' AND amount BETWEEN 100 AND 500"));
        
        // 3. 注册消息监听器
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                           ConsumeConcurrentlyContext context) {
                for (MessageExt msg : msgs) {
                    String amount = msg.getUserProperty("amount");
                    String vip = msg.getUserProperty("vip");
                    String region = msg.getUserProperty("region");
                    String content = new String(msg.getBody());
                    
                    System.out.printf("[复杂过滤] 收到消息 - 内容: %s, 金额: %s, VIP: %s, 地区: %s%n",
                        content, amount, vip, region);
                    System.out.println("  -> 北方地区中等金额订单处理");
                    
                    // 实际业务：针对特定地区和金额范围的营销活动
                    // marketingService.processRegionalOrder(orderId, region, amount);
                }
                
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        
        // 4. 启动消费者
        consumer.start();
        System.out.println("复杂过滤消费者已启动（SQL: region = 'NORTH' AND amount BETWEEN 100 AND 500）...");
        System.out.println("按 Enter 键退出...");
        System.in.read();
    }
}

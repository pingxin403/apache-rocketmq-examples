package com.rocketmq.example.filter;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.MessageSelector;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * SQL92 过滤 - 消费者1（大额订单处理）
 * 
 * 功能：只消费金额大于 200 的订单
 * 场景：风控服务需要对大额订单进行审核
 */
public class SqlFilterConsumer1 {
    
    public static void main(String[] args) throws Exception {
        // 1. 创建消费者
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("large_order_consumer_group");
        consumer.setNamesrvAddr("localhost:9876");
        
        // 2. 使用 SQL 表达式订阅：金额大于 200 的订单
        // 注意：Broker 需要开启 SQL 过滤支持（enablePropertyFilter = true）
        consumer.subscribe("OrderTopic", 
            MessageSelector.bySql("amount > 200"));
        
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
                    
                    System.out.printf("[大额订单] 收到消息 - 内容: %s, 金额: %s, VIP: %s, 地区: %s%n",
                        content, amount, vip, region);
                    System.out.println("  -> 触发风控审核");
                    
                    // 实际业务：调用风控系统进行审核
                    // riskControlService.audit(orderId, amount);
                }
                
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        
        // 4. 启动消费者
        consumer.start();
        System.out.println("大额订单消费者已启动（SQL: amount > 200）...");
        System.out.println("按 Enter 键退出...");
        System.in.read();
    }
}

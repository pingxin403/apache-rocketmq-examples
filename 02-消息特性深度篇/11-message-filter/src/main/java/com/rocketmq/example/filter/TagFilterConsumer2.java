package com.rocketmq.example.filter;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * Tag 过滤 - 消费者2（物流服务）
 * 
 * 功能：只消费订单发货消息
 * 场景：物流服务只需要在订单发货时安排配送
 */
public class TagFilterConsumer2 {
    
    public static void main(String[] args) throws Exception {
        // 1. 创建消费者
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("logistics_consumer_group");
        consumer.setNamesrvAddr("localhost:9876");
        
        // 2. 订阅 Topic，只订阅发货消息
        consumer.subscribe("OrderTopic", "ORDER_SHIPPED");
        
        // 3. 注册消息监听器
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                           ConsumeConcurrentlyContext context) {
                for (MessageExt msg : msgs) {
                    String tag = msg.getTags();
                    String content = new String(msg.getBody());
                    
                    System.out.printf("[物流服务] 收到消息 - Tag: %s, 内容: %s%n", tag, content);
                    System.out.println("  -> 安排物流配送");
                    
                    // 实际业务：调用物流系统 API，创建配送单
                    // logisticsService.createDeliveryOrder(orderId);
                }
                
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        
        // 4. 启动消费者
        consumer.start();
        System.out.println("物流服务消费者已启动（订阅 ORDER_SHIPPED）...");
        System.out.println("按 Enter 键退出...");
        System.in.read();
    }
}

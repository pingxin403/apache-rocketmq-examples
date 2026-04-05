package com.rocketmq.example.filter;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * Tag 过滤 - 消费者1（库存服务）
 * 
 * 功能：只消费订单创建和退款消息
 * 场景：库存服务需要在订单创建时扣减库存，在订单退款时恢复库存
 */
public class TagFilterConsumer1 {
    
    public static void main(String[] args) throws Exception {
        // 1. 创建消费者
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("inventory_consumer_group");
        consumer.setNamesrvAddr("localhost:9876");
        
        // 2. 订阅 Topic，并指定 Tag 过滤条件
        // 使用 || 分隔多个 Tag，表示订阅这些 Tag 中的任意一个
        consumer.subscribe("OrderTopic", "ORDER_CREATED || ORDER_REFUND");
        
        // 3. 注册消息监听器
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                           ConsumeConcurrentlyContext context) {
                for (MessageExt msg : msgs) {
                    String tag = msg.getTags();
                    String content = new String(msg.getBody());
                    
                    System.out.printf("[库存服务] 收到消息 - Tag: %s, 内容: %s%n", tag, content);
                    
                    // 根据 Tag 处理不同的业务逻辑
                    if ("ORDER_CREATED".equals(tag)) {
                        System.out.println("  -> 扣减库存");
                        // 实际业务：UPDATE inventory SET stock = stock - 1 WHERE product_id = ?
                    } else if ("ORDER_REFUND".equals(tag)) {
                        System.out.println("  -> 恢复库存");
                        // 实际业务：UPDATE inventory SET stock = stock + 1 WHERE product_id = ?
                    }
                }
                
                // 返回消费成功
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        
        // 4. 启动消费者
        consumer.start();
        System.out.println("库存服务消费者已启动（订阅 ORDER_CREATED || ORDER_REFUND）...");
        System.out.println("按 Enter 键退出...");
        System.in.read();
    }
}

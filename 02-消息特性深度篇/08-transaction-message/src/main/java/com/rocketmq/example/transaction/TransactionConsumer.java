package com.rocketmq.example.transaction;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RocketMQ 事务消息消费者示例
 * 
 * 功能说明：
 * 1. 订阅事务消息 Topic
 * 2. 消费已提交的事务消息
 * 3. 执行下游业务逻辑（扣减库存）
 * 4. 保证消费幂等性
 * 
 * @author RocketMQ Example
 */
public class TransactionConsumer {
    
    // 用于记录已处理的订单ID（实际场景中应该使用数据库或Redis）
    private static final Set<String> processedOrders = new HashSet<>();
    
    public static void main(String[] args) throws Exception {
        // 1. 创建消费者
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("transaction_consumer_group");
        consumer.setNamesrvAddr("localhost:9876");
        
        // 2. 订阅事务消息 Topic
        // * 表示订阅所有 Tag 的消息
        consumer.subscribe("TransactionTopic", "*");
        
        // 3. 注册消息监听器
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                            ConsumeConcurrentlyContext context) {
                for (MessageExt msg : msgs) {
                    try {
                        // 获取订单ID
                        String orderId = msg.getKeys();
                        String body = new String(msg.getBody());
                        
                        System.out.println("\n========================================");
                        System.out.println("收到事务消息");
                        System.out.println("订单ID: " + orderId);
                        System.out.println("消息内容: " + body);
                        System.out.println("消息ID: " + msg.getMsgId());
                        System.out.println("重试次数: " + msg.getReconsumeTimes());
                        
                        // 幂等性判断：检查是否已处理
                        if (isProcessed(orderId)) {
                            System.out.println("消息已处理，跳过（幂等性保证）");
                            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                        }
                        
                        // 执行业务逻辑：扣减库存
                        boolean success = deductStock(orderId);
                        
                        if (success) {
                            // 记录处理状态
                            markAsProcessed(orderId);
                            
                            System.out.println("库存扣减成功");
                            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                        } else {
                            System.err.println("库存扣减失败，消息将重试");
                            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                        }
                        
                    } catch (Exception e) {
                        System.err.println("消息处理异常: " + e.getMessage());
                        e.printStackTrace();
                        
                        // 返回失败，消息会重试
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        
        // 4. 启动消费者
        consumer.start();
        System.out.println("事务消息消费者已启动，等待接收消息...\n");
        
        // 保持进程运行
        Thread.sleep(Long.MAX_VALUE);
    }
    
    /**
     * 检查订单是否已处理（幂等性判断）
     * 
     * 实际场景中应该查询数据库或Redis
     * 
     * @param orderId 订单ID
     * @return true-已处理，false-未处理
     */
    private static boolean isProcessed(String orderId) {
        synchronized (processedOrders) {
            return processedOrders.contains(orderId);
        }
    }
    
    /**
     * 标记订单为已处理
     * 
     * 实际场景中应该写入数据库或Redis
     * 
     * @param orderId 订单ID
     */
    private static void markAsProcessed(String orderId) {
        synchronized (processedOrders) {
            processedOrders.add(orderId);
        }
    }
    
    /**
     * 扣减库存
     * 
     * 实际场景中应该是数据库操作：
     * UPDATE stock SET quantity = quantity - 1 WHERE product_id = ? AND quantity > 0
     * 
     * @param orderId 订单ID
     * @return true-成功，false-失败
     */
    private static boolean deductStock(String orderId) {
        System.out.println("执行库存扣减: " + orderId);
        
        try {
            // 模拟数据库操作
            Thread.sleep(100);
            
            // 模拟：90% 成功，10% 失败
            boolean success = Math.random() > 0.1;
            
            if (success) {
                System.out.println("库存扣减操作成功");
            } else {
                System.err.println("库存扣减操作失败（库存不足或数据库异常）");
            }
            
            return success;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}

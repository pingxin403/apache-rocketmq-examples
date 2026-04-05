package com.rocketmq.example.ordered;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;

import java.util.List;

/**
 * 顺序消息生产者
 * 
 * 功能说明：
 * 1. 使用 MessageQueueSelector 将同一订单的消息路由到同一队列
 * 2. 保证同一订单的消息按照发送顺序存储
 * 3. 演示分区顺序消息的发送方式
 * 
 * @author RocketMQ Example
 */
public class OrderedProducer {
    
    public static void main(String[] args) throws Exception {
        // 1. 创建生产者实例
        DefaultMQProducer producer = new DefaultMQProducer("ordered_producer_group");
        
        // 2. 设置 NameServer 地址
        producer.setNamesrvAddr("localhost:9876");
        
        // 3. 启动生产者
        producer.start();
        System.out.println("顺序消息生产者已启动...");
        
        // 4. 模拟订单消息（同一订单的多个状态变更）
        String[] orders = {"ORDER_001", "ORDER_002", "ORDER_001", "ORDER_002", "ORDER_001"};
        String[] statuses = {"创建", "创建", "支付", "支付", "发货"};
        
        // 5. 发送顺序消息
        for (int i = 0; i < orders.length; i++) {
            String orderId = orders[i];
            String status = statuses[i];
            
            // 构造消息
            Message msg = new Message(
                "OrderedTopic",           // Topic
                "OrderTag",               // Tag
                orderId,                  // Key（用于消息追踪）
                (orderId + " - " + status).getBytes()  // Body
            );
            
            // 使用 MessageQueueSelector 选择队列
            // 同一 orderId 的消息会路由到同一队列
            SendResult result = producer.send(msg, new MessageQueueSelector() {
                @Override
                public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                    // 根据 orderId 的 hashCode 选择队列
                    String orderId = (String) arg;
                    int index = Math.abs(orderId.hashCode()) % mqs.size();
                    return mqs.get(index);
                }
            }, orderId);  // 传入 orderId 作为选择队列的参数
            
            System.out.printf("发送顺序消息 - OrderId: %s, Status: %s, Queue: %d, MsgId: %s%n",
                orderId, status, result.getMessageQueue().getQueueId(), result.getMsgId());
            
            // 模拟发送间隔
            Thread.sleep(100);
        }
        
        System.out.println("所有顺序消息发送完成");
        
        // 6. 关闭生产者
        producer.shutdown();
    }
}

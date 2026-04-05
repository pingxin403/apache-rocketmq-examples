package com.rocketmq.example.ordered;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * IM 聊天消息消费者
 * 
 * 功能说明：
 * 1. 接收 IM 系统的聊天消息
 * 2. 保证同一会话的消息按顺序消费
 * 3. 模拟消息存储和推送
 * 
 * @author RocketMQ Example
 */
public class IMMessageConsumer {
    
    public static void main(String[] args) throws Exception {
        // 1. 创建消费者实例
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("im_consumer_group");
        
        // 2. 设置 NameServer 地址
        consumer.setNamesrvAddr("localhost:9876");
        
        // 3. 订阅 Topic
        consumer.subscribe("IMTopic", "*");
        
        // 4. 注册顺序消息监听器
        consumer.registerMessageListener(new MessageListenerOrderly() {
            @Override
            public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs,
                                                       ConsumeOrderlyContext context) {
                for (MessageExt msg : msgs) {
                    String sessionId = msg.getKeys();
                    String content = new String(msg.getBody());
                    
                    System.out.printf("[会话:%s] 收到消息: %s (Queue: %d)%n", 
                        sessionId, content, msg.getQueueId());
                    
                    try {
                        // 存储到数据库
                        saveMessageToDB(sessionId, content);
                        
                        // 推送给客户端
                        pushToClient(sessionId, content);
                        
                        return ConsumeOrderlyStatus.SUCCESS;
                        
                    } catch (Exception e) {
                        System.err.println("消息处理失败: " + e.getMessage());
                        return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
                    }
                }
                
                return ConsumeOrderlyStatus.SUCCESS;
            }
        });
        
        // 5. 启动消费者
        consumer.start();
        System.out.println("IM 消息消费者已启动...");
        
        // 保持进程运行
        Thread.sleep(Long.MAX_VALUE);
    }
    
    /**
     * 保存消息到数据库
     */
    private static void saveMessageToDB(String sessionId, String content) {
        // 实际场景：INSERT INTO messages (session_id, content, create_time) VALUES (?, ?, NOW())
        System.out.println("  -> 保存消息到数据库: " + sessionId);
    }
    
    /**
     * 推送消息给客户端
     */
    private static void pushToClient(String sessionId, String content) {
        // 实际场景：通过 WebSocket 推送给客户端
        System.out.println("  -> 推送消息给客户端: " + sessionId);
    }
}

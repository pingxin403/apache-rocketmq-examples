package com.rocketmq.example.ordered;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;

import java.util.List;

/**
 * IM 聊天消息生产者
 * 
 * 功能说明：
 * 1. 模拟 IM 系统的聊天消息发送
 * 2. 保证同一会话的消息有序
 * 3. 使用 sessionId 作为路由键
 * 
 * 场景说明：
 * - 用户 A 和用户 B 的聊天消息需要保持顺序
 * - 不同会话的消息可以并发处理
 * 
 * @author RocketMQ Example
 */
public class IMMessageProducer {
    
    private static DefaultMQProducer producer;
    
    public static void main(String[] args) throws Exception {
        // 1. 初始化生产者
        producer = new DefaultMQProducer("im_producer_group");
        producer.setNamesrvAddr("localhost:9876");
        producer.start();
        System.out.println("IM 消息生产者已启动...");
        
        // 2. 模拟用户 A 和用户 B 的聊天
        System.out.println("\n=== 用户 A 发送消息 ===");
        sendChatMessage("USER_A", "USER_B", "你好");
        Thread.sleep(100);
        sendChatMessage("USER_A", "USER_B", "在吗？");
        Thread.sleep(100);
        sendChatMessage("USER_A", "USER_B", "有个问题想请教");
        Thread.sleep(200);
        
        System.out.println("\n=== 用户 B 回复消息 ===");
        sendChatMessage("USER_B", "USER_A", "在的");
        Thread.sleep(100);
        sendChatMessage("USER_B", "USER_A", "什么问题？");
        Thread.sleep(100);
        
        System.out.println("\n=== 用户 A 继续发送 ===");
        sendChatMessage("USER_A", "USER_B", "关于 RocketMQ 顺序消息的问题");
        
        System.out.println("\n聊天消息发送完成");
        
        // 3. 关闭生产者
        Thread.sleep(1000);
        producer.shutdown();
    }
    
    /**
     * 发送聊天消息
     * 
     * @param fromUser 发送者
     * @param toUser 接收者
     * @param content 消息内容
     */
    private static void sendChatMessage(String fromUser, String toUser, String content) 
            throws Exception {
        // 生成会话 ID（保证同一会话的消息有序）
        String sessionId = generateSessionId(fromUser, toUser);
        
        // 构造消息
        Message msg = new Message(
            "IMTopic",
            "ChatTag",
            sessionId,  // 使用 sessionId 作为 Key
            (fromUser + " -> " + toUser + ": " + content).getBytes()
        );
        
        // 使用 sessionId 选择队列
        SendResult result = producer.send(msg, new MessageQueueSelector() {
            @Override
            public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                String sessionId = (String) arg;
                int index = Math.abs(sessionId.hashCode()) % mqs.size();
                return mqs.get(index);
            }
        }, sessionId);
        
        System.out.printf("发送聊天消息 - Session: %s, 内容: %s, Queue: %d%n",
            sessionId, content, result.getMessageQueue().getQueueId());
    }
    
    /**
     * 生成会话 ID
     * 
     * 说明：保证双向会话使用同一个 ID
     * 例如：USER_A 和 USER_B 的会话 ID 为 USER_A_USER_B
     *      USER_B 和 USER_A 的会话 ID 也为 USER_A_USER_B
     * 
     * @param user1 用户1
     * @param user2 用户2
     * @return 会话 ID
     */
    private static String generateSessionId(String user1, String user2) {
        // 按字典序排序，保证 A->B 和 B->A 使用同一个 sessionId
        if (user1.compareTo(user2) < 0) {
            return user1 + "_" + user2;
        } else {
            return user2 + "_" + user1;
        }
    }
}

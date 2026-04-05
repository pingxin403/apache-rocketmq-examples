package com.example.rocketmq;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RocketMQ 广播消费模式示例
 * 演示广播消费模式的特点：一条消息被消费者组内的所有实例消费
 * 
 * 注意：RocketMQ 5.x 实现广播消费的方式：
 * 每个消费者实例使用不同的消费者组名，这样每个实例都会消费所有消息
 */
public class BroadcastingConsumer {
    
    // 消息计数器
    private static final AtomicInteger messageCount = new AtomicInteger(0);
    
    public static void main(String[] args) throws Exception {
        // 1. 创建消费者实例
        // 广播消费：每个实例使用不同的消费者组名（添加唯一标识）
        String uniqueGroupName = "broadcasting_consumer_group_" + UUID.randomUUID().toString().substring(0, 8);
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(uniqueGroupName);
        
        // 2. 设置 NameServer 地址
        consumer.setNamesrvAddr("localhost:9876");
        
        // 3. 广播消费模式说明：
        // RocketMQ 5.x 中，通过让每个消费者实例使用不同的消费者组名来实现广播消费
        // 这样每个消费者组都会独立消费所有消息
        // 
        // 广播消费特点：
        // - 一条消息被所有消费者实例消费
        // - 消费进度存储在消费者本地
        // - 不支持消息重试和死信队列
        // - 不支持负载均衡
        
        // 4. 订阅 Topic
        consumer.subscribe("BroadcastingTopic", "*");
        
        // 5. 注册消息监听器
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(
                    List<MessageExt> msgs, 
                    ConsumeConcurrentlyContext context) {
                
                for (MessageExt msg : msgs) {
                    try {
                        // 获取消费者实例标识
                        String consumerInstance = InetAddress.getLocalHost().getHostName() 
                            + "-" + Thread.currentThread().getName();
                        
                        // 消息计数
                        int count = messageCount.incrementAndGet();
                        
                        // 打印消息信息
                        System.out.printf("[广播消费] 消费者实例: %s%n", consumerInstance);
                        System.out.printf("  消费者组: %s%n", uniqueGroupName);
                        System.out.printf("  消息ID: %s%n", msg.getMsgId());
                        System.out.printf("  消息内容: %s%n", new String(msg.getBody()));
                        System.out.printf("  队列ID: %d%n", msg.getQueueId());
                        System.out.printf("  已消费消息数: %d%n", count);
                        System.out.println("  ---");
                        
                        // 模拟配置更新场景
                        updateLocalConfig(new String(msg.getBody()));
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                        // 注意：广播消费不支持消息重试
                        // 即使返回 RECONSUME_LATER，消息也不会重试
                        System.err.println("警告：广播消费不支持消息重试，消费失败的消息会丢失");
                    }
                }
                
                // 返回消费成功
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        
        // 6. 启动消费者
        consumer.start();
        System.out.println("=== 广播消费者启动成功 ===");
        System.out.println("消费模式: BROADCASTING（广播消费）");
        System.out.println("消费者组: " + uniqueGroupName);
        System.out.println("订阅 Topic: BroadcastingTopic");
        System.out.println("消费进度存储: 消费者本地");
        System.out.println("说明: 每个消费者实例使用不同的消费者组名，实现广播消费效果");
        System.out.println("等待接收消息...\n");
        
        // 保持运行
        Thread.sleep(Long.MAX_VALUE);
    }
    
    /**
     * 模拟更新本地配置
     */
    private static void updateLocalConfig(String configData) {
        System.out.printf("  [配置更新] 更新本地配置: %s%n", configData);
    }
}

package com.rocketmq.example.ordered;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

/**
 * 顺序消息消费者
 * 
 * 功能说明：
 * 1. 使用 MessageListenerOrderly 保证消息顺序消费
 * 2. 同一队列的消息串行消费
 * 3. 消费失败时会阻塞当前队列，直到消费成功
 * 
 * @author RocketMQ Example
 */
public class OrderedConsumer {
    
    public static void main(String[] args) throws Exception {
        // 1. 创建消费者实例
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("ordered_consumer_group");
        
        // 2. 设置 NameServer 地址
        consumer.setNamesrvAddr("localhost:9876");
        
        // 3. 订阅 Topic 和 Tag
        consumer.subscribe("OrderedTopic", "*");
        
        // 4. 注册顺序消息监听器
        consumer.registerMessageListener(new MessageListenerOrderly() {
            @Override
            public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs,
                                                       ConsumeOrderlyContext context) {
                // 设置自动提交（默认为 true）
                context.setAutoCommit(true);
                
                for (MessageExt msg : msgs) {
                    String orderId = msg.getKeys();
                    String content = new String(msg.getBody());
                    
                    System.out.printf("[线程:%s] 消费顺序消息 - OrderId: %s, 内容: %s, Queue: %d, MsgId: %s%n",
                        Thread.currentThread().getName(),
                        orderId,
                        content,
                        msg.getQueueId(),
                        msg.getMsgId());
                    
                    try {
                        // 模拟业务处理
                        processOrder(orderId, content);
                        
                        // 返回消费成功
                        return ConsumeOrderlyStatus.SUCCESS;
                        
                    } catch (Exception e) {
                        System.err.println("订单处理失败: " + e.getMessage());
                        
                        // 返回稍后重试（会阻塞当前队列）
                        // 可以设置重试间隔时间
                        context.setSuspendCurrentQueueTimeMillis(1000);  // 1秒后重试
                        return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
                    }
                }
                
                return ConsumeOrderlyStatus.SUCCESS;
            }
        });
        
        // 5. 启动消费者
        consumer.start();
        System.out.println("顺序消息消费者已启动...");
        
        // 保持进程运行
        Thread.sleep(Long.MAX_VALUE);
    }
    
    /**
     * 处理订单
     */
    private static void processOrder(String orderId, String content) throws InterruptedException {
        // 模拟业务处理耗时
        Thread.sleep(100);
        System.out.println("订单处理完成: " + orderId + " - " + content);
    }
}

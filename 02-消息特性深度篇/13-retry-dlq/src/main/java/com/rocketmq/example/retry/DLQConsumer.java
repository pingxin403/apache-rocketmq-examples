package com.rocketmq.example.retry;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

/**
 * 死信队列消费者示例
 * 订阅 %DLQ%RetryConsumerGroup 主题，处理重试耗尽的消息
 * 死信消息通常需要人工介入或特殊补偿逻辑
 */
public class DLQConsumer {
    public static void main(String[] args) throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("DLQConsumerGroup");
        consumer.setNamesrvAddr("localhost:9876");
        // 订阅死信队列（格式：%DLQ% + 原消费组名）
        consumer.subscribe("%DLQ%RetryConsumerGroup", "*");

        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            for (MessageExt msg : msgs) {
                System.out.printf("[死信] msgId=%s, body=%s%n",
                        msg.getMsgId(), new String(msg.getBody()));
                // 记录到数据库或告警通知，人工处理
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        consumer.start();
        System.out.println("死信队列消费者已启动...");
    }
}

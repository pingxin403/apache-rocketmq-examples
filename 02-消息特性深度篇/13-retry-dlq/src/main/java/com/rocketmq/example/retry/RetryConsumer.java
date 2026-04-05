package com.rocketmq.example.retry;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

/**
 * 消息重试消费者示例
 * 消费失败返回 RECONSUME_LATER，RocketMQ 会按延迟等级自动重试
 * 重试 16 次后进入死信队列 %DLQ%RetryConsumerGroup
 */
public class RetryConsumer {
    public static void main(String[] args) throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("RetryConsumerGroup");
        consumer.setNamesrvAddr("localhost:9876");
        consumer.subscribe("RetryTopic", "*");
        // 最大重试次数（默认 16 次）
        consumer.setMaxReconsumeTimes(3);

        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            for (MessageExt msg : msgs) {
                int reconsumeTimes = msg.getReconsumeTimes();
                System.out.printf("收到消息: msgId=%s, 重试次数=%d%n",
                        msg.getMsgId(), reconsumeTimes);
                // 模拟消费失败
                if (reconsumeTimes < 3) {
                    System.out.println("消费失败，触发重试...");
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
                System.out.println("第3次重试成功消费！");
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        consumer.start();
        System.out.println("重试消费者已启动...");
    }
}

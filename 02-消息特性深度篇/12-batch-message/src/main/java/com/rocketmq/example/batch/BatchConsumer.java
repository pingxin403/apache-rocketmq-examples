package com.rocketmq.example.batch;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

/**
 * 批量消息消费示例
 * 通过 setConsumeMessageBatchMaxSize 控制每次消费的消息数量
 */
public class BatchConsumer {
    public static void main(String[] args) throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("BatchConsumerGroup");
        consumer.setNamesrvAddr("localhost:9876");
        consumer.subscribe("BatchTopic", "*");
        // 每次最多消费 32 条消息
        consumer.setConsumeMessageBatchMaxSize(32);

        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            System.out.printf("本次收到 %d 条消息%n", msgs.size());
            for (MessageExt msg : msgs) {
                System.out.printf("  消息: %s%n", new String(msg.getBody()));
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        consumer.start();
        System.out.println("批量消费者已启动...");
    }
}

package com.rocketmq.example.trace;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

/**
 * 消息轨迹消费者示例
 * 开启轨迹后，消费记录也会被追踪
 * 可通过 RocketMQ Dashboard 查看完整消息生命周期
 */
public class TraceConsumer {
    public static void main(String[] args) throws Exception {
        // 第三个参数 true 表示开启消息轨迹
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(
                "TraceConsumerGroup", true);
        consumer.setNamesrvAddr("localhost:9876");
        consumer.subscribe("TraceTopic", "*");

        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            for (MessageExt msg : msgs) {
                System.out.printf("消费消息: msgId=%s, key=%s, body=%s（轨迹已记录）%n",
                        msg.getMsgId(), msg.getKeys(), new String(msg.getBody()));
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        consumer.start();
        System.out.println("轨迹消费者已启动...");
    }
}

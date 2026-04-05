package com.rocketmq.example.idempotent;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 幂等消费者示例
 * 使用内存 Set 去重（生产环境应使用 Redis 或数据库唯一键）
 */
public class IdempotentConsumer {
    // 模拟去重表（生产环境用 Redis: SETNX key value）
    private static final ConcurrentHashMap<String, Boolean> CONSUMED = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("IdempotentConsumerGroup");
        consumer.setNamesrvAddr("localhost:9876");
        consumer.subscribe("IdempotentTopic", "*");

        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            for (MessageExt msg : msgs) {
                String bizKey = msg.getKeys();
                // 幂等检查：判断是否已消费过
                if (CONSUMED.putIfAbsent(bizKey, true) != null) {
                    System.out.printf("[跳过] 重复消息: key=%s, msgId=%s%n", bizKey, msg.getMsgId());
                    continue;
                }
                // 首次消费，执行业务逻辑
                System.out.printf("[处理] 新消息: key=%s, body=%s%n",
                        bizKey, new String(msg.getBody()));
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        consumer.start();
        System.out.println("幂等消费者已启动...");
    }
}

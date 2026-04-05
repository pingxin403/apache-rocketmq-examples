package com.rocketmq.example.pop;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;

import java.nio.charset.StandardCharsets;

/**
 * 传统 Push 消费者 —— 与 Pop 消费模式对比
 *
 * ============================================================
 * Push 消费 vs Pop 消费 对比：
 * ============================================================
 *
 * ┌──────────────┬──────────────────────┬──────────────────────┐
 * │     维度      │    Push 消费模式      │    Pop 消费模式       │
 * ├──────────────┼──────────────────────┼──────────────────────┤
 * │ 负载均衡      │ 客户端 Rebalance      │ 服务端（Broker）调度   │
 * │ 队列绑定      │ 消费者独占队列         │ 无绑定，任意消费者可消费│
 * │ 状态          │ 有状态（持有队列锁）    │ 无状态                │
 * │ 消费方式      │ 被动回调 Listener      │ 主动 receive() 拉取   │
 * │ Rebalance    │ 有，可能导致消费暂停    │ 无 Rebalance          │
 * │ 消费确认      │ 返回 CONSUME_SUCCESS   │ 手动调用 ack()        │
 * │ 多语言支持    │ 仅 Java（Remoting）    │ 多语言（gRPC）        │
 * │ 消费者数限制  │ ≤ 队列数               │ 无限制                │
 * │ 适用场景      │ 高吞吐、低延迟         │ 弹性伸缩、Serverless  │
 * └──────────────┴──────────────────────┴──────────────────────┘
 *
 * Push 消费的痛点（Pop 消费解决的问题）：
 * 1. Rebalance 风暴：消费者上下线触发全组 Rebalance，导致短暂消费暂停
 * 2. 队列数限制：消费者数量不能超过队列数，否则有消费者空闲
 * 3. 单语言绑定：经典客户端仅支持 Java
 * 4. 有状态部署：消费者持有队列锁，不适合 Serverless 场景
 */
public class PushConsumer {

    /** NameServer 地址 */
    private static final String NAME_SERVER = "localhost:9876";

    /** 消费者组 */
    private static final String CONSUMER_GROUP = "PushConsumerGroup";

    /** 订阅的 Topic */
    private static final String TOPIC = "PopDemoTopic";

    public static void main(String[] args) throws Exception {
        // ========================================
        // 1. 创建传统 Push 消费者
        // ========================================
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(CONSUMER_GROUP);
        consumer.setNamesrvAddr(NAME_SERVER);

        // 从最新位点开始消费
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);

        // 订阅 Topic，* 表示所有 Tag
        consumer.subscribe(TOPIC, "*");

        // ========================================
        // 2. 注册消息监听器（被动回调模式）
        // ========================================
        // 与 Pop 消费的 receive() 主动拉取不同，
        // Push 模式通过注册 Listener 被动接收消息
        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            for (MessageExt msg : msgs) {
                String body = new String(msg.getBody(), StandardCharsets.UTF_8);
                System.out.printf("[Push消费] msgId=%s, queue=%d, body=%s%n",
                        msg.getMsgId(),
                        msg.getQueueId(),
                        body);
            }
            // 返回消费状态（与 Pop 的手动 ack 不同）
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        // ========================================
        // 3. 启动消费者
        // ========================================
        consumer.start();
        System.out.println("=== Push 消费者已启动（传统模式） ===");
        System.out.println("提示：Push 消费者通过客户端 Rebalance 绑定队列");
        System.out.println("按 Ctrl+C 退出...\n");

        // 保持运行
        Thread.sleep(Long.MAX_VALUE);

        consumer.shutdown();
    }
}

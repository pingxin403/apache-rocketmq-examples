package com.rocketmq.example.acl;

import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.remoting.RPCHook;

import java.nio.charset.StandardCharsets;

/**
 * 带 ACL 权限认证的消费者示例
 *
 * 使用说明：
 * 1. 消费者同样需要通过 AclClientRPCHook 传入 accessKey/secretKey
 * 2. Broker 会校验消费者是否有订阅对应 Topic 的权限
 * 3. 如果权限不足，消费者启动时会抛出 AclException
 *
 * 权限配置示例（plain_acl.yml）：
 *   accounts:
 *     - accessKey: YOUR_ACCESS_KEY
 *       secretKey: YOUR_SECRET_KEY
 *       topicPerms:
 *         - AclTestTopic=PUB|SUB    # 允许发布和订阅
 *       groupPerms:
 *         - AclConsumerGroup=SUB    # 允许该消费组订阅
 */
public class AclConsumer {
    public static void main(String[] args) throws Exception {
        // ========== 1. 创建 ACL 认证钩子 ==========
        // 消费者使用与生产者相同的认证方式
        // 实际生产中，生产者和消费者可以使用不同的 accessKey，分别授权
        RPCHook rpcHook = new AclClientRPCHook(
                new SessionCredentials("YOUR_ACCESS_KEY", "YOUR_SECRET_KEY"));

        // ========== 2. 创建消费者并传入认证钩子 ==========
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("AclConsumerGroup", rpcHook);
        consumer.setNamesrvAddr("localhost:9876");

        // 订阅 Topic（ACL 会校验该账号是否有 SUB 权限）
        consumer.subscribe("AclTestTopic", "*");

        // ========== 3. 注册消息监听器 ==========
        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            msgs.forEach(msg -> {
                String body = new String(msg.getBody(), StandardCharsets.UTF_8);
                System.out.printf("收到消息: msgId=%s, body=%s%n", msg.getMsgId(), body);
            });
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        consumer.start();
        System.out.println("ACL 消费者启动成功，等待消息...");

        // 保持运行，按 Ctrl+C 退出
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            consumer.shutdown();
            System.out.println("ACL 消费者已关闭");
        }));
    }
}

package com.rocketmq.example.acl;

import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.RPCHook;

import java.nio.charset.StandardCharsets;

/**
 * 带 ACL 权限认证的生产者示例
 *
 * 使用说明：
 * 1. Broker 端需要开启 ACL：在 broker.conf 中设置 aclEnable=true
 * 2. 配置 plain_acl.yml 文件，定义 accessKey/secretKey 及对应的 Topic 权限
 * 3. 客户端通过 AclClientRPCHook 传入凭证，每次 RPC 请求会自动携带签名
 *
 * ACL 认证流程：
 * - 客户端使用 secretKey 对请求内容进行 HMAC-SHA1 签名
 * - Broker 收到请求后，根据 accessKey 查找对应的 secretKey 进行验签
 * - 验签通过后，再检查该账号是否有操作对应 Topic 的权限
 */
public class AclProducer {
    public static void main(String[] args) throws Exception {
        // ========== 1. 创建 ACL 认证钩子 ==========
        // AclClientRPCHook 会在每次 RPC 调用前自动对请求进行签名
        // accessKey: 用于标识用户身份
        // secretKey: 用于生成请求签名，不会在网络上传输
        RPCHook rpcHook = new AclClientRPCHook(
                new SessionCredentials("YOUR_ACCESS_KEY", "YOUR_SECRET_KEY"));

        // ========== 2. 创建生产者并传入认证钩子 ==========
        // 第二个参数传入 rpcHook，生产者的所有请求都会携带 ACL 签名
        DefaultMQProducer producer = new DefaultMQProducer("AclProducerGroup", rpcHook);
        producer.setNamesrvAddr("localhost:9876");
        producer.start();

        System.out.println("ACL 生产者启动成功，开始发送消息...");

        // ========== 3. 发送消息（与普通发送完全一致） ==========
        // ACL 认证对业务代码透明，签名由 rpcHook 自动完成
        for (int i = 0; i < 5; i++) {
            Message msg = new Message("AclTestTopic", "TagA",
                    ("ACL认证消息-" + i).getBytes(StandardCharsets.UTF_8));
            SendResult result = producer.send(msg);
            System.out.printf("发送成功: msgId=%s, queue=%s%n",
                    result.getMsgId(), result.getMessageQueue());
        }

        producer.shutdown();
        System.out.println("ACL 生产者已关闭");
    }
}

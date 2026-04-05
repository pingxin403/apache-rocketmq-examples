# ACL 权限控制

本示例演示如何在 RocketMQ 客户端中使用 ACL（Access Control List）权限认证，保障消息系统的安全访问。

## 示例说明

| 类名 | 说明 |
|------|------|
| AclProducer | 带 ACL 认证的生产者，通过 AclClientRPCHook 自动签名 |
| AclConsumer | 带 ACL 认证的消费者，订阅时校验 SUB 权限 |

## 前置条件

### 1. Broker 开启 ACL

在 `broker.conf` 中添加：

```properties
aclEnable=true
```

### 2. 配置权限文件

编辑 Broker 的 `conf/plain_acl.yml`：

```yaml
globalWhiteRemoteAddresses:
  - 10.10.10.*

accounts:
  - accessKey: YOUR_ACCESS_KEY
    secretKey: YOUR_SECRET_KEY
    whiteRemoteAddress:
    admin: false
    defaultTopicPerm: DENY
    defaultGroupPerm: SUB
    topicPerms:
      - AclTestTopic=PUB|SUB
    groupPerms:
      - AclConsumerGroup=SUB
      - AclProducerGroup=PUB
```

### 3. 重启 Broker 使配置生效

## 核心原理

1. 客户端创建 `AclClientRPCHook`，传入 `accessKey` 和 `secretKey`
2. 每次 RPC 请求前，Hook 使用 `secretKey` 对请求内容进行 HMAC-SHA1 签名
3. Broker 收到请求后，根据 `accessKey` 查找对应的 `secretKey` 验签
4. 验签通过后，检查该账号对目标 Topic/Group 的操作权限（PUB/SUB/DENY）

## 运行方式

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.rocketmq.example.acl.AclProducer"
mvn exec:java -Dexec.mainClass="com.rocketmq.example.acl.AclConsumer"
```

## 常见问题

- `AclException: No accessKey is configured`：客户端未传入 rpcHook
- `AclException: CHECK_FAILED`：accessKey/secretKey 不匹配
- `AclException: No permissions`：账号无对应 Topic 的操作权限

## 对应文章

[权限控制ACL机制](../../../RocketMQ/06-运维与监控/36-权限控制ACL机制.md)

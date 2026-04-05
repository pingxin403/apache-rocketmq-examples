# 30 - Pop 消费模式示例

> 对应文章：[第30篇 - Pop 消费模式](../../../RocketMQ/05-消费模型与负载均衡/30-Pop消费模式.md)

## 概述

本示例演示 RocketMQ 5.x 引入的 **Pop 消费模式**，并与传统 Push 消费模式进行对比。

Pop 消费是 RocketMQ 5.0 的核心特性之一，它将负载均衡从客户端迁移到服务端（Broker），解决了传统 Rebalance 机制的诸多痛点。

## Pop 消费 vs Push 消费

```
┌──────────────┬──────────────────────┬──────────────────────┐
│     维度      │    Push 消费模式      │    Pop 消费模式       │
├──────────────┼──────────────────────┼──────────────────────┤
│ 负载均衡      │ 客户端 Rebalance      │ 服务端（Broker）调度   │
│ 队列绑定      │ 消费者独占队列         │ 无绑定，任意消费者可消费│
│ 状态          │ 有状态（持有队列锁）    │ 无状态                │
│ 消费方式      │ 被动回调 Listener      │ 主动 receive() 拉取   │
│ Rebalance    │ 有，可能导致消费暂停    │ 无 Rebalance          │
│ 消费确认      │ 返回 CONSUME_SUCCESS   │ 手动调用 ack()        │
│ 多语言支持    │ 仅 Java（Remoting）    │ 多语言（gRPC）        │
│ 消费者数限制  │ ≤ 队列数               │ 无限制                │
└──────────────┴──────────────────────┴──────────────────────┘
```

## 核心概念：invisibleDuration

Pop 消费引入了 `invisibleDuration`（不可见时间）机制：

```
消费者A receive() → 消息变为不可见（30s）
                     ├─ 30s 内 ack → 消息标记已消费 ✓
                     └─ 30s 内未 ack → 消息重新可见，可被其他消费者消费
```

这类似于 AWS SQS 的 VisibilityTimeout，确保消息不会因消费者宕机而丢失。

## 示例文件

| 文件 | 说明 |
|------|------|
| `PopConsumer.java` | Pop 消费模式（RocketMQ 5.x SimpleConsumer） |
| `SimpleProducer.java` | 配套生产者（经典 DefaultMQProducer） |
| `PushConsumer.java` | 传统 Push 消费者（对比用） |

## 前置条件

- RocketMQ 5.x Broker（需开启 gRPC Proxy，默认端口 8081）
- NameServer 运行在 `localhost:9876`
- gRPC Proxy 运行在 `localhost:8081`

## 运行步骤

### 1. 编译项目

```bash
cd apache-rocketmq-examples/05-消费模型与负载均衡/30-pop-consume
mvn clean compile
```

### 2. 发送测试消息

```bash
mvn exec:java -Dexec.mainClass="com.rocketmq.example.pop.SimpleProducer"
```

### 3. Pop 消费（推荐体验）

```bash
mvn exec:java -Dexec.mainClass="com.rocketmq.example.pop.PopConsumer"
```

### 4. Push 消费（对比体验）

```bash
mvn exec:java -Dexec.mainClass="com.rocketmq.example.pop.PushConsumer"
```

## 关键代码解读

### SimpleConsumer 创建

```java
SimpleConsumer consumer = provider.newSimpleConsumerBuilder()
        .setClientConfiguration(clientConfig)
        .setConsumerGroup(CONSUMER_GROUP)
        .setSubscriptionExpressions(Collections.singletonMap(TOPIC, filterExpression))
        .setAwaitDuration(Duration.ofSeconds(5))
        .build();
```

### 主动拉取 + 手动 ack

```java
// 主动拉取，指定不可见时间
List<MessageView> messages = consumer.receive(16, Duration.ofSeconds(30));

for (MessageView message : messages) {
    // 处理消息...
    consumer.ack(message);  // 手动确认
}
```

## Pop 消费适用场景

- **弹性伸缩**：消费者数量可以超过队列数，适合 K8s HPA 场景
- **Serverless**：无状态设计，适合 FaaS 函数计算
- **多语言**：gRPC 协议天然支持多语言客户端
- **避免 Rebalance 风暴**：消费者上下线不影响其他消费者

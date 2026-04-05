# RocketMQ 顺序消息示例

本示例演示了 RocketMQ 5.x 顺序消息的使用方法，包括分区顺序消息的发送和消费。

## 📚 相关文章

[《顺序消息真的顺序吗：全局顺序 vs 分区顺序》](https://github.com/pingxin403/RocketMQ/blob/main/02-消息特性深度篇/10-顺序消息真的顺序吗.md)

## 🎯 功能说明

### 1. 顺序消息生产者（OrderedProducer）

演示如何使用 `MessageQueueSelector` 将同一业务 ID 的消息路由到同一队列，保证消息顺序。

**核心代码**：
```java
SendResult result = producer.send(msg, new MessageQueueSelector() {
    @Override
    public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
        String orderId = (String) arg;
        int index = Math.abs(orderId.hashCode()) % mqs.size();
        return mqs.get(index);
    }
}, orderId);
```

### 2. 顺序消息消费者（OrderedConsumer）

演示如何使用 `MessageListenerOrderly` 保证消息顺序消费。

**核心代码**：
```java
consumer.registerMessageListener(new MessageListenerOrderly() {
    @Override
    public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs,
                                               ConsumeOrderlyContext context) {
        // 同一队列的消息串行消费
        for (MessageExt msg : msgs) {
            processMessage(msg);
        }
        return ConsumeOrderlyStatus.SUCCESS;
    }
});
```

### 3. IM 聊天消息场景（IMMessageProducer & IMMessageConsumer）

演示 IM 系统中如何保证同一会话的消息有序。

**场景说明**：
- 用户 A 和用户 B 的聊天消息需要保持顺序
- 不同会话的消息可以并发处理
- 使用 sessionId 作为路由键

## 🚀 快速开始

### 前置条件

1. 安装并启动 RocketMQ 5.x
2. 确保 NameServer 运行在 `localhost:9876`
3. JDK 1.8+
4. Maven 3.x+

### 创建 Topic

```bash
# 创建顺序消息 Topic（建议 4 个队列）
sh bin/mqadmin updateTopic -n localhost:9876 -t OrderedTopic -c DefaultCluster -r 4 -w 4

# 创建 IM 消息 Topic
sh bin/mqadmin updateTopic -n localhost:9876 -t IMTopic -c DefaultCluster -r 4 -w 4
```

### 编译项目

```bash
mvn clean package
```

### 运行示例

#### 1. 基础顺序消息示例

**启动消费者**：
```bash
mvn exec:java -Dexec.mainClass="com.rocketmq.example.ordered.OrderedConsumer"
```

**启动生产者**：
```bash
mvn exec:java -Dexec.mainClass="com.rocketmq.example.ordered.OrderedProducer"
```

**预期输出**：
```
发送顺序消息 - OrderId: ORDER_001, Status: 创建, Queue: 0
发送顺序消息 - OrderId: ORDER_002, Status: 创建, Queue: 1
发送顺序消息 - OrderId: ORDER_001, Status: 支付, Queue: 0
发送顺序消息 - OrderId: ORDER_002, Status: 支付, Queue: 1
发送顺序消息 - OrderId: ORDER_001, Status: 发货, Queue: 0

[线程:ConsumeMessageThread_1] 消费顺序消息 - OrderId: ORDER_001, 内容: ORDER_001 - 创建, Queue: 0
[线程:ConsumeMessageThread_1] 消费顺序消息 - OrderId: ORDER_001, 内容: ORDER_001 - 支付, Queue: 0
[线程:ConsumeMessageThread_1] 消费顺序消息 - OrderId: ORDER_001, 内容: ORDER_001 - 发货, Queue: 0
```

#### 2. IM 聊天消息示例

**启动消费者**：
```bash
mvn exec:java -Dexec.mainClass="com.rocketmq.example.ordered.IMMessageConsumer"
```

**启动生产者**：
```bash
mvn exec:java -Dexec.mainClass="com.rocketmq.example.ordered.IMMessageProducer"
```

**预期输出**：
```
=== 用户 A 发送消息 ===
发送聊天消息 - Session: USER_A_USER_B, 内容: 你好, Queue: 2
发送聊天消息 - Session: USER_A_USER_B, 内容: 在吗？, Queue: 2
发送聊天消息 - Session: USER_A_USER_B, 内容: 有个问题想请教, Queue: 2

=== 用户 B 回复消息 ===
发送聊天消息 - Session: USER_A_USER_B, 内容: 在的, Queue: 2
发送聊天消息 - Session: USER_A_USER_B, 内容: 什么问题？, Queue: 2

[会话:USER_A_USER_B] 收到消息: USER_A -> USER_B: 你好 (Queue: 2)
[会话:USER_A_USER_B] 收到消息: USER_A -> USER_B: 在吗？ (Queue: 2)
[会话:USER_A_USER_B] 收到消息: USER_A -> USER_B: 有个问题想请教 (Queue: 2)
[会话:USER_A_USER_B] 收到消息: USER_B -> USER_A: 在的 (Queue: 2)
[会话:USER_A_USER_B] 收到消息: USER_B -> USER_A: 什么问题？ (Queue: 2)
```

## 📖 核心概念

### 1. 全局顺序 vs 分区顺序

| 特性 | 全局顺序 | 分区顺序 |
|------|----------|----------|
| 队列数量 | 1个 | 多个 |
| 顺序范围 | 全局有序 | 分区内有序 |
| 并发度 | 无并发 | 多队列并发 |
| 性能 | 最低 | 较高 |
| 推荐度 | ⭐ | ⭐⭐⭐⭐⭐ |

### 2. 顺序消息的三个关键环节

1. **顺序发送**：使用 `MessageQueueSelector` 将同一业务 ID 的消息路由到同一队列
2. **顺序存储**：Broker 按照接收顺序存储消息
3. **顺序消费**：使用 `MessageListenerOrderly` 保证消息顺序消费

### 3. 性能对比

| 消息类型 | 发送 TPS | 消费 TPS | 延迟 |
|----------|----------|----------|------|
| 普通消息 | ~10万 | ~10万 | ~10ms |
| 顺序消息 | ~5万 | ~2万 | ~50ms |

## ⚠️ 注意事项

### 1. 队列数量设置

```java
// 队列数 = 预期 TPS / 单队列 TPS
// 例如：预期 10万 TPS，单队列 2万 TPS，则需要 5 个队列
```

### 2. 避免热点队列

```java
// ❌ 错误：所有消息都路由到同一队列
int index = 0;  // 固定队列
return mqs.get(index);

// ✅ 正确：根据业务 ID 均匀分布
int index = Math.abs(orderId.hashCode()) % mqs.size();
return mqs.get(index);
```

### 3. 消费失败处理

```java
@Override
public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs,
                                           ConsumeOrderlyContext context) {
    try {
        processMessage(msg);
        return ConsumeOrderlyStatus.SUCCESS;
    } catch (BusinessException e) {
        // 业务异常：跳过该消息
        return ConsumeOrderlyStatus.SUCCESS;
    } catch (Exception e) {
        // 系统异常：稍后重试（会阻塞队列）
        context.setSuspendCurrentQueueTimeMillis(1000);
        return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
    }
}
```

### 4. 幂等性保证

```java
// 检查消息是否已处理
if (isMessageProcessed(msg.getMsgId())) {
    return ConsumeOrderlyStatus.SUCCESS;
}

// 执行业务逻辑
processMessage(msg);

// 记录消息已处理
markMessageProcessed(msg.getMsgId());
```

## 🔍 常见问题

### Q1: 为什么消息还是乱序？

**A**: 检查以下几点：
1. 是否使用了 `MessageQueueSelector`？
2. 是否使用了 `MessageListenerOrderly`？
3. 同一业务 ID 的消息是否路由到了同一队列？

### Q2: 为什么队列被阻塞了？

**A**: 可能原因：
1. 某条消息消费失败，导致队列阻塞
2. 消费逻辑耗时过长
3. 消费者线程池满了

**解决方案**：
- 优化消费逻辑，减少耗时
- 设置合理的超时时间
- 增加消费者数量

### Q3: 顺序消息的性能为什么这么低？

**A**: 顺序消息的性能损耗主要来自：
1. 同一队列的消息必须串行消费
2. 消费失败会阻塞整个队列
3. 无法并发消费

**优化建议**：
- 增加队列数量
- 减少消费耗时
- 避免过度使用顺序消息

## 📚 延伸阅读

- [RocketMQ 官方文档 - 顺序消息](https://rocketmq.apache.org/docs/featureBehavior/03ordermessage)
- [《顺序消息真的顺序吗：全局顺序 vs 分区顺序》](https://github.com/pingxin403/RocketMQ/blob/main/02-消息特性深度篇/10-顺序消息真的顺序吗.md)

## 📝 License

Apache License 2.0

# RocketMQ 消费模式示例

这个示例演示了 RocketMQ 的两种消费模式：集群消费（Clustering）和广播消费（Broadcasting）。

## 前置条件

1. 已安装 JDK 8 或更高版本
2. 已安装并启动 RocketMQ（NameServer 和 Broker）

## 运行步骤

### 1. 启动 RocketMQ

```bash
# 启动 NameServer
nohup sh bin/mqnamesrv &

# 启动 Broker
nohup sh bin/mqbroker -n localhost:9876 &
```

### 2. 编译项目

```bash
mvn clean compile
```

### 3. 测试集群消费模式

**场景**：启动 3 个集群消费者实例，发送 9 条消息，观察每个消费者消费的消息数量。

**步骤**：

1. 在 3 个不同的终端窗口启动集群消费者：

```bash
# 终端 1
mvn exec:java -Dexec.mainClass="com.example.rocketmq.ClusteringConsumer"

# 终端 2
mvn exec:java -Dexec.mainClass="com.example.rocketmq.ClusteringConsumer"

# 终端 3
mvn exec:java -Dexec.mainClass="com.example.rocketmq.ClusteringConsumer"
```

2. 在第 4 个终端窗口运行生产者：

```bash
mvn exec:java -Dexec.mainClass="com.example.rocketmq.ConsumeModesProducer" -Dexec.args="clustering"
```

**预期结果**：
- 生产者发送 9 条消息
- 每个消费者大约消费 3 条消息（负载均衡）
- 总共 9 条消息被消费，每条消息只被一个消费者消费

### 4. 测试广播消费模式

**场景**：启动 3 个广播消费者实例，发送 3 条消息，观察每个消费者消费的消息数量。

**步骤**：

1. 在 3 个不同的终端窗口启动广播消费者：

```bash
# 终端 1
mvn exec:java -Dexec.mainClass="com.example.rocketmq.BroadcastingConsumer"

# 终端 2
mvn exec:java -Dexec.mainClass="com.example.rocketmq.BroadcastingConsumer"

# 终端 3
mvn exec:java -Dexec.mainClass="com.example.rocketmq.BroadcastingConsumer"
```

2. 在第 4 个终端窗口运行生产者：

```bash
mvn exec:java -Dexec.mainClass="com.example.rocketmq.ConsumeModesProducer" -Dexec.args="broadcasting"
```

**预期结果**：
- 生产者发送 3 条消息
- 每个消费者都消费 3 条消息（广播）
- 总共 9 次消费（3 个消费者 × 3 条消息）

## 代码说明

### 生产者（ConsumeModesProducer）

- 支持两种模式：`clustering` 和 `broadcasting`
- 集群消费模式：发送 9 条消息到 `ClusteringTopic`
- 广播消费模式：发送 3 条消息到 `BroadcastingTopic`

### 集群消费者（ClusteringConsumer）

- 消费模式：`MessageModel.CLUSTERING`（默认）
- 消费进度存储：Broker 端
- 支持消息重试和死信队列
- 支持负载均衡

### 广播消费者（BroadcastingConsumer）

- 消费模式：`MessageModel.BROADCASTING`
- 消费进度存储：消费者本地
- 不支持消息重试和死信队列
- 每个消费者都消费所有消息

## 核心差异对比

| 特性 | 集群消费 | 广播消费 |
|------|----------|----------|
| 消息分配 | 负载均衡 | 广播 |
| 消费进度存储 | Broker 端 | 消费者本地 |
| 消息重试 | 支持 | 不支持 |
| 死信队列 | 支持 | 不支持 |
| 适用场景 | 业务处理 | 配置更新 |

## 常见问题

**Q: 集群消费模式下，消费者数量大于队列数量会怎样？**

A: 部分消费者会空闲，不会分配到队列。建议消费者数量 ≤ 队列数量。

**Q: 广播消费模式下，消费失败怎么办？**

A: 广播消费不支持消息重试，消费失败的消息会丢失。建议在业务层实现重试逻辑。

**Q: 可以在同一个消费者组中混用两种消费模式吗？**

A: 不可以。同一个消费者组内的所有消费者实例必须使用相同的消费模式。

## 相关文章

详细讲解请参考：[集群消费vs广播消费：90%的人都选错了消费模式](../../RocketMQ/01-入门篇/07-集群消费vs广播消费.md)


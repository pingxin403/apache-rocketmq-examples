# 消息轨迹全生命周期追踪

本示例演示如何开启 RocketMQ 消息轨迹追踪，实现消息从发送到消费的全链路可观测。

## 示例说明

| 类名 | 说明 |
|------|------|
| TraceProducer | 开启轨迹的生产者，发送消息时自动记录轨迹 |
| TraceConsumer | 开启轨迹的消费者，消费消息时自动记录轨迹 |

## 前置条件

Broker 需要开启轨迹存储：`traceTopicEnable=true`

## 运行方式

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.rocketmq.example.trace.TraceProducer"
mvn exec:java -Dexec.mainClass="com.rocketmq.example.trace.TraceConsumer"
```

## 对应文章

[消息轨迹全生命周期追踪](../../../RocketMQ/02-消息特性深度篇/15-消息轨迹全生命周期追踪.md)

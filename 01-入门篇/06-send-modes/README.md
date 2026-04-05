# 消息发送的3种方式

本示例演示 RocketMQ 的三种消息发送方式：同步发送、异步发送、单向发送。

## 示例说明

| 类名 | 说明 |
|------|------|
| SyncProducer | 同步发送：等待 Broker 确认后返回，可靠性最高 |
| AsyncProducer | 异步发送：通过回调获取结果，吞吐量更高 |
| OnewayProducer | 单向发送：不等待响应，适合日志采集等场景 |

## 运行方式

```bash
mvn clean compile
# 同步发送
mvn exec:java -Dexec.mainClass="com.rocketmq.example.send.SyncProducer"
# 异步发送
mvn exec:java -Dexec.mainClass="com.rocketmq.example.send.AsyncProducer"
# 单向发送
mvn exec:java -Dexec.mainClass="com.rocketmq.example.send.OnewayProducer"
```

## 对应文章

[消息发送的3种方式](../../../RocketMQ/01-入门篇/06-消息发送的3种方式.md)

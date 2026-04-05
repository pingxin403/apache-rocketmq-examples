# 消息幂等性设计

本示例演示如何实现 RocketMQ 消息的幂等消费，避免重复消费导致的业务问题。

## 示例说明

| 类名 | 说明 |
|------|------|
| IdempotentProducer | 发送带业务唯一键（Keys）的消息 |
| IdempotentConsumer | 基于唯一键去重的幂等消费者 |

## 核心思路

1. 生产者为每条消息设置业务唯一键（如订单号）
2. 消费者消费前检查该键是否已处理过
3. 生产环境建议使用 Redis SETNX 或数据库唯一索引实现去重

## 运行方式

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.rocketmq.example.idempotent.IdempotentProducer"
mvn exec:java -Dexec.mainClass="com.rocketmq.example.idempotent.IdempotentConsumer"
```

## 对应文章

[消息幂等性设计](../../../RocketMQ/02-消息特性深度篇/14-消息幂等性设计.md)

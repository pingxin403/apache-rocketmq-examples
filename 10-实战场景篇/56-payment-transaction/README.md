# 支付中心的事务消息实践

本示例演示支付场景下如何使用 RocketMQ 事务消息，保证「扣款」与「通知下游」的原子性。

## 示例说明

| 项目 | 说明 |
|------|------|
| 场景 | 用户支付成功后，通过事务消息通知积分服务、通知服务等下游系统 |
| 核心技术 | `TransactionMQProducer` + `TransactionListener` |
| 一致性保证 | 本地扣款事务与消息发送的原子性 |
| 消费保证 | 幂等消费，防止重复发放积分 |

## 项目结构

```
56-payment-transaction/
├── pom.xml
├── README.md
└── src/main/java/com/rocketmq/example/payment/
    ├── PaymentTransactionProducer.java   # 支付事务消息生产者
    └── PaymentNotifyConsumer.java        # 支付通知消费者（积分服务）
```

## 核心流程

```
用户发起支付
    │
    ▼
PaymentTransactionProducer
    │
    ├── 1. 发送半消息（Half Message）到 Broker
    │
    ├── 2. executeLocalTransaction: 执行本地扣款
    │       ├── 扣款成功 → COMMIT_MESSAGE（消息对消费者可见）
    │       ├── 余额不足 → ROLLBACK_MESSAGE（消息丢弃）
    │       └── 异常     → UNKNOW（等待 Broker 回查）
    │
    └── 3. checkLocalTransaction: Broker 回查本地事务状态
            └── 查询本地事务表，返回 COMMIT / ROLLBACK / UNKNOW
    │
    ▼
PaymentNotifyConsumer（积分服务）
    ├── 幂等检查：paymentId 去重
    ├── 发放积分：1元 = 1积分
    └── 发送通知：模拟短信/Push
```

## 前置条件

1. **JDK 1.8+**
2. **Maven 3.x**
3. **RocketMQ 5.x** 已启动（NameServer + Broker）

## 运行方式

### 1. 编译

```bash
cd apache-rocketmq-examples/10-实战场景篇/56-payment-transaction
mvn clean package
```

### 2. 先启动消费者

```bash
mvn exec:java -Dexec.mainClass="com.rocketmq.example.payment.PaymentNotifyConsumer"
```

### 3. 再启动生产者

```bash
mvn exec:java -Dexec.mainClass="com.rocketmq.example.payment.PaymentTransactionProducer"
```

## 预期输出

### 生产者端

```
发送支付事务消息: paymentId=PAY_20240101_001, amount=99.90
[本地事务] 开始扣款: paymentId=PAY_20240101_001, amount=99.90
[本地事务] 扣款成功: paymentId=PAY_20240101_001, 余额=9900.10
发送结果: status=SEND_OK, localTx=COMMIT_MESSAGE

发送支付事务消息: paymentId=PAY_20240101_003, amount=50000.00
[本地事务] 余额不足，回滚: paymentId=PAY_20240101_003
发送结果: status=SEND_OK, localTx=ROLLBACK_MESSAGE
```

### 消费者端

```
[收到消息] paymentId=PAY_20240101_001, msgId=xxx
[积分发放] paymentId=PAY_20240101_001, 本次+100积分, 累计=100积分
[通知发送] 用户 U10086 支付成功
```

## 关键设计点

1. **事务消息保证原子性**：扣款和发消息要么都成功，要么都失败
2. **本地事务表**：记录事务状态，支持 Broker 回查
3. **幂等消费**：消费端通过 paymentId 去重，防止重复发放积分
4. **金额传递**：通过 `UserProperty` 传递金额，避免在消息体中重复解析

## 对应文章

- [《支付中心的事务消息实践》](../../../RocketMQ/10-实战场景篇/56-支付中心的事务消息实践.md)
- [《事务消息原理：分布式事务的优雅解法》](../../../RocketMQ/02-消息特性深度篇/08-事务消息原理.md)

## License

Apache License 2.0

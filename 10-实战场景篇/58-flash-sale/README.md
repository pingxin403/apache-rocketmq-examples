# 电商秒杀库存扣减

本示例演示如何使用 RocketMQ 实现秒杀场景的「削峰填谷」，将瞬时高并发请求转为平稳的串行消费。

## 示例说明

| 项目 | 说明 |
|------|------|
| 场景 | 100 个用户同时抢购 10 件商品，通过 MQ 削峰保护数据库 |
| 核心技术 | 高并发发送 + 顺序消费 + AtomicInteger 库存扣减 |
| 削峰效果 | 瞬时 100 QPS → 消费端 ~20 QPS，数据库压力降低 80% |
| 防超卖 | CAS 原子操作 + 用户去重 |

## 项目结构

```
58-flash-sale/
├── pom.xml
├── README.md
└── src/main/java/com/rocketmq/example/flashsale/
    ├── FlashSaleProducer.java    # 秒杀请求生产者（模拟高并发）
    └── FlashSaleConsumer.java    # 秒杀消费者（串行扣减库存）
```

## 核心流程

```
100 个用户同时发起秒杀请求
    │
    ▼
FlashSaleProducer（20线程并发发送）
    │  所有请求在 < 1秒 内全部写入 MQ
    │  用户立即收到"排队中"响应
    │
    ▼
RocketMQ Broker（消息缓冲区）
    │  削峰：将瞬时流量转为队列中的有序消息
    │
    ▼
FlashSaleConsumer（串行消费）
    ├── 去重检查：同一用户只能抢一次
    ├── 库存检查：AtomicInteger CAS 扣减
    ├── 库存 > 0 → 抢购成功（仅 10 人）
    └── 库存 = 0 → 返回"已售罄"（90 人）
```

## 前置条件

1. **JDK 1.8+**
2. **Maven 3.x**
3. **RocketMQ 5.x** 已启动（NameServer + Broker）

## 运行方式

### 1. 编译

```bash
cd apache-rocketmq-examples/10-实战场景篇/58-flash-sale
mvn clean package
```

### 2. 先启动消费者

```bash
mvn exec:java -Dexec.mainClass="com.rocketmq.example.flashsale.FlashSaleConsumer"
```

### 3. 再启动生产者（模拟秒杀）

```bash
mvn exec:java -Dexec.mainClass="com.rocketmq.example.flashsale.FlashSaleProducer"
```

## 预期输出

### 生产者端

```
========== 秒杀开始！100 人同时抢购 SKU_IPHONE_2024 ===========
[请求入队] userId=USER_0001, msgId=xxx
[请求入队] userId=USER_0002, msgId=xxx
...
========== 秒杀请求统计 ===========
总请求数: 100
入队成功: 100
入队失败: 0
总耗时:   523 ms
平均 QPS: 191
====================================
```

### 消费者端

```
[处理 #1] userId=USER_0023
[成功] 用户 USER_0023 抢购成功！剩余库存: 9

[处理 #2] userId=USER_0071
[成功] 用户 USER_0071 抢购成功！剩余库存: 8
...
[处理 #11] userId=USER_0045
[售罄] 库存已空，用户 USER_0045 抢购失败
...
========== 秒杀结果统计 ===========
总处理请求: 100
抢购成功:   10
库存售罄:   90
重复请求:   0
剩余库存:   0
====================================
```

## 削峰效果对比

| 指标 | 不用 MQ | 使用 MQ 削峰 |
|------|---------|-------------|
| 数据库瞬时 QPS | 100+ | ~20 |
| 响应时间 | 可能超时 | 毫秒级入队 |
| 超卖风险 | 高（并发竞争） | 低（串行消费） |
| 系统稳定性 | 可能崩溃 | 平稳运行 |

## 关键设计点

1. **CountDownLatch 模拟同时开抢**：所有线程在 startGate 处等待，模拟真实秒杀场景
2. **AtomicInteger CAS 扣减**：无锁操作，线程安全且高性能
3. **用户去重**：同一用户只能抢购一次，防止刷单
4. **顺序消费**：使用 `MessageListenerOrderly` 保证同一队列内串行处理

## 对应文章

- [《电商秒杀库存扣减》](../../../RocketMQ/10-实战场景篇/58-电商秒杀库存扣减.md)

## License

Apache License 2.0

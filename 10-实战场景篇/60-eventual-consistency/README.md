# 库存系统的最终一致性

本示例演示如何使用 RocketMQ 事务消息实现订单服务与库存服务之间的最终一致性。

## 示例说明

| 项目 | 说明 |
|------|------|
| 场景 | 订单服务下单后，通过事务消息通知库存服务扣减库存 |
| 核心技术 | 事务消息 + 幂等消费 + 本地事务日志 |
| 一致性模型 | 最终一致性（非强一致） |
| 失败处理 | 消息重试 + 幂等保证 + 补偿机制 |

## 项目结构

```
60-eventual-consistency/
├── pom.xml
├── README.md
└── src/main/java/com/rocketmq/example/consistency/
    ├── OrderProducer.java        # 订单服务（事务消息生产者）
    └── InventoryConsumer.java    # 库存服务（幂等消费者）
```

## 核心流程

```
用户下单
    │
    ▼
OrderProducer（订单服务）
    │
    ├── 1. 发送半消息：库存扣减指令
    │
    ├── 2. executeLocalTransaction: 本地事务
    │       ├── 创建订单记录
    │       ├── 记录事务日志（用于回查）
    │       ├── 成功 → COMMIT_MESSAGE
    │       ├── 失败 → ROLLBACK_MESSAGE
    │       └── 超时 → UNKNOW（等待回查）
    │
    └── 3. checkLocalTransaction: 查询事务日志表
    │
    ▼
InventoryConsumer（库存服务）
    ├── 幂等检查：orderId 去重（防止重复扣减）
    ├── 本地事务：扣减库存 + 记录消费日志
    ├── 成功 → CONSUME_SUCCESS
    └── 失败 → RECONSUME_LATER（自动重试）
```

## 最终一致性 vs 强一致性

| 对比项 | 强一致性（2PC） | 最终一致性（事务消息） |
|--------|----------------|----------------------|
| 一致性 | 实时一致 | 短暂不一致，最终一致 |
| 性能 | 低（锁定资源） | 高（异步解耦） |
| 可用性 | 低（协调者单点） | 高（MQ 高可用） |
| 复杂度 | 高 | 中 |
| 适用场景 | 金融核心 | 电商、积分、通知 |

## 前置条件

1. **JDK 1.8+**
2. **Maven 3.x**
3. **RocketMQ 5.x** 已启动（NameServer + Broker）

## 运行方式

### 1. 编译

```bash
cd apache-rocketmq-examples/10-实战场景篇/60-eventual-consistency
mvn clean package
```

### 2. 先启动库存消费者

```bash
mvn exec:java -Dexec.mainClass="com.rocketmq.example.consistency.InventoryConsumer"
```

### 3. 再启动订单生产者

```bash
mvn exec:java -Dexec.mainClass="com.rocketmq.example.consistency.OrderProducer"
```

## 预期输出

### 订单服务（生产者）

```
[下单] orderId=ORD_100001, product=SKU_001, qty=1
[本地事务] 开始创建订单: orderId=ORD_100001
[本地事务] 订单已创建: orderId=ORD_100001
[本地事务] 事务日志已记录: orderId=ORD_100001
[本地事务] 提交成功: orderId=ORD_100001
[发送结果] status=SEND_OK, localTx=COMMIT_MESSAGE

[下单] orderId=ORD_100003, product=SKU_003, qty=3
[本地事务] 模拟超时，返回 UNKNOWN: orderId=ORD_100003
...
[事务回查] orderId=ORD_100003, 事务日志状态=COMMIT
```

### 库存服务（消费者）

```
初始库存: {SKU_001=100, SKU_002=50, SKU_003=200}

[收到消息] orderId=ORD_100001, msgId=xxx
[扣减成功] orderId=ORD_100001, product=SKU_001, qty=-1, 剩余=99

[收到消息] orderId=ORD_100002, msgId=xxx
[扣减成功] orderId=ORD_100002, product=SKU_002, qty=-2, 剩余=48
```

## 关键设计点

1. **事务消息保证原子性**：订单创建与消息发送绑定在同一个事务流程中
2. **本地事务日志表**：独立于业务表，专门用于事务回查，避免业务表被污染
3. **幂等消费（三道防线）**：
   - 消费日志表去重（`CONSUME_LOG`）
   - CAS 库存扣减（防止超卖）
   - 失败时移除去重标记（允许重试）
4. **CAS 无锁扣减**：`AtomicInteger.compareAndSet` 保证并发安全
5. **补偿机制**：库存不足时可发送补偿消息通知订单服务取消订单

## 生产环境建议

```sql
-- 本地事务日志表
CREATE TABLE tx_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id VARCHAR(64) UNIQUE NOT NULL,
    status VARCHAR(16) NOT NULL COMMENT 'COMMIT/ROLLBACK',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 消费去重表
CREATE TABLE consume_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id VARCHAR(64) UNIQUE NOT NULL,
    consume_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
```

## 对应文章

- [《库存系统的最终一致性》](../../../RocketMQ/10-实战场景篇/60-库存系统的最终一致性.md)
- [《事务消息原理：分布式事务的优雅解法》](../../../RocketMQ/02-消息特性深度篇/08-事务消息原理.md)
- [《消息幂等性设计》](../../../RocketMQ/02-消息特性深度篇/14-消息幂等性设计.md)

## License

Apache License 2.0

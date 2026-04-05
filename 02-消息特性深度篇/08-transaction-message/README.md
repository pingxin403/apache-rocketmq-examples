# RocketMQ 事务消息示例

本示例演示了 RocketMQ 5.x 事务消息的完整使用流程，包括：
- 发送事务消息
- 执行本地事务
- 事务状态回查
- 消费事务消息

## 项目结构

```
08-transaction-message/
├── pom.xml                           # Maven 配置文件
├── README.md                         # 本文件
└── src/main/java/com/rocketmq/example/transaction/
    ├── TransactionProducer.java      # 事务消息生产者
    ├── TransactionConsumer.java      # 事务消息消费者
    ├── OrderService.java             # 订单服务（模拟本地事务）
    └── TransactionTable.java         # 本地事务表（模拟数据库）
```

## 前置条件

1. **JDK 1.8+**
2. **Maven 3.x**
3. **RocketMQ 5.x** 已启动（NameServer + Broker）

## 快速开始

### 1. 编译项目

```bash
mvn clean package
```

### 2. 启动消费者

```bash
java -cp target/transaction-message-1.0-SNAPSHOT.jar \
  com.rocketmq.example.transaction.TransactionConsumer
```

### 3. 启动生产者

在新的终端窗口中运行：

```bash
java -cp target/transaction-message-1.0-SNAPSHOT.jar \
  com.rocketmq.example.transaction.TransactionProducer
```

## 核心代码说明

### TransactionProducer

事务消息生产者，负责：
1. 发送半消息（Half Message）
2. 执行本地事务（扣款操作）
3. 根据本地事务结果提交或回滚消息
4. 响应 Broker 的事务回查请求

**关键方法**：
- `executeLocalTransaction()`: 执行本地事务
- `checkLocalTransaction()`: 事务状态回查

### TransactionConsumer

事务消息消费者，负责：
1. 订阅事务消息 Topic
2. 消费已提交的事务消息
3. 执行下游业务逻辑（扣减库存）

### OrderService

模拟订单服务，提供：
- `deductBalance()`: 扣款操作
- `checkTransactionStatus()`: 查询事务状态

### TransactionTable

模拟本地事务表，用于：
- 记录事务状态
- 支持事务回查

## 测试场景

### 场景1：本地事务成功

生产者输出：
```
发送事务消息: ORDER_1704096000000
执行本地事务，消息Key: ORDER_1704096000000
执行扣款操作: ORDER_1704096000000
本地事务执行成功，提交消息
```

消费者输出：
```
收到事务消息 - OrderId: ORDER_1704096000000
扣减库存: ORDER_1704096000000
库存扣减成功
```

### 场景2：本地事务失败

生产者输出：
```
发送事务消息: ORDER_1704096000001
执行本地事务，消息Key: ORDER_1704096000001
执行扣款操作: ORDER_1704096000001
本地事务执行失败，回滚消息
```

消费者输出：
```
（无输出，消息已回滚）
```

### 场景3：事务状态未知（触发回查）

生产者输出：
```
发送事务消息: ORDER_1704096000002
执行本地事务，消息Key: ORDER_1704096000002
执行扣款操作: ORDER_1704096000002
返回未知状态，等待回查

（等待约6秒后）
收到事务回查请求，消息Key: ORDER_1704096000002
查询事务状态: ORDER_1704096000002
回查结果：事务已提交
```

消费者输出：
```
收到事务消息 - OrderId: ORDER_1704096000002
扣减库存: ORDER_1704096000002
库存扣减成功
```

## 配置说明

### 事务超时配置

```java
// 设置事务超时时间（单位：秒）
producer.setTransactionTimeout(10);  // 默认6秒
```

### 回查配置

在 Broker 配置文件中设置：

```properties
# 事务消息回查间隔（单位：毫秒）
transactionCheckInterval=60000

# 事务消息最大回查次数
transactionCheckMax=15

# 事务消息超时时间（单位：毫秒）
transactionTimeout=6000
```

## 最佳实践

### 1. 本地事务表设计

建议在数据库中维护一张本地事务表：

```sql
CREATE TABLE local_transaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    transaction_id VARCHAR(64) UNIQUE NOT NULL,
    business_id VARCHAR(64) NOT NULL,
    status TINYINT NOT NULL COMMENT '0-处理中，1-已提交，2-已回滚',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL
) ENGINE=InnoDB;
```

### 2. 幂等性保证

消费端必须保证幂等性：

```java
// 检查是否已处理
if (isProcessed(orderId)) {
    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
}

// 执行业务逻辑
deductStock(orderId);

// 记录处理状态
markAsProcessed(orderId);
```

### 3. 消息Key设计

使用业务唯一ID作为消息Key：

```java
String key = "ORDER_" + orderId;  // ✅ 推荐
String key = UUID.randomUUID().toString();  // ✅ 推荐
String key = String.valueOf(System.currentTimeMillis());  // ❌ 不推荐
```

## 常见问题

### Q1: 回查不生效？

**原因**：`checkLocalTransaction()` 方法未正确实现

**解决**：确保该方法能够根据消息Key查询到本地事务状态

### Q2: 消息重复消费？

**原因**：网络重传或回查重试

**解决**：在消费端实现幂等性判断

### Q3: 事务超时？

**原因**：本地事务执行时间过长

**解决**：增大 `transactionTimeout` 参数

## 相关文档

- [RocketMQ 官方文档 - 事务消息](https://rocketmq.apache.org/docs/featureBehavior/04transactionmessage)
- [文章：《事务消息原理：分布式事务的优雅解法》](../../RocketMQ/02-消息特性深度篇/08-事务消息原理.md)

## License

Apache License 2.0

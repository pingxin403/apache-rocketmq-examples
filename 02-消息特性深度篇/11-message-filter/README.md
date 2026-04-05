# RocketMQ 消息过滤示例

本示例演示 RocketMQ 5.x 的两种消息过滤方式：**Tag 过滤**和 **SQL92 过滤**。

## 📋 目录结构

```
11-message-filter/
├── pom.xml
├── README.md
└── src/main/java/com/rocketmq/example/filter/
    ├── TagFilterProducer.java      # Tag 过滤 - 生产者
    ├── TagFilterConsumer1.java     # Tag 过滤 - 消费者1（库存服务）
    ├── TagFilterConsumer2.java     # Tag 过滤 - 消费者2（物流服务）
    ├── SqlFilterProducer.java      # SQL92 过滤 - 生产者
    ├── SqlFilterConsumer1.java     # SQL92 过滤 - 消费者1（大额订单）
    ├── SqlFilterConsumer2.java     # SQL92 过滤 - 消费者2（VIP 服务）
    └── SqlFilterConsumer3.java     # SQL92 过滤 - 消费者3（复杂条件）
```

## 🎯 功能说明

### Tag 过滤

**场景**：电商订单系统，不同服务只关心特定类型的订单消息

- **生产者**：发送不同 Tag 的订单消息（ORDER_CREATED、ORDER_PAID、ORDER_SHIPPED、ORDER_REFUND）
- **消费者1（库存服务）**：只消费 ORDER_CREATED 和 ORDER_REFUND 消息
- **消费者2（物流服务）**：只消费 ORDER_SHIPPED 消息

### SQL92 过滤

**场景**：电商订单系统，需要根据金额、VIP 等级、地区等多维度过滤

- **生产者**：发送带自定义属性的订单消息（amount、vip、region）
- **消费者1（大额订单）**：只消费金额大于 200 的订单
- **消费者2（VIP 服务）**：只消费 VIP 用户的订单
- **消费者3（复杂条件）**：消费北方地区且金额在 100-500 之间的订单

## 🚀 运行步骤

### 前置条件

1. 启动 RocketMQ NameServer 和 Broker
2. **重要**：如果要运行 SQL92 过滤示例，需要在 Broker 配置文件中开启 SQL 过滤支持：

```bash
# 编辑 broker.conf
enablePropertyFilter = true

# 重启 Broker
sh bin/mqbroker -n localhost:9876 -c conf/broker.conf
```

### 运行 Tag 过滤示例

**步骤1：启动消费者**

```bash
# 终端1：启动库存服务消费者
mvn exec:java -Dexec.mainClass="com.rocketmq.example.filter.TagFilterConsumer1"

# 终端2：启动物流服务消费者
mvn exec:java -Dexec.mainClass="com.rocketmq.example.filter.TagFilterConsumer2"
```

**步骤2：启动生产者**

```bash
# 终端3：启动生产者发送消息
mvn exec:java -Dexec.mainClass="com.rocketmq.example.filter.TagFilterProducer"
```

**预期结果**：

- 库存服务消费者只收到 ORDER_CREATED 和 ORDER_REFUND 消息
- 物流服务消费者只收到 ORDER_SHIPPED 消息

### 运行 SQL92 过滤示例

**步骤1：启动消费者**

```bash
# 终端1：启动大额订单消费者
mvn exec:java -Dexec.mainClass="com.rocketmq.example.filter.SqlFilterConsumer1"

# 终端2：启动 VIP 服务消费者
mvn exec:java -Dexec.mainClass="com.rocketmq.example.filter.SqlFilterConsumer2"

# 终端3：启动复杂条件消费者
mvn exec:java -Dexec.mainClass="com.rocketmq.example.filter.SqlFilterConsumer3"
```

**步骤2：启动生产者**

```bash
# 终端4：启动生产者发送消息
mvn exec:java -Dexec.mainClass="com.rocketmq.example.filter.SqlFilterProducer"
```

**预期结果**：

- 大额订单消费者只收到金额大于 200 的消息
- VIP 服务消费者只收到 VIP 用户的消息
- 复杂条件消费者只收到北方地区且金额在 100-500 之间的消息

## 📊 核心代码解析

### Tag 过滤

**生产者设置 Tag**：

```java
Message msg = new Message(
    "OrderTopic",           // Topic
    "ORDER_CREATED",        // Tag
    "订单创建".getBytes()    // Body
);
producer.send(msg);
```

**消费者订阅 Tag**：

```java
// 订阅单个 Tag
consumer.subscribe("OrderTopic", "ORDER_CREATED");

// 订阅多个 Tag（用 || 分隔）
consumer.subscribe("OrderTopic", "ORDER_CREATED || ORDER_REFUND");

// 订阅所有 Tag（使用通配符 *）
consumer.subscribe("OrderTopic", "*");
```

### SQL92 过滤

**生产者设置消息属性**：

```java
Message msg = new Message("OrderTopic", "ORDER", "订单内容".getBytes());

// 设置自定义属性
msg.putUserProperty("amount", "150");
msg.putUserProperty("vip", "true");
msg.putUserProperty("region", "NORTH");

producer.send(msg);
```

**消费者使用 SQL 表达式订阅**：

```java
// 简单条件
consumer.subscribe("OrderTopic", 
    MessageSelector.bySql("amount > 200"));

// 复杂条件
consumer.subscribe("OrderTopic", 
    MessageSelector.bySql("region = 'NORTH' AND amount BETWEEN 100 AND 500"));
```

## 🔍 支持的 SQL 运算符

| 运算符 | 说明 | 示例 |
|--------|------|------|
| = | 等于 | `type = 'ORDER'` |
| <> 或 != | 不等于 | `type <> 'REFUND'` |
| > | 大于 | `amount > 100` |
| >= | 大于等于 | `amount >= 100` |
| < | 小于 | `amount < 1000` |
| <= | 小于等于 | `amount <= 1000` |
| AND | 逻辑与 | `type = 'ORDER' AND amount > 100` |
| OR | 逻辑或 | `type = 'ORDER' OR type = 'REFUND'` |
| NOT | 逻辑非 | `NOT (amount < 100)` |
| BETWEEN | 范围 | `amount BETWEEN 100 AND 1000` |
| IN | 包含 | `type IN ('ORDER', 'REFUND')` |
| IS NULL | 为空 | `remark IS NULL` |
| IS NOT NULL | 不为空 | `remark IS NOT NULL` |

## ⚠️ 注意事项

1. **SQL 过滤需要 Broker 支持**：必须在 Broker 配置文件中设置 `enablePropertyFilter = true`
2. **Tag 过滤性能更高**：Tag 过滤性能优于 SQL92 过滤约 20%，优先使用 Tag 过滤
3. **SQL 表达式尽量简单**：复杂的 SQL 表达式会增加 Broker 的 CPU 消耗
4. **字符串用单引号**：SQL 表达式中的字符串必须用单引号包围，如 `'ORDER'`
5. **数值不用引号**：数值类型不需要引号，如 `amount > 100`
6. **不能混用**：一个消费者只能选择一种过滤方式，不能同时使用 Tag 和 SQL 过滤

## 📚 相关文章

- [《Tag过滤 vs SQL92过滤：消息过滤的艺术》](../../RocketMQ/02-消息特性深度篇/11-Tag过滤vs-SQL92过滤.md)

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

Apache License 2.0

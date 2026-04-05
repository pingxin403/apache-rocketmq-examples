# RocketMQ 延迟消息示例

本示例演示了 RocketMQ 5.x 延迟消息的完整使用流程，包括：
- RocketMQ 4.x 的18级固定延迟
- RocketMQ 5.x 的任意时间延迟
- 订单超时自动取消场景
- 延迟消息的消费

## 项目结构

```
09-delay-message/
├── pom.xml                                    # Maven 配置文件
├── README.md                                  # 本文件
└── src/main/java/com/rocketmq/example/delay/
    ├── FixedDelayProducer.java                # 固定延迟生产者（4.x）
    ├── ArbitraryDelayProducer.java            # 任意时间延迟生产者（5.x）
    ├── DelayConsumer.java                     # 延迟消息消费者
    ├── OrderTimeoutProducer.java              # 订单超时场景生产者
    ├── OrderTimeoutConsumer.java              # 订单超时场景消费者
    └── OrderService.java                      # 订单服务（模拟业务逻辑）
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
java -cp target/delay-message-1.0-SNAPSHOT.jar:target/lib/* \
  com.rocketmq.example.delay.DelayConsumer
```

### 3. 测试固定延迟（RocketMQ 4.x 兼容模式）

在新的终端窗口中运行：

```bash
java -cp target/delay-message-1.0-SNAPSHOT.jar:target/lib/* \
  com.rocketmq.example.delay.FixedDelayProducer
```

### 4. 测试任意时间延迟（RocketMQ 5.x）

```bash
java -cp target/delay-message-1.0-SNAPSHOT.jar:target/lib/* \
  com.rocketmq.example.delay.ArbitraryDelayProducer
```

### 5. 测试订单超时场景

启动订单超时消费者：

```bash
java -cp target/delay-message-1.0-SNAPSHOT.jar:target/lib/* \
  com.rocketmq.example.delay.OrderTimeoutConsumer
```

启动订单超时生产者：

```bash
java -cp target/delay-message-1.0-SNAPSHOT.jar:target/lib/* \
  com.rocketmq.example.delay.OrderTimeoutProducer
```

## 核心代码说明

### FixedDelayProducer

固定延迟生产者，演示 RocketMQ 4.x 的18级固定延迟：
- 支持18个固定的延迟级别（1秒、5秒、10秒...2小时）
- 使用 `setDelayTimeLevel()` 方法设置延迟级别
- 兼容 RocketMQ 4.x 和 5.x

### ArbitraryDelayProducer

任意时间延迟生产者，演示 RocketMQ 5.x 的任意时间延迟：
- 支持任意时间延迟（精确到毫秒）
- 使用 `setDeliverTimeMs()` 方法设置投递时间
- 最大延迟时间可达40天
- 仅支持 RocketMQ 5.x

### DelayConsumer

延迟消息消费者，负责：
1. 订阅延迟消息 Topic
2. 消费到期的延迟消息
3. 计算实际延迟时间
4. 执行业务逻辑

### OrderTimeoutProducer

订单超时场景生产者，演示：
- 创建订单
- 发送延迟消息（30分钟后检查订单状态）
- 模拟真实的订单超时场景

### OrderTimeoutConsumer

订单超时场景消费者，负责：
- 接收延迟消息
- 查询订单状态
- 取消未支付订单
- 释放库存

### OrderService

订单服务，提供：
- `createOrder()`: 创建订单
- `getOrderStatus()`: 查询订单状态
- `cancelOrder()`: 取消订单
- `releaseStock()`: 释放库存

## 测试场景

### 场景1：固定延迟（18级）

生产者输出：
```
发送延迟消息: ORDER_1704096000000
延迟级别: 16 (30分钟)
消息已发送，30分钟后投递
```

消费者输出（30分钟后）：
```
收到延迟消息 - OrderId: ORDER_1704096000000
实际延迟: 1800秒 (30分钟)
订单已取消
```

### 场景2：任意时间延迟

生产者输出：
```
发送延迟消息: ORDER_1704096000001
延迟时间: 37分钟
消息已发送，37分钟后投递
```

消费者输出（37分钟后）：
```
收到延迟消息 - OrderId: ORDER_1704096000001
实际延迟: 2220秒 (37分钟)
订单已取消
```

### 场景3：订单超时自动取消

生产者输出：
```
创建订单: ORDER_1704096000002
订单状态: UNPAID
发送延迟消息: 30分钟后检查订单状态
```

消费者输出（30分钟后）：
```
检查订单状态: ORDER_1704096000002
订单状态: UNPAID
订单超时未支付，开始取消
取消订单: ORDER_1704096000002
释放库存: ORDER_1704096000002
订单已取消，库存已释放
```

## 18级固定延迟对照表

| 延迟级别 | 延迟时间 | 延迟级别 | 延迟时间 |
|---------|---------|---------|---------|
| 1 | 1秒 | 10 | 6分钟 |
| 2 | 5秒 | 11 | 7分钟 |
| 3 | 10秒 | 12 | 8分钟 |
| 4 | 30秒 | 13 | 9分钟 |
| 5 | 1分钟 | 14 | 10分钟 |
| 6 | 2分钟 | 15 | 20分钟 |
| 7 | 3分钟 | 16 | 30分钟 |
| 8 | 4分钟 | 17 | 1小时 |
| 9 | 5分钟 | 18 | 2小时 |

## 配置说明

### Broker 配置

在 Broker 配置文件中启用延迟消息：

```properties
# 启用定时消息（RocketMQ 5.x）
enableScheduleMessageStats=true

# 定时消息精度（单位：毫秒）
timerPrecisionMs=100

# 定时消息最大延迟时间（单位：毫秒，默认40天）
timerMaxDelaySec=3456000
```

### 生产者配置

```java
// 设置 NameServer 地址
producer.setNamesrvAddr("localhost:9876");

// 设置发送超时时间
producer.setSendMsgTimeout(3000);

// 设置重试次数
producer.setRetryTimesWhenSendFailed(2);
```

### 消费者配置

```java
// 设置 NameServer 地址
consumer.setNamesrvAddr("localhost:9876");

// 设置消费模式（集群消费）
consumer.setMessageModel(MessageModel.CLUSTERING);

// 设置消费线程数
consumer.setConsumeThreadMin(20);
consumer.setConsumeThreadMax(20);
```

## 最佳实践

### 1. 选择合适的延迟方式

| 场景 | 推荐方式 | 原因 |
|------|---------|------|
| 延迟时间固定（如30分钟） | 18级固定延迟 | 性能更好，兼容性强 |
| 延迟时间任意（如37分钟） | 任意时间延迟 | 灵活性高 |
| 延迟时间超过2小时 | 任意时间延迟 | 固定延迟最大2小时 |
| RocketMQ 4.x 环境 | 18级固定延迟 | 不支持任意时间延迟 |

### 2. 幂等性保证

消费端必须保证幂等性：

```java
// 检查是否已处理
if (isOrderCancelled(orderId)) {
    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
}

// 执行业务逻辑
cancelOrder(orderId);
```

### 3. 延迟时间计算

使用绝对时间而非相对时间：

```java
// ✅ 推荐：使用绝对时间
long deliverTimeMs = orderCreateTime + 30 * 60 * 1000;
msg.setDeliverTimeMs(deliverTimeMs);

// ❌ 不推荐：使用相对时间
long deliverTimeMs = System.currentTimeMillis() + 30 * 60 * 1000;
msg.setDeliverTimeMs(deliverTimeMs);
```

### 4. 延迟消息监控

监控以下指标：
- 延迟消息堆积量
- 延迟精度（实际投递时间与预期时间的差值）
- 消费延迟

```java
// 计算延迟精度
long expectedDeliverTime = msg.getDeliverTimeMs();
long actualDeliverTime = System.currentTimeMillis();
long delayError = actualDeliverTime - expectedDeliverTime;
System.out.println("延迟误差: " + delayError + "ms");
```

### 5. 延迟消息取消

RocketMQ 不支持直接取消延迟消息，可以通过消费端判断：

```java
// 查询订单状态
String orderStatus = getOrderStatus(orderId);

if ("PAID".equals(orderStatus)) {
    // 订单已支付，无需取消
    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
}

// 执行取消逻辑
cancelOrder(orderId);
```

## 常见问题

### Q1: 延迟消息未投递？

**原因**：Broker 未启用延迟消息功能

**解决**：检查 Broker 配置文件，确保 `enableScheduleMessageStats=true`

### Q2: 延迟时间不准确？

**原因**：系统时钟不同步或 Broker 负载过高

**解决**：
1. 同步系统时钟（使用 NTP）
2. 增加 Broker 资源
3. 检查延迟消息堆积情况

### Q3: 任意时间延迟不生效？

**原因**：RocketMQ 版本低于 5.0

**解决**：升级到 RocketMQ 5.x

### Q4: 延迟消息堆积？

**原因**：消费速度慢于生产速度

**解决**：
1. 增加消费者数量
2. 提高消费线程数
3. 优化消费逻辑

## 延迟消息的18般武艺

1. **订单超时自动取消**：下单后30分钟未支付自动取消
2. **支付超时关闭**：发起支付后15分钟未完成自动关闭
3. **消息重试**：消费失败后延迟重试（指数退避）
4. **定时提醒**：会议前10分钟提醒参会人员
5. **延迟通知**：用户注册后24小时发送激活邮件
6. **限流削峰**：将突发流量延迟处理
7. **优惠券过期提醒**：过期前3天提醒用户使用
8. **会员到期提醒**：到期前7天提醒续费
9. **还款日提醒**：还款日前3天提醒用户还款
10. **订单自动确认收货**：发货后7天自动确认收货
11. **试用期到期处理**：试用期结束后自动转为付费或停用
12. **定时发布内容**：预约发布文章、视频等内容
13. **延迟扣费**：服务使用后延迟扣费
14. **异步任务调度**：延迟执行数据统计、报表生成等任务
15. **延迟日志清理**：日志保留30天后自动清理
16. **延迟数据同步**：延迟同步数据到数据仓库
17. **延迟缓存预热**：活动开始前预热缓存
18. **延迟降级恢复**：系统降级后延迟恢复正常服务

## 相关文档

- [RocketMQ 官方文档 - 延迟消息](https://rocketmq.apache.org/docs/featureBehavior/02delaymessage)
- [文章：《延迟消息的18般武艺：从定时任务到精准触达》](../../RocketMQ/02-消息特性深度篇/09-延迟消息的18般武艺.md)

## License

Apache License 2.0

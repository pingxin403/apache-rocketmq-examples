# RocketMQ Hello World 示例

这是一个最简单的 RocketMQ 生产者和消费者示例，帮助你快速上手 RocketMQ。

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

### 3. 运行消费者

先启动消费者，等待接收消息：

```bash
mvn exec:java -Dexec.mainClass="com.example.rocketmq.HelloWorldConsumer"
```

### 4. 运行生产者

在另一个终端窗口运行生产者，发送消息：

```bash
mvn exec:java -Dexec.mainClass="com.example.rocketmq.HelloWorldProducer"
```

## 预期结果

**生产者输出：**
```
生产者启动成功
消息发送成功: SendResult [sendStatus=SEND_OK, msgId=...]
生产者已关闭
```

**消费者输出：**
```
消费者启动成功，等待接收消息...
收到消息: Topic=HelloWorldTopic, Tag=TagA, MsgId=..., Body=Hello RocketMQ!
```

## 代码说明

### 生产者关键步骤
1. 创建生产者实例并指定生产者组
2. 设置 NameServer 地址
3. 启动生产者
4. 创建消息（指定 Topic、Tag、消息体）
5. 发送消息
6. 关闭生产者

### 消费者关键步骤
1. 创建消费者实例并指定消费者组
2. 设置 NameServer 地址
3. 订阅 Topic 和 Tag
4. 注册消息监听器处理消息
5. 启动消费者

## 常见问题

**Q: 消费者收不到消息？**
- 检查 NameServer 和 Broker 是否正常运行
- 确认 NameServer 地址配置正确
- 确保消费者先于生产者启动

**Q: 连接超时？**
- 检查防火墙设置
- 确认 9876 端口（NameServer）和 10911 端口（Broker）可访问

## 相关文章

详细讲解请参考：[5分钟跑通第一个RocketMQ](../../RocketMQ/module-01-入门篇/01-5分钟跑通第一个RocketMQ.md)

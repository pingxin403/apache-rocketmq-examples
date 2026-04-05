# Apache RocketMQ 示例代码仓库

[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

本仓库包含《RocketMQ 从入门到精通》系列文章的所有代码示例。这是一个覆盖 RocketMQ 5.x 核心特性和最佳实践的完整示例集合，帮助开发者快速上手并深入理解 RocketMQ。

## 📚 关于本仓库

本仓库是《RocketMQ 从入门到精通》72 篇系列文章的配套代码库，所有示例代码均基于 **RocketMQ 5.x** 版本编写，涵盖从基础入门到高级特性的完整学习路径。

### 特点

- ✅ **完整可运行**：每个示例都是独立的 Maven 项目，可直接运行
- ✅ **详细注释**：代码包含详细的中文注释，便于理解
- ✅ **最佳实践**：遵循 RocketMQ 官方推荐的最佳实践
- ✅ **循序渐进**：从简单到复杂，适合不同水平的开发者

## 🚀 快速开始

### 环境要求

- JDK 8 或更高版本
- Maven 3.6 或更高版本
- RocketMQ 5.x 服务端（本地或远程）

### 启动 RocketMQ

如果你还没有运行中的 RocketMQ 服务，可以参考以下步骤快速启动：

```bash
# 下载 RocketMQ 5.x
wget https://dist.apache.org/repos/dist/release/rocketmq/5.1.0/rocketmq-all-5.1.0-bin-release.zip
unzip rocketmq-all-5.1.0-bin-release.zip
cd rocketmq-all-5.1.0-bin-release

# 启动 NameServer
nohup sh bin/mqnamesrv &

# 启动 Broker
nohup sh bin/mqbroker -n localhost:9876 &
```

### 运行示例

每个示例都是独立的 Maven 项目，可以按以下方式运行：

```bash
# 进入示例目录
cd 01-入门篇/01-hello-world

# 编译项目
mvn clean compile

# 运行生产者
mvn exec:java -Dexec.mainClass="com.rocketmq.example.quickstart.Producer"

# 运行消费者（新开一个终端）
mvn exec:java -Dexec.mainClass="com.rocketmq.example.quickstart.Consumer"
```

## 📖 模块导航

### 模块一：入门篇

基础概念和快速入门示例，适合初学者。

| 序号 | 文章标题 | 代码示例 | 说明 |
|------|---------|---------|------|
| 01 | 5分钟跑通第一个RocketMQ | [01-hello-world](./01-入门篇/01-hello-world) | 最简单的生产者和消费者示例 |
| 07 | 集群消费vs广播消费 | [07-consume-modes](./01-入门篇/07-consume-modes) | 演示集群消费和广播消费的区别 |

### 模块二：消息特性深度篇

深入理解 RocketMQ 的核心消息特性。

| 序号 | 文章标题 | 代码示例 | 说明 |
|------|---------|---------|------|
| 08 | 事务消息原理 | [08-transaction-message](./02-消息特性深度篇/08-transaction-message) | 事务消息的完整实现 |
| 09 | 延迟消息的18般武艺 | [09-delay-message](./02-消息特性深度篇/09-delay-message) | 延迟消息和定时消息示例 |
| 10 | 顺序消息真的顺序吗 | [10-ordered-message](./02-消息特性深度篇/10-ordered-message) | 全局顺序和分区顺序消息 |
| 11 | Tag过滤vs SQL92过滤 | [11-message-filter](./02-消息特性深度篇/11-message-filter) | Tag 过滤和 SQL 过滤示例 |
| 12 | 批量消息设计哲学 | [12-batch-message](./02-消息特性深度篇/12-batch-message) | 批量发送消息示例 |
| 13 | 消息重试与死信队列 | [13-retry-dlq](./02-消息特性深度篇/13-retry-dlq) | 重试机制和死信队列处理 |
| 14 | 消息幂等性设计 | [14-idempotent](./02-消息特性深度篇/14-idempotent) | 幂等性消费实现方案 |
| 15 | 消息轨迹全生命周期追踪 | [15-message-trace](./02-消息特性深度篇/15-message-trace) | 消息轨迹追踪示例 |

## 🛠️ 项目结构

每个示例项目都遵循统一的结构：

```
示例目录/
├── pom.xml                    # Maven 配置文件
├── README.md                  # 示例说明文档
└── src/
    └── main/
        └── java/
            └── com/rocketmq/example/
                ├── Producer.java      # 生产者示例
                └── Consumer.java      # 消费者示例
```

## 📝 配置说明

### NameServer 地址配置

所有示例默认连接到 `localhost:9876`。如果你的 RocketMQ 部署在其他地址，请修改代码中的 NameServer 地址：

```java
producer.setNamesrvAddr("your-nameserver:9876");
consumer.setNamesrvAddr("your-nameserver:9876");
```

### 常见配置项

```java
// 生产者配置
producer.setSendMsgTimeout(3000);              // 发送超时时间
producer.setRetryTimesWhenSendFailed(2);       // 同步发送失败重试次数
producer.setRetryTimesWhenSendAsyncFailed(2);  // 异步发送失败重试次数

// 消费者配置
consumer.setConsumeThreadMin(20);              // 最小消费线程数
consumer.setConsumeThreadMax(64);              // 最大消费线程数
consumer.setConsumeMessageBatchMaxSize(1);     // 批量消费消息数量
```

## 🔧 常见问题

### 1. 连接 NameServer 失败

**问题**：`connect to <172.17.0.1:9876> failed`

**解决方案**：
- 检查 NameServer 是否正常启动：`jps | grep NamesrvStartup`
- 检查防火墙是否开放 9876 端口
- 确认 NameServer 地址配置正确

### 2. 发送消息超时

**问题**：`sendDefaultImpl call timeout`

**解决方案**：
- 检查 Broker 是否正常启动：`jps | grep BrokerStartup`
- 检查网络连接是否正常
- 增加发送超时时间：`producer.setSendMsgTimeout(5000)`

### 3. 消费者无法消费消息

**问题**：消费者启动后没有收到消息

**解决方案**：
- 检查 Topic 是否存在：`sh bin/mqadmin topicList -n localhost:9876`
- 检查消费者组名是否正确
- 确认消息是否发送成功
- 检查消费者订阅的 Tag 是否匹配

### 4. Maven 依赖下载失败

**问题**：无法下载 RocketMQ 依赖

**解决方案**：
- 配置 Maven 镜像源（如阿里云镜像）
- 检查网络连接
- 清理本地 Maven 仓库：`mvn clean`

## 📚 学习路径建议

### 初学者路径

1. **入门篇**：从 01-hello-world 开始，理解基本概念
2. **消息特性**：学习事务消息、延迟消息、顺序消息
3. **实战场景**：结合实际业务场景理解应用

### 进阶路径

1. **存储机制**：深入理解 CommitLog、ConsumeQueue
2. **高可用**：学习主从同步、DLedger
3. **性能调优**：JVM 调优、系统参数优化

### 专家路径

1. **源码解析**：阅读核心模块源码
2. **云原生**：Kubernetes 部署、Operator
3. **生态集成**：与 Flink、Canal 等集成

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 提交 Issue

如果你发现代码问题或有改进建议，请：

1. 描述问题或建议
2. 提供复现步骤（如果是 Bug）
3. 说明你的环境信息（JDK 版本、RocketMQ 版本等）

### 提交 Pull Request

1. Fork 本仓库
2. 创建你的特性分支：`git checkout -b feature/your-feature`
3. 提交你的修改：`git commit -am 'Add some feature'`
4. 推送到分支：`git push origin feature/your-feature`
5. 提交 Pull Request

## 📄 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。

## 🔗 相关链接

- [Apache RocketMQ 官网](https://rocketmq.apache.org/)
- [RocketMQ GitHub](https://github.com/apache/rocketmq)
- [RocketMQ 官方文档](https://rocketmq.apache.org/docs/quick-start/)
- [系列文章地址](https://github.com/pingxin403/RocketMQ)

## 💬 交流与反馈

如果你在学习过程中遇到问题，或者有任何建议，欢迎：

- 提交 [Issue](https://github.com/pingxin403/apache-rocketmq-examples/issues)
- 参与 [Discussions](https://github.com/pingxin403/apache-rocketmq-examples/discussions)

---

⭐ 如果这个项目对你有帮助，欢迎 Star 支持！

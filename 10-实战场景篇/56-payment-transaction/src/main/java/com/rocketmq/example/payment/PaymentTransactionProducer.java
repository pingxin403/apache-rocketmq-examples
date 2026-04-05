package com.rocketmq.example.payment;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 支付中心 - 事务消息生产者
 *
 * 业务场景：
 *   用户支付成功后，需要通知下游系统（积分服务、通知服务等）。
 *   使用 RocketMQ 事务消息保证「扣款」与「发消息」的原子性：
 *     1. 发送半消息（Half Message）到 Broker
 *     2. 执行本地扣款事务
 *     3. 根据扣款结果提交或回滚消息
 *     4. Broker 回查时查询本地扣款状态
 *
 * 核心流程：
 *   Producer --半消息--> Broker（不可见）
 *   Producer 执行本地扣款
 *   扣款成功 -> COMMIT（消息对消费者可见）
 *   扣款失败 -> ROLLBACK（消息丢弃）
 *   超时未确认 -> Broker 回查 checkLocalTransaction
 */
public class PaymentTransactionProducer {

    /** 模拟本地事务表：paymentId -> 事务状态（COMMIT / ROLLBACK / UNKNOWN） */
    private static final ConcurrentHashMap<String, String> LOCAL_TRANS_TABLE = new ConcurrentHashMap<>();

    /** 模拟账户余额 */
    private static BigDecimal accountBalance = new BigDecimal("10000.00");

    public static void main(String[] args) throws Exception {
        // ========== 1. 创建事务消息生产者 ==========
        TransactionMQProducer producer = new TransactionMQProducer("payment_tx_producer_group");
        producer.setNamesrvAddr("localhost:9876");

        // ========== 2. 注册事务监听器 ==========
        producer.setTransactionListener(new TransactionListener() {

            /**
             * 执行本地事务 —— 半消息发送成功后回调
             * 这里模拟扣款操作
             */
            @Override
            public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
                String paymentId = msg.getKeys();
                BigDecimal amount = new BigDecimal(msg.getUserProperty("amount"));
                System.out.printf("[本地事务] 开始扣款: paymentId=%s, amount=%s%n", paymentId, amount);

                try {
                    // ---------- 模拟扣款 ----------
                    synchronized (accountBalance) {
                        if (accountBalance.compareTo(amount) >= 0) {
                            accountBalance = accountBalance.subtract(amount);
                            // 记录事务状态为已提交
                            LOCAL_TRANS_TABLE.put(paymentId, "COMMIT");
                            System.out.printf("[本地事务] 扣款成功: paymentId=%s, 余额=%s%n",
                                    paymentId, accountBalance);
                            return LocalTransactionState.COMMIT_MESSAGE;
                        } else {
                            // 余额不足，回滚
                            LOCAL_TRANS_TABLE.put(paymentId, "ROLLBACK");
                            System.out.printf("[本地事务] 余额不足，回滚: paymentId=%s%n", paymentId);
                            return LocalTransactionState.ROLLBACK_MESSAGE;
                        }
                    }
                } catch (Exception e) {
                    // 异常时返回 UNKNOW，等待 Broker 回查
                    LOCAL_TRANS_TABLE.put(paymentId, "UNKNOWN");
                    System.err.printf("[本地事务] 扣款异常，等待回查: paymentId=%s, error=%s%n",
                            paymentId, e.getMessage());
                    return LocalTransactionState.UNKNOW;
                }
            }

            /**
             * 事务回查 —— Broker 在超时未收到确认时回调
             * 查询本地事务表判断扣款是否成功
             */
            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                String paymentId = msg.getKeys();
                String status = LOCAL_TRANS_TABLE.getOrDefault(paymentId, "UNKNOWN");
                System.out.printf("[事务回查] paymentId=%s, 本地状态=%s%n", paymentId, status);

                switch (status) {
                    case "COMMIT":
                        return LocalTransactionState.COMMIT_MESSAGE;
                    case "ROLLBACK":
                        return LocalTransactionState.ROLLBACK_MESSAGE;
                    default:
                        return LocalTransactionState.UNKNOW;
                }
            }
        });

        // ========== 3. 启动生产者 ==========
        producer.start();
        System.out.println("支付事务消息生产者已启动\n");

        // ========== 4. 模拟多笔支付 ==========
        String[] paymentIds = {"PAY_20240101_001", "PAY_20240101_002", "PAY_20240101_003"};
        BigDecimal[] amounts = {
                new BigDecimal("99.90"),
                new BigDecimal("199.00"),
                new BigDecimal("50000.00")  // 这笔会因余额不足而回滚
        };

        for (int i = 0; i < paymentIds.length; i++) {
            String paymentId = paymentIds[i];
            BigDecimal amount = amounts[i];

            // 构建消息：Topic = PaymentTopic，Tag = PaySuccess
            Message msg = new Message("PaymentTopic", "PaySuccess",
                    paymentId,
                    String.format("{\"paymentId\":\"%s\",\"amount\":%s,\"userId\":\"U10086\"}",
                            paymentId, amount).getBytes());
            // 将金额放入用户属性，方便本地事务读取
            msg.putUserProperty("amount", amount.toPlainString());

            System.out.println("========================================");
            System.out.printf("发送支付事务消息: paymentId=%s, amount=%s%n", paymentId, amount);

            TransactionSendResult result = producer.sendMessageInTransaction(msg, null);
            System.out.printf("发送结果: status=%s, localTx=%s%n",
                    result.getSendStatus(), result.getLocalTransactionState());
            Thread.sleep(500);
        }

        // 保持进程运行，等待可能的回查
        System.out.println("\n等待事务回查（可按 Ctrl+C 退出）...");
        Thread.sleep(120000);
        producer.shutdown();
    }
}

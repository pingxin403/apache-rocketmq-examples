package com.rocketmq.example.payment;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 支付通知消费者 —— 模拟下游积分服务
 *
 * 业务场景：
 *   监听 PaymentTopic，当支付成功消息投递后：
 *     1. 解析支付信息（paymentId、amount、userId）
 *     2. 根据支付金额发放积分（1元 = 1积分）
 *     3. 通过幂等机制防止重复发放
 *
 * 消费保证：
 *   - 幂等消费：使用 paymentId 去重，防止重复发放积分
 *   - 失败重试：消费失败返回 RECONSUME_LATER，RocketMQ 自动重试
 */
public class PaymentNotifyConsumer {

    /** 模拟去重表：记录已处理的 paymentId（生产环境用 Redis SETNX 或数据库唯一键） */
    private static final ConcurrentHashMap<String, Boolean> PROCESSED = new ConcurrentHashMap<>();

    /** 模拟用户积分余额 */
    private static final AtomicLong userPoints = new AtomicLong(0);

    public static void main(String[] args) throws Exception {
        // ========== 1. 创建消费者 ==========
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("payment_notify_consumer_group");
        consumer.setNamesrvAddr("localhost:9876");

        // ========== 2. 订阅支付成功消息 ==========
        // 只消费 Tag 为 PaySuccess 的消息
        consumer.subscribe("PaymentTopic", "PaySuccess");

        // ========== 3. 注册消息监听器 ==========
        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            for (MessageExt msg : msgs) {
                String paymentId = msg.getKeys();
                String body = new String(msg.getBody());

                System.out.println("\n========================================");
                System.out.printf("[收到消息] paymentId=%s, msgId=%s, 重试次数=%d%n",
                        paymentId, msg.getMsgId(), msg.getReconsumeTimes());
                System.out.printf("[消息内容] %s%n", body);

                // ---------- 幂等检查 ----------
                if (PROCESSED.putIfAbsent(paymentId, true) != null) {
                    System.out.printf("[跳过] 重复消息，已处理: paymentId=%s%n", paymentId);
                    continue;
                }

                try {
                    // ---------- 业务逻辑：发放积分 ----------
                    // 简单规则：支付金额（元）= 积分数
                    // 实际场景中从消息体 JSON 解析 amount
                    long points = parsePoints(body);
                    long totalPoints = userPoints.addAndGet(points);

                    System.out.printf("[积分发放] paymentId=%s, 本次+%d积分, 累计=%d积分%n",
                            paymentId, points, totalPoints);

                    // ---------- 模拟发送支付成功通知（短信/Push） ----------
                    System.out.printf("[通知发送] 用户 U10086 支付成功，金额见消息体%n");

                } catch (Exception e) {
                    // 处理失败，移除去重标记，允许重试
                    PROCESSED.remove(paymentId);
                    System.err.printf("[异常] paymentId=%s, error=%s%n", paymentId, e.getMessage());
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        // ========== 4. 启动消费者 ==========
        consumer.start();
        System.out.println("支付通知消费者（积分服务）已启动，等待消息...\n");

        // 保持进程运行
        Thread.sleep(Long.MAX_VALUE);
    }

    /**
     * 从消息体中解析积分数
     * 简化实现：提取 amount 字段的整数部分作为积分
     */
    private static long parsePoints(String body) {
        try {
            // 简单解析 JSON 中的 amount 字段
            int idx = body.indexOf("\"amount\":");
            if (idx >= 0) {
                String sub = body.substring(idx + 9).trim();
                // 取到逗号或右花括号之前的数字
                int end = sub.indexOf(",");
                if (end < 0) end = sub.indexOf("}");
                String amountStr = sub.substring(0, end).trim();
                return Math.round(Double.parseDouble(amountStr));
            }
        } catch (Exception e) {
            System.err.println("解析积分失败: " + e.getMessage());
        }
        return 0;
    }
}

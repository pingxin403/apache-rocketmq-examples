package com.rocketmq.example.delay;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 延迟消息消费者
 * 接收并处理延迟消息
 */
public class DelayConsumer {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public static void main(String[] args) throws Exception {
        // 1. 创建消费者
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("delay_consumer_group");
        consumer.setNamesrvAddr("localhost:9876");
        
        // 2. 订阅延迟消息 Topic
        consumer.subscribe("DelayTopic", "*");
        
        // 3. 注册消息监听器
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                            ConsumeConcurrentlyContext context) {
                for (MessageExt msg : msgs) {
                    String orderId = msg.getKeys();
                    String body = new String(msg.getBody());
                    
                    // 计算实际延迟时间
                    long bornTime = msg.getBornTimestamp();
                    long consumeTime = System.currentTimeMillis();
                    long actualDelay = (consumeTime - bornTime) / 1000;
                    
                    System.out.println("========================================");
                    System.out.printf("收到延迟消息 - OrderId: %s%n", orderId);
                    System.out.printf("  消息内容: %s%n", body);
                    System.out.printf("  发送时间: %s%n", DATE_FORMAT.format(new Date(bornTime)));
                    System.out.printf("  消费时间: %s%n", DATE_FORMAT.format(new Date(consumeTime)));
                    System.out.printf("  实际延迟: %d秒 (%.2f分钟)%n", actualDelay, actualDelay / 60.0);
                    System.out.printf("  MsgId: %s%n", msg.getMsgId());
                    
                    try {
                        // 执行业务逻辑：取消订单
                        cancelOrder(orderId);
                        System.out.println("  处理结果: 订单已取消");
                        
                        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                    } catch (Exception e) {
                        System.err.println("  处理结果: 订单取消失败 - " + e.getMessage());
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    } finally {
                        System.out.println("========================================\n");
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        
        // 4. 启动消费者
        consumer.start();
        System.out.println("=== 延迟消息消费者已启动 ===");
        System.out.println("等待接收延迟消息...\n");
    }
    
    /**
     * 取消订单
     */
    private static void cancelOrder(String orderId) {
        // 实际场景中，这里应该是数据库操作
        // 例如：UPDATE orders SET status = 'CANCELLED' WHERE order_id = ?
        System.out.println("  执行取消订单: " + orderId);
    }
}

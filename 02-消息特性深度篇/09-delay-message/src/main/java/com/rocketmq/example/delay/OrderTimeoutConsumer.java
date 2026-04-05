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
 * 订单超时场景消费者
 * 接收延迟消息，检查订单状态并执行取消逻辑
 */
public class OrderTimeoutConsumer {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public static void main(String[] args) throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("order_timeout_consumer_group");
        consumer.setNamesrvAddr("localhost:9876");
        consumer.subscribe("OrderTimeoutTopic", "*");
        
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                            ConsumeConcurrentlyContext context) {
                for (MessageExt msg : msgs) {
                    String orderId = msg.getKeys();
                    
                    System.out.println("========================================");
                    System.out.println("收到订单超时检查消息");
                    System.out.println("  OrderId: " + orderId);
                    System.out.println("  检查时间: " + DATE_FORMAT.format(new Date()));
                    
                    // 查询订单状态
                    String orderStatus = OrderService.getOrderStatus(orderId);
                    System.out.println("  订单状态: " + orderStatus);
                    
                    if ("UNPAID".equals(orderStatus)) {
                        // 订单未支付，执行取消逻辑
                        System.out.println("  处理逻辑: 订单超时未支付，开始取消");
                        
                        try {
                            OrderService.cancelOrder(orderId);
                            OrderService.releaseStock(orderId);
                            
                            System.out.println("  处理结果: 订单已取消，库存已释放");
                        } catch (Exception e) {
                            System.err.println("  处理结果: 订单取消失败 - " + e.getMessage());
                            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                        }
                    } else if ("PAID".equals(orderStatus)) {
                        // 订单已支付，无需处理
                        System.out.println("  处理逻辑: 订单已支付，无需取消");
                    } else if ("CANCELLED".equals(orderStatus)) {
                        // 订单已取消，无需处理
                        System.out.println("  处理逻辑: 订单已取消，无需处理");
                    } else {
                        System.out.println("  处理逻辑: 订单状态异常 - " + orderStatus);
                    }
                    
                    System.out.println("========================================\n");
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        
        consumer.start();
        System.out.println("=== 订单超时检查消费者已启动 ===");
        System.out.println("等待接收订单超时检查消息...\n");
    }
}

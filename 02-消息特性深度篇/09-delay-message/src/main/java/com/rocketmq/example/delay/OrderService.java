package com.rocketmq.example.delay;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单服务
 * 模拟订单相关的业务逻辑
 */
public class OrderService {
    
    // 模拟订单数据库（实际场景应该使用真实数据库）
    private static final Map<String, String> ORDER_STATUS_MAP = new ConcurrentHashMap<>();
    
    /**
     * 创建订单
     */
    public static void createOrder(String orderId) {
        // 实际场景：INSERT INTO orders (order_id, status, create_time) VALUES (?, 'UNPAID', NOW())
        ORDER_STATUS_MAP.put(orderId, "UNPAID");
        System.out.println("创建订单: " + orderId + ", 状态: UNPAID");
    }
    
    /**
     * 查询订单状态
     */
    public static String getOrderStatus(String orderId) {
        // 实际场景：SELECT status FROM orders WHERE order_id = ?
        String status = ORDER_STATUS_MAP.getOrDefault(orderId, "UNPAID");
        System.out.println("查询订单状态: " + orderId + " -> " + status);
        return status;
    }
    
    /**
     * 取消订单
     */
    public static void cancelOrder(String orderId) {
        // 实际场景：UPDATE orders SET status = 'CANCELLED', update_time = NOW() WHERE order_id = ?
        ORDER_STATUS_MAP.put(orderId, "CANCELLED");
        System.out.println("取消订单: " + orderId);
    }
    
    /**
     * 支付订单
     */
    public static void payOrder(String orderId) {
        // 实际场景：UPDATE orders SET status = 'PAID', pay_time = NOW() WHERE order_id = ?
        ORDER_STATUS_MAP.put(orderId, "PAID");
        System.out.println("支付订单: " + orderId);
    }
    
    /**
     * 释放库存
     */
    public static void releaseStock(String orderId) {
        // 实际场景：UPDATE stock SET quantity = quantity + 1 WHERE product_id = ?
        System.out.println("释放库存: " + orderId);
    }
    
    /**
     * 扣减库存
     */
    public static void deductStock(String orderId) {
        // 实际场景：UPDATE stock SET quantity = quantity - 1 WHERE product_id = ?
        System.out.println("扣减库存: " + orderId);
    }
    
    /**
     * 检查订单是否已取消
     */
    public static boolean isOrderCancelled(String orderId) {
        String status = ORDER_STATUS_MAP.get(orderId);
        return "CANCELLED".equals(status);
    }
    
    /**
     * 检查订单是否已支付
     */
    public static boolean isOrderPaid(String orderId) {
        String status = ORDER_STATUS_MAP.get(orderId);
        return "PAID".equals(status);
    }
}

package com.rocketmq.example.transaction;

import java.util.HashMap;
import java.util.Map;

/**
 * 订单服务（模拟本地事务）
 * 
 * 功能说明：
 * 1. 模拟扣款操作
 * 2. 查询事务状态
 * 
 * 实际场景中，这里应该是真实的数据库操作
 * 
 * @author RocketMQ Example
 */
public class OrderService {
    
    // 模拟数据库：存储订单的支付状态
    // Key: 订单ID, Value: 是否已支付
    private static final Map<String, Boolean> orderPaymentStatus = new HashMap<>();
    
    /**
     * 扣款操作
     * 
     * 实际场景中应该是数据库操作：
     * UPDATE account SET balance = balance - amount WHERE user_id = ? AND balance >= amount
     * 
     * @param orderId 订单ID
     * @return true-扣款成功，false-扣款失败
     */
    public static boolean deductBalance(String orderId) {
        System.out.println("执行扣款操作: " + orderId);
        
        try {
            // 模拟数据库操作耗时
            Thread.sleep(50);
            
            // 模拟：80% 成功，20% 失败
            boolean success = Math.random() > 0.2;
            
            if (success) {
                // 记录支付状态
                synchronized (orderPaymentStatus) {
                    orderPaymentStatus.put(orderId, true);
                }
                System.out.println("扣款成功: " + orderId);
            } else {
                System.err.println("扣款失败: " + orderId + "（余额不足或账户异常）");
            }
            
            return success;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("扣款操作被中断: " + orderId);
            return false;
        }
    }
    
    /**
     * 查询事务状态
     * 
     * 实际场景中应该查询数据库：
     * SELECT status FROM orders WHERE order_id = ?
     * 
     * @param orderId 订单ID
     * @return true-已支付，false-未支付
     */
    public static boolean checkTransactionStatus(String orderId) {
        System.out.println("查询事务状态: " + orderId);
        
        synchronized (orderPaymentStatus) {
            Boolean isPaid = orderPaymentStatus.get(orderId);
            
            if (isPaid != null && isPaid) {
                System.out.println("订单已支付: " + orderId);
                return true;
            } else {
                System.out.println("订单未支付: " + orderId);
                return false;
            }
        }
    }
    
    /**
     * 获取订单支付状态（用于测试）
     * 
     * @param orderId 订单ID
     * @return 支付状态
     */
    public static String getOrderStatus(String orderId) {
        synchronized (orderPaymentStatus) {
            Boolean isPaid = orderPaymentStatus.get(orderId);
            
            if (isPaid == null) {
                return "未知";
            } else if (isPaid) {
                return "已支付";
            } else {
                return "未支付";
            }
        }
    }
}

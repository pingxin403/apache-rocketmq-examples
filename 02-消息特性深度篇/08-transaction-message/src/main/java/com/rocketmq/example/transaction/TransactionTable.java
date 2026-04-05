package com.rocketmq.example.transaction;

import java.util.HashMap;
import java.util.Map;

/**
 * 本地事务表（模拟数据库）
 * 
 * 功能说明：
 * 1. 记录事务状态
 * 2. 查询事务状态
 * 3. 更新事务状态
 * 
 * 实际场景中，应该使用真实的数据库表：
 * 
 * CREATE TABLE local_transaction (
 *     id BIGINT PRIMARY KEY AUTO_INCREMENT,
 *     transaction_id VARCHAR(64) UNIQUE NOT NULL COMMENT '事务ID（消息Key）',
 *     business_type VARCHAR(32) NOT NULL COMMENT '业务类型',
 *     business_id VARCHAR(64) NOT NULL COMMENT '业务ID（如订单ID）',
 *     status TINYINT NOT NULL COMMENT '事务状态：0-处理中，1-已提交，2-已回滚',
 *     create_time DATETIME NOT NULL,
 *     update_time DATETIME NOT NULL,
 *     INDEX idx_transaction_id (transaction_id),
 *     INDEX idx_business_id (business_id)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
 * 
 * @author RocketMQ Example
 */
public class TransactionTable {
    
    // 事务状态常量
    public static final int STATUS_UNKNOWN = 0;   // 处理中/未知
    public static final int STATUS_COMMIT = 1;    // 已提交
    public static final int STATUS_ROLLBACK = 2;  // 已回滚
    
    // 模拟数据库表：存储事务状态
    // Key: 事务ID（订单ID）, Value: 事务状态
    private static final Map<String, Integer> transactionStatus = new HashMap<>();
    
    /**
     * 记录事务状态
     * 
     * 实际场景中应该是数据库操作：
     * INSERT INTO local_transaction (transaction_id, business_id, status, create_time, update_time)
     * VALUES (?, ?, ?, NOW(), NOW())
     * 
     * @param transactionId 事务ID
     * @param status 事务状态
     */
    public static void recordTransaction(String transactionId, int status) {
        synchronized (transactionStatus) {
            transactionStatus.put(transactionId, status);
        }
        
        System.out.println("记录事务状态: " + transactionId + " -> " + getStatusName(status));
    }
    
    /**
     * 查询事务状态
     * 
     * 实际场景中应该是数据库操作：
     * SELECT status FROM local_transaction WHERE transaction_id = ?
     * 
     * @param transactionId 事务ID
     * @return 事务状态
     */
    public static int queryTransactionStatus(String transactionId) {
        synchronized (transactionStatus) {
            Integer status = transactionStatus.get(transactionId);
            
            if (status == null) {
                System.out.println("查询事务状态: " + transactionId + " -> 未找到记录");
                return STATUS_UNKNOWN;
            }
            
            System.out.println("查询事务状态: " + transactionId + " -> " + getStatusName(status));
            return status;
        }
    }
    
    /**
     * 更新事务状态
     * 
     * 实际场景中应该是数据库操作：
     * UPDATE local_transaction SET status = ?, update_time = NOW() WHERE transaction_id = ?
     * 
     * @param transactionId 事务ID
     * @param status 新的事务状态
     */
    public static void updateTransactionStatus(String transactionId, int status) {
        synchronized (transactionStatus) {
            transactionStatus.put(transactionId, status);
        }
        
        System.out.println("更新事务状态: " + transactionId + " -> " + getStatusName(status));
    }
    
    /**
     * 删除事务记录（用于测试）
     * 
     * @param transactionId 事务ID
     */
    public static void deleteTransaction(String transactionId) {
        synchronized (transactionStatus) {
            transactionStatus.remove(transactionId);
        }
        
        System.out.println("删除事务记录: " + transactionId);
    }
    
    /**
     * 清空所有事务记录（用于测试）
     */
    public static void clearAll() {
        synchronized (transactionStatus) {
            transactionStatus.clear();
        }
        
        System.out.println("清空所有事务记录");
    }
    
    /**
     * 获取事务状态名称
     * 
     * @param status 事务状态
     * @return 状态名称
     */
    private static String getStatusName(int status) {
        switch (status) {
            case STATUS_UNKNOWN:
                return "处理中/未知";
            case STATUS_COMMIT:
                return "已提交";
            case STATUS_ROLLBACK:
                return "已回滚";
            default:
                return "未知状态";
        }
    }
    
    /**
     * 打印所有事务记录（用于调试）
     */
    public static void printAll() {
        synchronized (transactionStatus) {
            System.out.println("\n========== 本地事务表 ==========");
            if (transactionStatus.isEmpty()) {
                System.out.println("（无记录）");
            } else {
                transactionStatus.forEach((id, status) -> {
                    System.out.println(id + " -> " + getStatusName(status));
                });
            }
            System.out.println("================================\n");
        }
    }
}

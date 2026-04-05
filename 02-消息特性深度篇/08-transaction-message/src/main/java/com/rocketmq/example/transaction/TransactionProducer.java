package com.rocketmq.example.transaction;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

/**
 * RocketMQ 事务消息生产者示例
 * 
 * 功能说明：
 * 1. 发送事务消息（半消息）
 * 2. 执行本地事务（扣款操作）
 * 3. 根据本地事务结果提交或回滚消息
 * 4. 响应 Broker 的事务回查请求
 * 
 * @author RocketMQ Example
 */
public class TransactionProducer {
    
    public static void main(String[] args) throws Exception {
        // 1. 创建事务消息生产者
        TransactionMQProducer producer = new TransactionMQProducer("transaction_producer_group");
        producer.setNamesrvAddr("localhost:9876");
        
        // 设置事务超时时间（单位：秒）
        // 如果本地事务执行时间较长，建议增大此值
        producer.setTransactionTimeout(10);
        
        // 2. 设置事务监听器
        producer.setTransactionListener(new TransactionListenerImpl());
        
        // 3. 启动生产者
        producer.start();
        System.out.println("事务消息生产者已启动");
        
        // 4. 发送多条事务消息，模拟不同场景
        sendTransactionMessage(producer, "ORDER_SUCCESS_001", true);   // 场景1：本地事务成功
        Thread.sleep(1000);
        
        sendTransactionMessage(producer, "ORDER_FAIL_002", false);     // 场景2：本地事务失败
        Thread.sleep(1000);
        
        sendTransactionMessage(producer, "ORDER_UNKNOWN_003", null);   // 场景3：状态未知，触发回查
        
        // 保持进程运行，等待回查
        System.out.println("\n等待事务回查（约6秒后）...");
        Thread.sleep(60000);
        
        // 5. 关闭生产者
        producer.shutdown();
        System.out.println("事务消息生产者已关闭");
    }
    
    /**
     * 发送事务消息
     * 
     * @param producer 生产者实例
     * @param orderId 订单ID
     * @param transactionResult 本地事务结果（true-成功，false-失败，null-未知）
     */
    private static void sendTransactionMessage(TransactionMQProducer producer, 
                                              String orderId, 
                                              Boolean transactionResult) throws Exception {
        System.out.println("\n========================================");
        System.out.println("发送事务消息: " + orderId);
        
        // 创建消息
        Message msg = new Message(
            "TransactionTopic",                          // Topic
            "PaymentTag",                                 // Tag
            orderId,                                      // Key（用于回查时识别）
            ("支付订单: " + orderId).getBytes()           // 消息体
        );
        
        // 发送事务消息
        // 第二个参数会传递给 executeLocalTransaction 方法
        TransactionSendResult result = producer.sendMessageInTransaction(msg, transactionResult);
        
        System.out.println("事务消息发送结果: " + result.getSendStatus());
        System.out.println("本地事务状态: " + result.getLocalTransactionState());
    }
    
    /**
     * 事务监听器实现
     */
    static class TransactionListenerImpl implements TransactionListener {
        
        /**
         * 执行本地事务
         * 
         * 在发送半消息成功后被调用
         * 
         * @param msg 消息对象
         * @param arg sendMessageInTransaction 方法的第二个参数
         * @return 本地事务状态
         */
        @Override
        public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
            String orderId = msg.getKeys();
            System.out.println("执行本地事务，订单ID: " + orderId);
            
            try {
                // 从参数中获取事务结果（实际场景中应该执行真实的业务逻辑）
                Boolean transactionResult = (Boolean) arg;
                
                if (transactionResult == null) {
                    // 模拟：执行事务后返回未知状态（用于测试回查）
                    OrderService.deductBalance(orderId);
                    TransactionTable.recordTransaction(orderId, TransactionTable.STATUS_UNKNOWN);
                    
                    System.out.println("本地事务执行完成，返回未知状态，等待回查");
                    return LocalTransactionState.UNKNOW;
                    
                } else if (transactionResult) {
                    // 模拟：本地事务成功
                    boolean success = OrderService.deductBalance(orderId);
                    if (success) {
                        TransactionTable.recordTransaction(orderId, TransactionTable.STATUS_COMMIT);
                        System.out.println("本地事务执行成功，提交消息");
                        return LocalTransactionState.COMMIT_MESSAGE;
                    } else {
                        TransactionTable.recordTransaction(orderId, TransactionTable.STATUS_ROLLBACK);
                        System.out.println("本地事务执行失败，回滚消息");
                        return LocalTransactionState.ROLLBACK_MESSAGE;
                    }
                    
                } else {
                    // 模拟：本地事务失败
                    TransactionTable.recordTransaction(orderId, TransactionTable.STATUS_ROLLBACK);
                    System.out.println("本地事务执行失败，回滚消息");
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                
            } catch (Exception e) {
                System.err.println("本地事务执行异常: " + e.getMessage());
                e.printStackTrace();
                
                // 返回 UNKNOW，等待 Broker 回查
                return LocalTransactionState.UNKNOW;
            }
        }
        
        /**
         * 事务状态回查
         * 
         * 当 Broker 长时间未收到事务状态确认时被调用
         * 
         * @param msg 消息对象
         * @return 本地事务状态
         */
        @Override
        public LocalTransactionState checkLocalTransaction(MessageExt msg) {
            String orderId = msg.getKeys();
            System.out.println("\n收到事务回查请求，订单ID: " + orderId);
            
            try {
                // 查询本地事务表，获取事务状态
                int status = TransactionTable.queryTransactionStatus(orderId);
                
                switch (status) {
                    case TransactionTable.STATUS_COMMIT:
                        System.out.println("回查结果：事务已提交");
                        return LocalTransactionState.COMMIT_MESSAGE;
                        
                    case TransactionTable.STATUS_ROLLBACK:
                        System.out.println("回查结果：事务已回滚");
                        return LocalTransactionState.ROLLBACK_MESSAGE;
                        
                    case TransactionTable.STATUS_UNKNOWN:
                    default:
                        // 如果状态仍然未知，可以再次查询业务系统
                        // 这里简化处理，直接提交
                        boolean isCommitted = OrderService.checkTransactionStatus(orderId);
                        if (isCommitted) {
                            TransactionTable.updateTransactionStatus(orderId, TransactionTable.STATUS_COMMIT);
                            System.out.println("回查结果：事务已提交（从业务系统查询）");
                            return LocalTransactionState.COMMIT_MESSAGE;
                        } else {
                            System.out.println("回查结果：状态仍然未知，等待下次回查");
                            return LocalTransactionState.UNKNOW;
                        }
                }
                
            } catch (Exception e) {
                System.err.println("事务回查异常: " + e.getMessage());
                e.printStackTrace();
                
                // 返回 UNKNOW，等待下次回查
                return LocalTransactionState.UNKNOW;
            }
        }
    }
}

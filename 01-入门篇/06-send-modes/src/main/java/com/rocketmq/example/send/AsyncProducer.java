package com.rocketmq.example.send;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 异步发送示例
 * 特点：通过回调获取发送结果，不阻塞主线程，吞吐量更高
 */
public class AsyncProducer {
    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("AsyncProducerGroup");
        producer.setNamesrvAddr("localhost:9876");
        producer.setRetryTimesWhenSendAsyncFailed(2);
        producer.start();

        int messageCount = 10;
        CountDownLatch latch = new CountDownLatch(messageCount);

        for (int i = 0; i < messageCount; i++) {
            final int index = i;
            Message msg = new Message("AsyncTopic", "TagA",
                    ("异步消息-" + i).getBytes(StandardCharsets.UTF_8));
            // 异步发送：通过回调处理结果
            producer.send(msg, new SendCallback() {
                @Override
                public void onSuccess(SendResult result) {
                    System.out.printf("消息 %d 发送成功: msgId=%s%n", index, result.getMsgId());
                    latch.countDown();
                }
                @Override
                public void onException(Throwable e) {
                    System.out.printf("消息 %d 发送失败: %s%n", index, e.getMessage());
                    latch.countDown();
                }
            });
        }
        // 等待所有回调完成
        latch.await(10, TimeUnit.SECONDS);
        producer.shutdown();
    }
}

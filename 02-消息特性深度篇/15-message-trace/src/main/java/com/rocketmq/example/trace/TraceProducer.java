package com.rocketmq.example.trace;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import java.nio.charset.StandardCharsets;

/**
 * 消息轨迹生产者示例
 * 通过 enableMsgTrace=true 开启消息轨迹追踪
 * 轨迹数据会发送到 RMQ_SYS_TRACE_TOPIC
 */
public class TraceProducer {
    public static void main(String[] args) throws Exception {
        // 第二个参数 true 表示开启消息轨迹
        DefaultMQProducer producer = new DefaultMQProducer("TraceProducerGroup", true);
        producer.setNamesrvAddr("localhost:9876");
        producer.start();

        for (int i = 0; i < 5; i++) {
            Message msg = new Message("TraceTopic", "TagA",
                    ("轨迹消息-" + i).getBytes(StandardCharsets.UTF_8));
            msg.setKeys("TRACE_KEY_" + i);
            SendResult result = producer.send(msg);
            System.out.printf("发送成功: msgId=%s（轨迹已记录）%n", result.getMsgId());
        }
        producer.shutdown();
    }
}

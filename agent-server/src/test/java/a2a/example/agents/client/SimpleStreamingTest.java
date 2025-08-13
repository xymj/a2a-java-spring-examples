package a2a.example.agents.client;

import io.a2a.A2A;
import io.a2a.client.A2AClient;
import io.a2a.spec.*;

import java.util.concurrent.CountDownLatch;

public class SimpleStreamingTest {

    public static void main(String[] args) throws A2AClientError, A2AServerException {
        // 获取AgentCard
        AgentCard agentCard = A2A.getAgentCard("http://localhost:8082/", "/multimodal/.well-known/agent-card.json", null);
        
        // 创建客户端
        A2AClient client = new A2AClient(agentCard);
        
        // 创建消息
        Message message = A2A.toUserMessage("你好");
        
        // 构建参数
        MessageSendParams params = new MessageSendParams.Builder()
            .message(message)
            .build();
            
        // 发送流式消息
        sendStreamingMessage(client, params);
    }

    private static void sendStreamingMessage(A2AClient client, MessageSendParams params) throws A2AServerException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        
        System.out.println("开始发送流式消息...");
        
        client.sendStreamingMessage(params, 
            // onNext 回调 - 接收每个流式响应
            response -> {
                System.out.println("收到流式响应: " + response);
            }, 
            // onError 回调 - 处理错误
            error -> {
                System.out.println("发生错误: " + error);
                countDownLatch.countDown();
            }, 
            // onComplete 回调 - 流完成时调用
            () -> {
                System.out.println("流式响应完成");
                countDownLatch.countDown();
            }
        );

        try {
            // 等待流完成
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
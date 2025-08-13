package a2a.example.agents.client;


import io.a2a.A2A;
import io.a2a.client.A2AClient;
import io.a2a.spec.*;

import java.util.concurrent.CountDownLatch;

public class NovelAgentClient {

    public static void main(String[] args) throws A2AClientError, A2AServerException {
        AgentCard agentCard = A2A.getAgentCard("http://localhost:8082/", "/novel/.well-known/agent-card.json", null);
        A2AClient client = new A2AClient(agentCard);
        Message message = A2A.toUserMessage("金庸武侠小说");

        MessageSendParams params = new MessageSendParams.Builder()
            .message(message)
            .build();

        sendMessage(client, params);

        //sendStreamingMessage(client, params);


    }

    private static void sendStreamingMessage(A2AClient client, MessageSendParams params) throws A2AServerException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        client.sendStreamingMessage(params, c -> {
            System.out.println("event: " + c.toString());
        }, e -> {
            System.out.println("error: " + e);
        }, () -> {
            System.out.println("failure: ");
        });

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendMessage(A2AClient client, MessageSendParams params) throws A2AServerException {
        SendMessageResponse response = client.sendMessage(params);

        System.out.println("Message sent with ID: " + response.getId());
        System.out.println("Message result: " + response.getResult());
        System.out.println("response: " + response);
    }
}

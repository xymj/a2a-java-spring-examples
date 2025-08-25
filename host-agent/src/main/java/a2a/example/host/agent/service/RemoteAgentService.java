package a2a.example.host.agent.service;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import a2a.example.host.agent.util.JsonUtil;
import io.a2a.A2A;
import io.a2a.client.A2AClient;
import io.a2a.spec.*;

@Service
public class RemoteAgentService {

  @Value("${novel.agent.url}")
  private String novelAgentUrl;

  @Value("${novel.agent.relative.card.path}")
  private String novelAgentRelativeCardPath;

  @Value("${flight.agent.url}")
  private String flightAgentUrl;

  @Value("${flight.agent.relative.card.path}")
  private String flightAgentRelativeCardPath;

  // private final Map<String, AgentCard> agentsCard = new HashMap<>();

  private final Map<String, String> agentsInfo = new HashMap<>();

  private final Map<String, A2AClient> agentsClient = new HashMap<>();

  @PostConstruct
  public void init() {
    System.out.println("init remote agent service");
    try {
      AgentCard novelAgentCard = A2A.getAgentCard(novelAgentUrl, novelAgentRelativeCardPath, null);
      // agentsCard.put(novelAgentCard.name(), novelAgentCard);
      agentsClient.put(novelAgentCard.name(), new A2AClient(novelAgentCard));
      agentsInfo.put(novelAgentCard.name(), getAgentDesc(novelAgentCard));

      AgentCard flightAgentCard =
          A2A.getAgentCard(flightAgentUrl, flightAgentRelativeCardPath, null);
      // agentsCard.put(flightAgentCard.name(), flightAgentCard);
      agentsClient.put(flightAgentCard.name(), new A2AClient(flightAgentCard));
      agentsInfo.put(flightAgentCard.name(), getAgentDesc(flightAgentCard));
    } catch (A2AClientError e) {
      System.out.println("get agent card error: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  public String getAgentsInfo() {
    String agentsInfo = String.join("\n", this.agentsInfo.values());
    System.out.println("agentsInfo: " + agentsInfo);
    return agentsInfo;
  }

  public String sendMessage(@ToolParam(description = "远端智能体名称")String agentName, @ToolParam(description = "用户咨询的问题")String userMessage) {
    System.out.println("sendMessage: " + agentName + " ---> " + userMessage);
    StringBuilder result = new StringBuilder();
    try {
      A2AClient client = agentsClient.get(agentName);
      if (client == null) {
        System.out.println("agent:" + agentName + " not found a2a client");
        throw new RuntimeException("agent:" + agentName + " not found a2a client");
      }
      Message message = A2A.toUserMessage(userMessage);
      System.out.println("message : " + JsonUtil.toJson(message));
      MessageSendParams params = new MessageSendParams.Builder().message(message).build();
      System.out.println("params : " + JsonUtil.toJson(params));
      CountDownLatch countDownLatch = new CountDownLatch(1);

      System.out.println("start sending streaming messages...");

      client.sendStreamingMessage(params,
          // onNext 回调 - 接收每个流式响应
          response -> {
            System.out.println("streaming response received: " + response);

            try {
              if (response instanceof Task task) {
                System.out.println("task kind: " + task.getKind());
                TaskStatus status = task.getStatus();
                String msg = status.message() == null ? "" : JsonUtil.toJson(status.message());
                System.out.println("task status: " + status.state() + ", " + msg);
                System.out.println("task history message: " + JsonUtil.toJson(task.getHistory()));
              }
              if (response instanceof TaskStatusUpdateEvent taskStatusUpdateEvent) {
                System.out.println("taskStatusUpdateEvent kind: " + taskStatusUpdateEvent.getKind());
                TaskStatus status = taskStatusUpdateEvent.getStatus();
                String msg = status.message() == null ? "" : JsonUtil.toJson(status.message());
                System.out.println("taskStatusUpdateEvent status: " + status.state() + ", " + msg);
              }
              if (response instanceof TaskArtifactUpdateEvent taskArtifactUpdateEvent) {
                Artifact artifact = taskArtifactUpdateEvent.getArtifact();
                for (Part<?> part : artifact.parts()) {
                  if (part instanceof TextPart textPart) {
                    System.out.println("textPart: " + textPart.getText());
                    result.append(textPart.getText());
                  }
                }
              }
            } catch (Exception e) {
              System.out.println("handle response error: " + e.getMessage());
            }
          },
          // onError 回调 - 处理错误
          error -> {
            System.out.println("An error occurred: " + error);
            if (countDownLatch.getCount() > 0) {
              countDownLatch.countDown();
            }
          },
          // onComplete 回调 - 流完成时调用
          () -> {
            System.out.println("Streaming response is completed");
            if (countDownLatch.getCount() > 0) {
              countDownLatch.countDown();
            }
          }
      );

      try {
        // 等待流完成
        countDownLatch.await();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    } catch (A2AServerException e) {
      throw new RuntimeException(e);
    }
    return result.isEmpty() ? "" : result.toString();
  }

  private String getAgentDesc(AgentCard agentCard) {
    HashMap<String, String> agentInfo = new HashMap<>();
    agentInfo.put("name", agentCard.name());
    agentInfo.put("description", agentCard.description());
    //agentInfo.put("skills", JsonUtil.toJson(agentCard.skills()));
    return JsonUtil.toJson(agentInfo);
  }
}

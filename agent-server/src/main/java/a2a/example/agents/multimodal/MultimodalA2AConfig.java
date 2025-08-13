package a2a.example.agents.multimodal;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import a2a.example.agents.constant.Constants;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.a2a.server.PublicAgentCard;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.events.QueueManager;
import io.a2a.server.requesthandlers.DefaultRequestHandler;
import io.a2a.server.requesthandlers.JSONRPCHandler;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.server.tasks.*;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;

@Configuration
public class MultimodalA2AConfig {

  @Resource(name = "multimodalExecutor")
  private AgentExecutor multimodalExecutor;

  @Bean (name = "multimodalAgentCard")
  @PublicAgentCard
  public AgentCard multimodalAgentCard() {
    AgentCapabilities capabilities = new AgentCapabilities.Builder()
        .streaming(true)
        .pushNotifications(true)
        .stateTransitionHistory(true)
        .build();
    AgentSkill skill = new AgentSkill.Builder()
        .id("image_analysis")
        .name("Image Analysis")
        .description("图片内容分析智能体，依据图片URL进行图片内容分析")
        .tags(List.of("图片内容", "图片详情", "图片URL"))
        .examples(List.of("请分析图片：https://dashscope.oss-cn-beijing.aliyuncs.com/images/dog_and_girl.jpeg 内容", "图片详情"))
        .build();
    return new AgentCard.Builder()
        .name("Image Analysis Agent")
        .description("图片内容分析智能体，依据图片URL进行图片内容分析")
        .url("http://localhost:8082/multimodal/")
        .version("1.0.0")
        .documentationUrl("http://example.com/docs")
        .capabilities(capabilities)
        .defaultInputModes(Constants.SUPPORTED_CONTENT_TYPES)
        .defaultOutputModes(Constants.SUPPORTED_CONTENT_TYPES)
        .skills(Collections.singletonList(skill))
        .protocolVersion("0.0.1").build();
  }

  @Bean(name = "multimodalRequestHandler")
  public RequestHandler multimodalRequestHandler() {
    TaskStore taskStore = new InMemoryTaskStore();
    QueueManager queueManager = new InMemoryQueueManager();
    PushNotificationConfigStore pushConfigStore = new InMemoryPushNotificationConfigStore();
    PushNotificationSender pushSender = new BasePushNotificationSender(pushConfigStore);
    Executor executor = Executors.newFixedThreadPool(10);
    return new DefaultRequestHandler(multimodalExecutor, taskStore, queueManager, pushConfigStore,
        pushSender, executor);
  }

  @Bean(name = "multimodalJsonRpcHandler")
  public JSONRPCHandler multimodalJsonRpcHandler() {
    return new JSONRPCHandler(multimodalAgentCard(), multimodalRequestHandler());
  }
}

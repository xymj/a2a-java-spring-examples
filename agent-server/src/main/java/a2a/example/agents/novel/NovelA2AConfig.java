package a2a.example.agents.novel;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import a2a.example.agents.constant.Constants;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

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
public class NovelA2AConfig {

  @Resource(name = "novelExecutor")
  private AgentExecutor novelExecutor;

  @Bean (name = "novelAgentCard")
  @PublicAgentCard
  public AgentCard novelAgentCard() {
    AgentCapabilities capabilities = new AgentCapabilities.Builder()
        .streaming(true)
        .pushNotifications(true)
        .stateTransitionHistory(true)
        .build();
    AgentSkill skill = new AgentSkill.Builder()
        .id("wuxiao_novel_recommend")
        .name("wuxiao novel recommend")
        .description("武侠小说推荐智能体")
        .tags(List.of("武侠小说", "武侠", "武侠小说推荐", "小说推荐"))
        .examples(List.of("武侠小说推荐", "小说推荐"))
        .build();
    return new AgentCard.Builder()
        .name("wuxiao novel recommend agent")
        .description("武侠小说推荐")
        .url("http://localhost:8082/novel/")
        .version("1.0.0")
        .documentationUrl("http://example.com/docs")
        .capabilities(capabilities)
        .defaultInputModes(Constants.SUPPORTED_CONTENT_TYPES)
        .defaultOutputModes(Constants.SUPPORTED_CONTENT_TYPES)
        .skills(Collections.singletonList(skill))
        .protocolVersion("0.0.1").build();
  }

  @Bean(name = "novelRequestHandler")
  public RequestHandler novelRequestHandler() {
    TaskStore taskStore = new InMemoryTaskStore();
    QueueManager queueManager = new InMemoryQueueManager();
    PushNotificationConfigStore pushConfigStore = new InMemoryPushNotificationConfigStore();
    PushNotificationSender pushSender = new BasePushNotificationSender(pushConfigStore);
    Executor executor = Executors.newFixedThreadPool(10);
    return new DefaultRequestHandler(novelExecutor, taskStore, queueManager, pushConfigStore,
        pushSender, executor);
  }

  @Bean(name = "novelJsonRpcHandler")
  public JSONRPCHandler novelJsonRpcHandler() {
    return new JSONRPCHandler(novelAgentCard(), novelRequestHandler());
  }
}

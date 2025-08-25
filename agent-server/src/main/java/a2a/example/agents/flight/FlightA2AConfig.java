package a2a.example.agents.flight;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import a2a.example.agents.constant.Constants;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
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
public class FlightA2AConfig {
  @Resource(name = "flightAgentExecutor")
  private AgentExecutor flightAgentExecutor;

  @Bean(name = "flightAgentCard")
  @PublicAgentCard
  public AgentCard flightAgentCard() {
    AgentCapabilities capabilities = new AgentCapabilities.Builder()
        .streaming(true)
        .pushNotifications(true)
        .stateTransitionHistory(true)
        .build();
    AgentSkill skill = new AgentSkill.Builder()
        .id("flight_booking")
        .name("Flight Booking")
        .description("支持已有机票的预订详情查询、机票日期改签、机票预订取消操作智能体")
        .tags(List.of("机票详情", "机票改签", "机票预订取消"))
        .examples(List.of("小明机票详情", "机票改签", "机票预订取消"))
        .build();
    return new AgentCard.Builder()
        .name("Flight Booking Agent")
        .description("支持已有机票的预订详情查询、机票日期改签、机票预订取消等操作")
        .url("http://localhost:8082/flight/")
        .version("1.0.0")
        .documentationUrl("http://example.com/docs")
        .capabilities(capabilities)
        .defaultInputModes(Constants.SUPPORTED_CONTENT_TYPES)
        .defaultOutputModes(Constants.SUPPORTED_CONTENT_TYPES)
        .skills(Collections.singletonList(skill))
        .protocolVersion("0.0.1").build();
  }

  @Bean(name = "flightRequestHandler")
  public RequestHandler flightRequestHandler() {
    TaskStore taskStore = new InMemoryTaskStore();
    QueueManager queueManager = new InMemoryQueueManager();
    PushNotificationConfigStore pushConfigStore = new InMemoryPushNotificationConfigStore();
    PushNotificationSender pushSender = new BasePushNotificationSender(pushConfigStore);
    Executor executor = Executors.newFixedThreadPool(10);
    return new DefaultRequestHandler(flightAgentExecutor, taskStore, queueManager, pushConfigStore,
        pushSender, executor);
  }

  @Bean(name = "flightJsonRpcHandler")
  public JSONRPCHandler flightJsonRpcHandler() {
    return new JSONRPCHandler(flightAgentCard(), flightRequestHandler());
  }


}

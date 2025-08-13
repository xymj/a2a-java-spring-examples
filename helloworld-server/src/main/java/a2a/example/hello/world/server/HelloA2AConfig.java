package a2a.example.hello.world.server;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
public class HelloA2AConfig {

  @Autowired
  private AgentExecutor agentExecutor;

  @Bean
  @PublicAgentCard
  public AgentCard agentCard() {
    return new AgentCard.Builder().name("Hello World Agent").description("Just a hello world agent")
        .url("http://localhost:8082/hello/world/").version("1.0.0").documentationUrl("http://example.com/docs")
        .capabilities(new AgentCapabilities.Builder().streaming(true).pushNotifications(true)
            .stateTransitionHistory(true).build())
        .defaultInputModes(Collections.singletonList("text"))
        .defaultOutputModes(Collections.singletonList("text"))
        .skills(Collections.singletonList(new AgentSkill.Builder().id("hello_world")
            .name("Returns hello world").description("just returns hello world")
            .tags(Collections.singletonList("hello world")).examples(List.of("hi", "hello world"))
            .build()))
        .protocolVersion("0.2.5").build();
  }

  @Bean
  @Primary
  public RequestHandler requestHandler() {
    TaskStore taskStore = new InMemoryTaskStore();
    QueueManager queueManager = new InMemoryQueueManager();
    PushNotificationConfigStore pushConfigStore = new InMemoryPushNotificationConfigStore();
    PushNotificationSender pushSender = new BasePushNotificationSender(pushConfigStore);
    Executor executor = Executors.newFixedThreadPool(10);
    return new DefaultRequestHandler(agentExecutor, taskStore, queueManager, pushConfigStore,
        pushSender, executor);
  }

  @Bean
  public JSONRPCHandler jsonRpcHandler() {
    return new JSONRPCHandler(agentCard(), requestHandler());
  }
}

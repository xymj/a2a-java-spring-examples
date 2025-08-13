package a2a.example.agents.flight;


import io.a2a.A2A;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.*;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component("flightAgentExecutor")
public class FlightAgentExecutor implements AgentExecutor {

  private final FlightAgent flightAgent;

  public FlightAgentExecutor(FlightAgent flightAgent) {
    this.flightAgent = flightAgent;
  }

  @Override
  public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
    String userInput = context.getUserInput(null);
    Task task = context.getTask();
    if (task == null) {
      Message message = context.getMessage();
      String taskId = message != null && message.getTaskId() != null ? message.getTaskId()
          : UUID.randomUUID().toString();
      String contextId = message != null && message.getContextId() != null ? message.getContextId()
          : UUID.randomUUID().toString();
      task = new Task.Builder().status(new TaskStatus(TaskState.SUBMITTED)).id(taskId)
          .contextId(contextId).history(message).build();
      eventQueue.enqueueEvent(task);
    }
    TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
    StringBuilder content = new StringBuilder();

    // 使用CompletableFuture确保execute方法等待流完成
    CompletableFuture<Void> completionFuture = new CompletableFuture<>();

    flightAgent.chat(context.getContextId(), userInput)
        .publishOn(Schedulers.boundedElastic())
        .doOnNext(c -> {
          content.append(c);
          System.out.println("问题处理中：" + c);
          taskUpdater.startWork(A2A.toAgentMessage("问题处理中：" + c));
        })
        .doOnComplete(() -> {
          taskUpdater.addArtifact(List.of(new TextPart(content.toString())), null, "result", null);
          taskUpdater.complete();
          completionFuture.complete(null); // 流完成后完成future
        })
        .doOnError(throwable -> {
          completionFuture.completeExceptionally(throwable); // 流出错时完成future
        })
        .subscribe(); // 使用subscribe而不是blockLast，这样doOnNext会逐步触发执行

    // 等待流完成后再退出execute方法
    try {
      completionFuture.join(); // 等待流处理完成
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {

  }
}
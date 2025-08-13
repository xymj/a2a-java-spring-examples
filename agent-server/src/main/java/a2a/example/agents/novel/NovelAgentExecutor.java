package a2a.example.agents.novel;


import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import io.a2a.A2A;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.*;

@Component("novelExecutor")
public class NovelAgentExecutor implements AgentExecutor {

  private final NovelAgent novelAgent;

  public NovelAgentExecutor(NovelAgent novelAgent) {
    this.novelAgent = novelAgent;
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
    novelAgent.chat(context.getContextId(), userInput).doOnNext(c -> {
      content.append(c);
      taskUpdater.startWork(A2A.toAgentMessage("问题处理中：" + c));
      // if (content.length() > 100) {
      // Message message = taskUpdater.newAgentMessage(List.of(new TextPart("请继续输如问题：")), null);
      // TaskStatusUpdateEvent event = new TaskStatusUpdateEvent.Builder()
      // .taskId(message.getTaskId())
      // .contextId(message.getContextId())
      // .isFinal(true)
      // .status(new TaskStatus(TaskState.INPUT_REQUIRED, message, null))
      // .build();
      // eventQueue.enqueueEvent(event);
      // }
    }).doOnComplete(() -> {
      taskUpdater.addArtifact(List.of(new TextPart(content.toString())), null, "result", null);
      taskUpdater.complete();
    }).blockLast();
  }

  @Override
  public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {

  }
}

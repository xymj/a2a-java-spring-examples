package a2a.example.host.agent;


import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.*;
import org.springframework.stereotype.Component;

import io.a2a.A2A;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;

import java.util.List;
import java.util.UUID;

@Component
public class HostAgentExecutor implements AgentExecutor {

  private final HostAgent hostAgent;

  public HostAgentExecutor(HostAgent hostAgent) {
    this.hostAgent = hostAgent;
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
    String result = hostAgent.chat(context.getContextId(), userInput);
    taskUpdater.addArtifact(List.of(new TextPart(result)), null, "result", null);
    taskUpdater.complete();
  }

  @Override
  public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
    throw new UnsupportedOperationError();
  }
}

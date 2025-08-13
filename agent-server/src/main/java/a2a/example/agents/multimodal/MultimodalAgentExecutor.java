package a2a.example.agents.multimodal;

import io.a2a.A2A;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CountDownLatch;

@Component("multimodalExecutor")
public class MultimodalAgentExecutor implements AgentExecutor {

    private final MultimodalAgent multimodalAgent;

    public MultimodalAgentExecutor(MultimodalAgent multimodalAgent) {
        this.multimodalAgent = multimodalAgent;
    }

    @Override
    public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        String userInput = context.getUserInput(null);
        Task task = context.getTask();
        if (task == null) {
            Message message = context.getMessage();
            task = new Task.Builder().status(new TaskStatus(TaskState.SUBMITTED)).id(message.getTaskId())
                .contextId(message.getContextId()).history(message).build();
            eventQueue.enqueueEvent(task);
        }
        TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
        StringBuilder content = new StringBuilder();

        // 使用Flux处理流式响应
        Flux<ChatResponse> chatResponseFlux = multimodalAgent.streamImage(context.getContextId(), userInput);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        // 订阅流并逐个处理响应
        chatResponseFlux.subscribe(
            // onNext - 处理每个ChatResponse
            chatResponse -> {
                String outputContent = chatResponse.getResult().getOutput().getText();
                System.out.println("|||------>>>>: " + outputContent);
                taskUpdater.startWork(A2A.toAgentMessage("问题处理中：" + outputContent));
                content.append(outputContent);
            },
            // onError - 处理错误
            throwable -> {
                System.err.println("流式处理出错: " + throwable.getMessage());
                taskUpdater.complete();
            },
            // onComplete - 流完成时的处理
            () -> {
                String result = content.toString();
                System.out.println("|||------>>>> result: " + result);
                taskUpdater.addArtifact(List.of(new TextPart(result)), null, "result", null);
                taskUpdater.complete();
                countDownLatch.countDown();
            }
        );

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {

    }
}
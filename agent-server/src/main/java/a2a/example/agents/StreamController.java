package a2a.example.agents;

import io.a2a.server.events.EventQueueClosedException;
import io.a2a.server.util.async.AsyncUtils;
import io.a2a.spec.Event;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskStatusUpdateEvent;
import mutiny.zero.BackpressureStrategy;
import mutiny.zero.TubeConfiguration;
import mutiny.zero.ZeroPublisher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@RestController
@RequestMapping("/stream")
public class StreamController {

    @GetMapping(value = "/data", produces = "text/event-stream")
    public Flow.Publisher<String> streamData() {
        MyDataPublisher publisher = new MyDataPublisher();
        publisher.sendData(); // 启动数据发送
        return publisher;
    }

    @GetMapping(value = "/data2", produces = "text/event-stream")
    public Flow.Publisher<String> streamData2() {
        TubeConfiguration conf = new TubeConfiguration()
            .withBackpressureStrategy(BackpressureStrategy.BUFFER)
            .withBufferSize(256);

        return ZeroPublisher.create(conf, tube -> {
            new Thread(() -> {
                try {
                    for (int i = 1; i <= 10; i++) {
                        String item = "Item-" + i;
                        tube.send(item);
                        Thread.sleep(1000); // 模拟延迟
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    tube.complete();
                }
            }).start();
//            for (int i = 1; i <= 10; i++) {
//                String item = "Item-" + i;
//                tube.send(item);
//                try {
//                    Thread.sleep(1000); // 模拟延迟
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//            tube.complete();
        });
    }


    @GetMapping(value = "/data3", produces = "text/event-stream")
    public Flow.Publisher<String> streamData3() {
        BlockingQueue<String> queue = new LinkedBlockingDeque<>(1000);

        CompletableFuture.runAsync(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    String item = "Item-" + i;
                    queue.add(item);
                    System.out.println("Produced: " + item);
                    Thread.sleep(1000); // 模拟延迟
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });


        TubeConfiguration conf = new TubeConfiguration()
            .withBackpressureStrategy(BackpressureStrategy.BUFFER)
            .withBufferSize(256);

        Flow.Publisher<String> objectPublisher = ZeroPublisher.create(conf, tube -> {
            new Thread(() -> {
                int count = 0;
                while (true) {
                    try {
                        String item = queue.poll(500, TimeUnit.MILLISECONDS);
                        if (item == null) {
                            continue;
                        }
                        tube.send(item);
                        ++count;
                        System.out.println("Consumed: " + item);
                        if (count >= 10) {
                            tube.complete();
                            break;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        tube.fail(e);
                        break;
                    }
                }
            }).start();
        });


        Flow.Publisher<String> results = AsyncUtils.processor(conf, objectPublisher, ((errorConsumer, event) -> {
            System.out.println("Received callTaskManagerProcess: " + event);
            return true;
        }));

        Flow.Publisher<String> eventPublisher = AsyncUtils.processor(conf, results, ((errorConsumer, event) -> {
            System.out.println("Received pushConfigStore: " + event);
            return true;
        }));

        return AsyncUtils.convertingProcessor(eventPublisher, str -> "--->data: " + str);
    }




    // 推荐使用 SubmissionPublisher 作为 Flow.Publisher 的实现
    public class MyDataPublisher extends SubmissionPublisher<String> {

        public void sendData() {
            new Thread(() -> {
                try {
                    for (int i = 1; i <= 10; i++) {
                        String item = "Item-" + i;
                        this.submit(item); // 非阻塞提交
                        Thread.sleep(1000); // 模拟延迟
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    this.close();
                }
            }).start();
        }
    }

}
package org.example;

import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SqsConsumer {
    private final List<String> receivedMessages = new CopyOnWriteArrayList<>();

    @SqsListener(
            id = "testQueueListener",
            value = "test-queue",
            maxConcurrentMessages = "20",
            maxMessagesPerPoll = "10",
            pollTimeoutSeconds = "2"
    )
    public void receiveMessages(List<String> messages) throws InterruptedException {
        System.out.println(Thread.currentThread().getName() + " Received batch: " + messages);
        receivedMessages.addAll(messages);
        Thread.sleep(200);
        System.out.println(Thread.currentThread().getName() + " Consumed batch: " + messages);
    }

    public List<String> getReceivedMessages() {
        return receivedMessages;
    }
}

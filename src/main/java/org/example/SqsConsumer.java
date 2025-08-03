package org.example;

import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SqsConsumer {
    private static final Logger logger = LoggerFactory.getLogger(SqsConsumer.class);

    public static final String TEST_QUEUE_NAME = "test-queue";
    public static final String TEST_QUEUE_LISTENER = "testQueueListener";
    private final List<String> receivedMessages = new CopyOnWriteArrayList<>();

    @SqsListener(
            id = TEST_QUEUE_LISTENER,
            value = TEST_QUEUE_NAME,
            maxConcurrentMessages = "10", // default is 10
            maxMessagesPerPoll = "10", // default is 10, should be less or equal than the above
            pollTimeoutSeconds = "2" // default is 10 seconds
    )
    public void receiveMessages(List<String> messages) throws InterruptedException {
        logger.info("Received batch: {}", messages);
        Thread.sleep(1000);
        receivedMessages.addAll(messages);
        logger.info("Consumed batch: {}", messages);
    }

    public List<String> getReceivedMessages() {
        return receivedMessages;
    }
}

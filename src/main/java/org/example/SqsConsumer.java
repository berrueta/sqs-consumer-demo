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
    private static final long DELAY_PER_MESSAGE_MS = 100;

    private final List<String> receivedMessages = new CopyOnWriteArrayList<>();

    // see https://docs.awspring.io/spring-cloud-aws/docs/3.3.0/reference/html/index.html#sqs-integration for details about the parameters
    @SqsListener(
            id = TEST_QUEUE_LISTENER,
            value = TEST_QUEUE_NAME,
            maxConcurrentMessages = "10", // default is 10
            maxMessagesPerPoll = "10", // default is 10, should be less or equal than the above
            pollTimeoutSeconds = "10" // default is 10 seconds
    )
//    public void receiveMessages(String message) throws InterruptedException {
//        var messages = List.of(message);
    public void receiveMessages(List<String> messages) throws InterruptedException {
        logger.info("Received batch of size {}: {}", messages.size(), messages);
        for (@SuppressWarnings("unused") String individualMessage : messages) {
            Thread.sleep(DELAY_PER_MESSAGE_MS);
        }
        receivedMessages.addAll(messages);
        logger.info("Consumed batch: {}", messages);
    }

    public List<String> getReceivedMessages() {
        return receivedMessages;
    }
}

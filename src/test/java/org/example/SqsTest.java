package org.example;

import io.awspring.cloud.sqs.listener.MessageListenerContainer;
import io.awspring.cloud.sqs.listener.MessageListenerContainerRegistry;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.StopWatch;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;

import static java.util.Objects.requireNonNull;
import static org.awaitility.Awaitility.await;
import static org.example.SqsConsumer.TEST_QUEUE_LISTENER;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
public class SqsTest {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(SqsTest.class);

    private static final int TOTAL_MESSAGES = 100;

    @SuppressWarnings("resource")
    @Container
    public static LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.4.0"))
            .withServices(LocalStackContainer.Service.SQS);

    @Autowired
    private SqsConsumer sqsConsumer;

    @Autowired
    private MessageListenerContainerRegistry messageListenerContainerRegistry;

    @Autowired
    private SqsAsyncClient sqsClient;

    @Autowired
    private SqsTemplate sqsTemplate;

    private final StopWatch stopWatch = new StopWatch();

    @DynamicPropertySource
    static void configureAwsProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.sqs.endpoint", () -> localstack.getEndpointOverride(LocalStackContainer.Service.SQS).toString());
        registry.add("spring.cloud.aws.region.static", localstack::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", localstack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localstack::getSecretKey);
        registry.add("cloud.aws.sqs.autoStart", () -> "false"); // Disable auto-start of SQS listener
    }

    @BeforeEach
    void createQueue() throws ExecutionException, InterruptedException {
        sqsClient.createQueue(CreateQueueRequest.builder().queueName(SqsConsumer.TEST_QUEUE_NAME).build()).get();
    }

    @Test
    void testSqsSendAndReceive() {
        addMessagesToQueue();

        stopWatch.start();
        getQueueListener().start();

        await()
                .atMost(5, TimeUnit.MINUTES)
                .until(() -> sqsConsumer.getReceivedMessages().size() == TOTAL_MESSAGES);
        stopWatch.stop();

        logger.info("All messages received in {} seconds", stopWatch.getTotalTimeSeconds());

        getQueueListener().stop();
    }

    private void addMessagesToQueue() {
        int batchSize = 10;
        for (int i = 1; i <= TOTAL_MESSAGES; i += batchSize) {
            var batch = new ArrayList<Message<String>>();
            for (int j = 0; j < batchSize && (i + j) <= TOTAL_MESSAGES; j++) {
                int msgNum = i + j;
                batch.add(new GenericMessage<>("message " + msgNum));
            }
            sqsTemplate.sendMany(SqsConsumer.TEST_QUEUE_NAME, batch);
        }

        logger.info("All {} messages sent to the queue in batches of {}", TOTAL_MESSAGES, batchSize);
    }

    private MessageListenerContainer<?> getQueueListener() {
        return requireNonNull(messageListenerContainerRegistry.getContainerById(TEST_QUEUE_LISTENER));
    }
}

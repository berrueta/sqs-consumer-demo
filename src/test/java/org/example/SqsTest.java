package org.example;

import io.awspring.cloud.sqs.listener.MessageListenerContainer;
import io.awspring.cloud.sqs.listener.MessageListenerContainerRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.StopWatch;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;

import static java.util.Objects.requireNonNull;
import static org.awaitility.Awaitility.await;
import static org.example.SqsConsumer.TEST_QUEUE_LISTENER;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ExtendWith(SpringExtension.class)
public class SqsTest {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(SqsTest.class);

    private static final int TOTAL_MESSAGES = 100;

    @Container
    public static LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.4.0"))
            .withServices(LocalStackContainer.Service.SQS);

    @Autowired
    private SqsConsumer sqsConsumer;

    @Autowired
    private MessageListenerContainerRegistry messageListenerContainerRegistry;

    @DynamicPropertySource
    static void configureAwsProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.sqs.endpoint", () -> localstack.getEndpointOverride(LocalStackContainer.Service.SQS).toString());
        registry.add("spring.cloud.aws.region.static", localstack::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", localstack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localstack::getSecretKey);
        registry.add("cloud.aws.sqs.autoStart", () -> "false"); // Disable auto-start of SQS listener
    }

    @Test
    void testSqsSendAndReceive() {
        SqsClient sqsClient = SqsClient.builder()
                .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.SQS))
                .region(Region.of(localstack.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                ))
                .build();

        String queueUrl = sqsClient.createQueue(CreateQueueRequest.builder().queueName(SqsConsumer.TEST_QUEUE_NAME).build()).queueUrl();

        addMessagesToQueue(sqsClient, queueUrl);

        await().atMost(Duration.ofSeconds(15))
                .until(() -> sqsConsumer.getReceivedMessages().contains("message 200"));
    }

    private void addMessagesToQueue(SqsClient sqsClient, String queueUrl) {
        int batchSize = 10;
        for (int i = 1; i <= TOTAL_MESSAGES; i += batchSize) {
            var batch = new ArrayList<SendMessageBatchRequestEntry>();
            for (int j = 0; j < batchSize && (i + j) <= TOTAL_MESSAGES; j++) {
                int msgNum = i + j;
                batch.add(SendMessageBatchRequestEntry.builder()
                        .id("msg-" + msgNum)
                        .messageBody("message " + msgNum)
                        .build());
            }
            sqsClient.sendMessageBatch(SendMessageBatchRequest.builder()
                    .queueUrl(queueUrl)
                    .entries(batch)
                    .build());
        }

        logger.info("All {} messages sent to the queue in batches of {}", TOTAL_MESSAGES, batchSize);

        getQueueListener().start();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        await()
                .atMost(5, TimeUnit.MINUTES)
                .until(() -> sqsConsumer.getReceivedMessages().size() == TOTAL_MESSAGES);
        stopWatch.stop();

        logger.info("All messages received in {} seconds", stopWatch.getTotalTimeSeconds());

        getQueueListener().stop();
    }

    private MessageListenerContainer<?> getQueueListener() {
        return requireNonNull(messageListenerContainerRegistry.getContainerById(TEST_QUEUE_LISTENER));
    }
}

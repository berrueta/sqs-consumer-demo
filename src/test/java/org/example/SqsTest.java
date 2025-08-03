package org.example;

import io.awspring.cloud.sqs.listener.MessageListenerContainer;
import io.awspring.cloud.sqs.listener.MessageListenerContainerRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ExtendWith(SpringExtension.class)
public class SqsTest {
    private static final int TOTAL_MESSAGES = 200;

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
    }

    @Test
    void testSqsSendAndReceive() throws Exception {
        SqsClient sqsClient = SqsClient.builder()
                .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.SQS))
                .region(Region.of(localstack.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                ))
                .build();

        String queueName = "test-queue";
        String queueUrl = sqsClient.createQueue(CreateQueueRequest.builder().queueName(queueName).build()).queueUrl();

        addMessagesToQueue(sqsClient, queueUrl);

        await().atMost(Duration.ofSeconds(15))
                .until(() -> sqsConsumer.getReceivedMessages().contains("message 200"));
    }

    private void addMessagesToQueue(SqsClient sqsClient, String queueUrl) {
        getQueueListener("testQueueListener").stop();

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
        System.out.println("All messages sent to the queue.");
        getQueueListener("testQueueListener").start();

        await()
                .atMost(5, TimeUnit.MINUTES)
                .until(() -> sqsConsumer.getReceivedMessages().size() == TOTAL_MESSAGES);
    }

    private MessageListenerContainer<?> getQueueListener(String id) {
        return requireNonNull(messageListenerContainerRegistry.getContainerById(id));
    }
}

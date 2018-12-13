package edu.esipe.i3.ezipflix.videodispatcher.service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.rpc.ApiException;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import edu.esipe.i3.ezipflix.videodispatcher.definition.VideoConversion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ConversionServiceImpl implements ConversionService {

    @Value("${gcloud.pubsub.project}") String projectId;
    @Value("${gcloud.pubsub.topic}") String topic;

    @Value("${aws.dynamodb.table}") String tableName;

    @Value("${aws.s3.name}") String bucketName;

    private String result;

    @Override
    public String publish(VideoConversion video) throws Exception {
        ProjectTopicName topicName = ProjectTopicName.of(projectId, topic);
        Publisher publisher = null;

        try {

            publisher = Publisher.newBuilder(topicName).build();

            ObjectMapper mapper = new ObjectMapper();
            String message = mapper.writeValueAsString(video);

            ByteString data = ByteString.copyFromUtf8(message);
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

            ApiFuture<String> future = publisher.publish(pubsubMessage);

            ApiFutures.addCallback(
                    future,
                    new ApiFutureCallback<String>() {

                        @Override
                        public void onFailure(Throwable throwable) {
                            if (throwable instanceof ApiException) {
                                ApiException apiException = ((ApiException) throwable);
                                log.info("Code = {}", apiException.getStatusCode().getCode());
                                log.info("Retryable = {}", apiException.isRetryable());
                                throw apiException;
                            }
                            log.error("Error publishing message = {}", message);
                        }

                        @Override
                        public void onSuccess(String messageId) {
                            log.info("Message ID = {}", messageId);
                            result = messageId;
                        }
                    },
                    MoreExecutors.directExecutor());
        } finally {
            if (publisher != null) {
                publisher.shutdown();
                publisher.awaitTermination(1, TimeUnit.MINUTES);
            }
        }
        return result;
    }

    @Override
    public String save(VideoConversion video) {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.EU_WEST_3)
                .build();
        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable(tableName);
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        df.setTimeZone(tz);
        Item item = new Item()
                .withPrimaryKey("uuid", video.getUuid().toString())
                .withString("conversion_date", df.format(video.getConversionDate()))
                .withString("origin_path", video.getOriginPath().toString())
                .withString("target_path", ".");

        PutItemOutcome outcome = table.putItem(item);
        log.info(outcome.toString());
        return outcome.toString();
    }

    @Override
    public boolean checkOriginFileExists(String objectName) {
        AmazonS3 client = AmazonS3Client
                .builder()
                .withRegion(Regions.EU_WEST_3)
                .build();
        return client.doesObjectExist(bucketName, objectName);
    }

}

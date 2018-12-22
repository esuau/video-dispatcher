package edu.esipe.i3.ezipflix.videodispatcher.service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import edu.esipe.i3.ezipflix.videodispatcher.definition.VideoConversion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.UUID;

@Slf4j
@Service
public class DatabaseServiceImpl implements DatabaseService {

    @Value("${aws.dynamodb.table}")
    String tableName;

    @Override
    public PutItemResult save(VideoConversion video) {
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
                .withString("target_path", video.getTargetPath().toString());

        PutItemOutcome outcome = table.putItem(item);
        log.info("DB outcome = {}", outcome.getPutItemResult().getSdkHttpMetadata().toString());
        return outcome.getPutItemResult();
    }

    @Override
    public DeleteItemResult remove(UUID uuid) {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.EU_WEST_3)
                .build();
        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable(tableName);

        DeleteItemSpec deleteItemSpec = new DeleteItemSpec()
                .withPrimaryKey(new PrimaryKey("uuid", uuid.toString()));

        try {
            log.info("Attempting delete...");
            DeleteItemOutcome outcome = table.deleteItem(deleteItemSpec);
            log.info("Successfully deleted item {}.", uuid);
            return outcome.getDeleteItemResult();
        } catch (Exception e) {
            log.error("Unable to delete item.", e);
        }
        return null;
    }

}

package edu.esipe.i3.ezipflix.videodispatcher.service;

import com.amazonaws.services.dynamodbv2.model.DeleteItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import edu.esipe.i3.ezipflix.videodispatcher.definition.VideoConversion;

import java.util.UUID;

public interface ConversionService {

    String publish(VideoConversion video) throws Exception;

    PutItemResult save(VideoConversion video);

    DeleteItemResult remove(UUID uuid);

    boolean checkFileExists(String objectName);

}

package edu.esipe.i3.ezipflix.videodispatcher.service;

import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import edu.esipe.i3.ezipflix.videodispatcher.definition.VideoConversion;

public interface ConversionService {

    String publish(VideoConversion video) throws Exception;

    PutItemResult save(VideoConversion video);

    boolean checkFileExists(String objectName);

}

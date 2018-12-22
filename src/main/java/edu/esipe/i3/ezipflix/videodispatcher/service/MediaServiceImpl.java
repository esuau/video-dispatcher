package edu.esipe.i3.ezipflix.videodispatcher.service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MediaServiceImpl implements MediaService {

    @Value("${aws.s3.name}")
    String bucketName;

    @Override
    public boolean checkFileExists(String objectName) {
        AmazonS3 client = AmazonS3Client
                .builder()
                .withRegion(Regions.EU_WEST_3)
                .build();
        return client.doesObjectExist(bucketName, objectName);
    }

}

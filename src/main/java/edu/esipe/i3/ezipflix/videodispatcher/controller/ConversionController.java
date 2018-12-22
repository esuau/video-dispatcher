package edu.esipe.i3.ezipflix.videodispatcher.controller;

import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import edu.esipe.i3.ezipflix.videodispatcher.definition.ConversionRequest;
import edu.esipe.i3.ezipflix.videodispatcher.definition.ConversionResponse;
import edu.esipe.i3.ezipflix.videodispatcher.definition.VideoConversion;
import edu.esipe.i3.ezipflix.videodispatcher.definition.exception.AlreadyExistsException;
import edu.esipe.i3.ezipflix.videodispatcher.definition.exception.BadRequestException;
import edu.esipe.i3.ezipflix.videodispatcher.definition.exception.NotFoundException;
import edu.esipe.i3.ezipflix.videodispatcher.definition.exception.ServiceException;
import edu.esipe.i3.ezipflix.videodispatcher.service.DatabaseService;
import edu.esipe.i3.ezipflix.videodispatcher.service.MediaService;
import edu.esipe.i3.ezipflix.videodispatcher.service.MessagingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

@Slf4j
@RestController
public class ConversionController {

    private final DatabaseService databaseService;
    private final MediaService mediaService;
    private final MessagingService messagingService;

    @Autowired
    public ConversionController(DatabaseService databaseService, MediaService mediaService, MessagingService messagingService) {
        this.databaseService = databaseService;
        this.mediaService = mediaService;
        this.messagingService = messagingService;
    }

    @PostMapping(value = "/convert")
    public ConversionResponse convert(@RequestBody ConversionRequest request) throws Exception {
        String originPathStr;
        String targetPathStr;

        if (null != request.getOriginPath()) {
            originPathStr = request.getOriginPath().toString();
        } else {
            throw new BadRequestException("Missing parameter: field \"originPath\" is required.");
        }

        if (null != request.getTargetPath()) {
            targetPathStr = request.getTargetPath().toString();
            if (!targetPathStr.substring(targetPathStr.lastIndexOf('.') + 1).equals("avi")) {
                throw new BadRequestException("Extension of media file is invalid: " + targetPathStr);
            }
        } else {
            targetPathStr = this.getConvertedFileName(originPathStr);
        }

        log.info("originPath = {}", originPathStr);
        log.info("targetPath = {}", targetPathStr);

        if (!this.mediaService.checkFileExists(originPathStr)) {
            throw new NotFoundException("Object \"" + originPathStr + "\" does not exist.");
        }

        if (this.mediaService.checkFileExists(targetPathStr)) {
            throw new AlreadyExistsException("Object \"" + originPathStr + "\" has already been converted.");
        }

        VideoConversion videoConversion = new VideoConversion(
                UUID.randomUUID(),
                new Date(),
                request.getOriginPath(),
                new URI(targetPathStr));

        PutItemResult dbResult = this.databaseService.save(videoConversion);
        if (dbResult == null
                || dbResult.getSdkHttpMetadata() == null
                || dbResult.getSdkHttpMetadata().getHttpStatusCode() != 200) {
            throw new ServiceException("Error when saving conversion info to database." );
        }

        String messageId = this.messagingService.publish(videoConversion);

        log.info("Successfully sent conversion request.");
        return new ConversionResponse(videoConversion.getUuid(), messageId, dbResult, videoConversion.getConversionDate());
    }

    private String getConvertedFileName(String fileName) {
        int i = fileName.lastIndexOf('.');
        String name = fileName.substring(0, i);
        return name + ".avi";
    }

}

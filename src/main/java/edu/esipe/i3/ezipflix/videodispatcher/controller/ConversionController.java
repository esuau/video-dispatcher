package edu.esipe.i3.ezipflix.videodispatcher.controller;

import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import edu.esipe.i3.ezipflix.videodispatcher.definition.ConversionRequest;
import edu.esipe.i3.ezipflix.videodispatcher.definition.ConversionResponse;
import edu.esipe.i3.ezipflix.videodispatcher.definition.VideoConversion;
import edu.esipe.i3.ezipflix.videodispatcher.definition.exception.AlreadyExistsException;
import edu.esipe.i3.ezipflix.videodispatcher.definition.exception.BadRequestException;
import edu.esipe.i3.ezipflix.videodispatcher.definition.exception.NotFoundException;
import edu.esipe.i3.ezipflix.videodispatcher.service.ConversionService;
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

    private final ConversionService conversionService;

    @Autowired
    public ConversionController(ConversionService conversionService) {
        this.conversionService = conversionService;
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

        if (!this.conversionService.checkFileExists(originPathStr)) {
            throw new NotFoundException("Object \"" + originPathStr + "\" does not exist.");
        }

        if (this.conversionService.checkFileExists(targetPathStr)) {
            throw new AlreadyExistsException("Object \"" + originPathStr + "\" has already been converted.");
        }

        VideoConversion videoConversion = new VideoConversion(
                UUID.randomUUID(),
                new Date(),
                request.getOriginPath(),
                new URI(targetPathStr));
        PutItemResult dbResult = this.conversionService.save(videoConversion);
        String messageId = this.conversionService.publish(videoConversion);

        log.info("Successfully sent conversion request.");
        return new ConversionResponse(videoConversion.getUuid(), messageId, dbResult, videoConversion.getConversionDate());
    }

    private String getConvertedFileName(String fileName) {
        int i = fileName.lastIndexOf('.');
        String name = fileName.substring(0, i);
        return name + ".avi";
    }

}

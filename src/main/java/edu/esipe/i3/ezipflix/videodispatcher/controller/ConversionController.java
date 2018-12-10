package edu.esipe.i3.ezipflix.videodispatcher.controller;

import edu.esipe.i3.ezipflix.videodispatcher.definition.ConversionRequest;
import edu.esipe.i3.ezipflix.videodispatcher.definition.ConversionResponse;
import edu.esipe.i3.ezipflix.videodispatcher.definition.VideoConversion;
import edu.esipe.i3.ezipflix.videodispatcher.service.ConversionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
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
        log.info("URI = {}", request.getPath().toString());

        VideoConversion videoConversion = new VideoConversion(UUID.randomUUID(), request.getPath(), new URI(""));
        String messageId = this.conversionService.publish(videoConversion);

        return new ConversionResponse(videoConversion.getUuid(), messageId);
    }

}

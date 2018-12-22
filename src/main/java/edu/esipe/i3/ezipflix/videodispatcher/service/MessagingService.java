package edu.esipe.i3.ezipflix.videodispatcher.service;

import edu.esipe.i3.ezipflix.videodispatcher.definition.VideoConversion;

public interface MessagingService {

    String publish(VideoConversion video) throws Exception;

}

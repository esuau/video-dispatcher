package edu.esipe.i3.ezipflix.videodispatcher.definition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoConversion {
    private UUID uuid;
    private URI originPath;
    private URI targetPath;
}

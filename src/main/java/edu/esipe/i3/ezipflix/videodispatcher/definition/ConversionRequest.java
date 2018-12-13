package edu.esipe.i3.ezipflix.videodispatcher.definition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URI;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversionRequest {
    private URI originPath;
    private URI targetPath;
}

package edu.esipe.i3.ezipflix.videodispatcher.definition;

import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversionResponse {
    private UUID uuid;
    private String messageId;
    private PutItemResult dbResult;
    private Date date;
}

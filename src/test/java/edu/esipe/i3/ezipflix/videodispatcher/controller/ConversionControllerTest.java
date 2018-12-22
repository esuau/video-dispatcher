package edu.esipe.i3.ezipflix.videodispatcher.controller;

import com.amazonaws.http.HttpResponse;
import com.amazonaws.http.SdkHttpMetadata;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.esipe.i3.ezipflix.videodispatcher.definition.ConversionRequest;
import edu.esipe.i3.ezipflix.videodispatcher.definition.exception.AlreadyExistsException;
import edu.esipe.i3.ezipflix.videodispatcher.definition.exception.BadRequestException;
import edu.esipe.i3.ezipflix.videodispatcher.definition.exception.NotFoundException;
import edu.esipe.i3.ezipflix.videodispatcher.definition.exception.ServiceException;
import edu.esipe.i3.ezipflix.videodispatcher.service.DatabaseService;
import edu.esipe.i3.ezipflix.videodispatcher.service.MediaService;
import edu.esipe.i3.ezipflix.videodispatcher.service.MessagingService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URI;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(MockitoJUnitRunner.class)
public class ConversionControllerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private DatabaseService databaseService;

    @Mock
    private MediaService mediaService;

    @Mock
    private MessagingService messagingService;

    @InjectMocks
    private ConversionController conversionController;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(conversionController).build();
    }

    @Test
    public void convertReturnsAConversionResponse() throws Exception {
        PutItemResult result = new PutItemResult();
        AttributeValue attributes = new AttributeValue("testAttribute");
        HttpResponse response = new HttpResponse(null, null);
        response.setStatusCode(200);
        result.addAttributesEntry("attribute", attributes);
        result.setSdkHttpMetadata(SdkHttpMetadata.from(response));

        when(messagingService.publish(any())).thenReturn("fakeMessageId");
        when(databaseService.save(any())).thenReturn(result);
        when(mediaService.checkFileExists("fake.mkv")).thenReturn(true);
        when(mediaService.checkFileExists("fake.avi")).thenReturn(false);

        ConversionRequest request = new ConversionRequest(new URI("fake.mkv"), null);
        mockMvc.perform(post("/convert")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$", Matchers.notNullValue()))
                .andExpect(jsonPath("$.uuid", Matchers.notNullValue()))
                .andExpect(jsonPath("$.messageId", Matchers.is("fakeMessageId")))
                .andExpect(jsonPath("$.dbResult", Matchers.notNullValue()))
                .andExpect(jsonPath("$.dbResult.attributes.attribute.s", Matchers.is("testAttribute")))
                .andExpect(jsonPath("$.date", Matchers.notNullValue()))
                .andExpect(jsonPath("$.date").value(Matchers.lessThanOrEqualTo(new Date().getTime())));
    }

    @Test
    public void convertThrowsNotFoundException() throws Exception {
        thrown.expect(NotFoundException.class);
        thrown.expectMessage(Matchers.containsString("Object \"testObject.mkv\" does not exist."));

        when(mediaService.checkFileExists("testObject.mkv")).thenReturn(false);

        ConversionRequest request = new ConversionRequest(new URI("testObject.mkv"), null);
        conversionController.convert(request);
    }

    @Test
    public void convertThrowsAlreadyExistsException() throws Exception {
        thrown.expect(AlreadyExistsException.class);
        thrown.expectMessage(Matchers.containsString("Object \"testObject.mkv\" has already been converted."));

        when(mediaService.checkFileExists(anyString())).thenReturn(true);

        ConversionRequest request = new ConversionRequest(new URI("testObject.mkv"), null);
        conversionController.convert(request);
    }

    @Test
    public void convertThrowsAlreadyExistsExceptionWithTargetPath() throws Exception {
        thrown.expect(AlreadyExistsException.class);
        thrown.expectMessage(Matchers.containsString("Object \"testObject.mkv\" has already been converted."));

        when(mediaService.checkFileExists(anyString())).thenReturn(true);

        ConversionRequest request = new ConversionRequest(new URI("testObject.mkv"), new URI("targetPath.avi"));
        conversionController.convert(request);
    }

    @Test
    public void convertThrowsBadRequestExceptionWithNullOriginPath() throws Exception {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage(Matchers.containsString("Missing parameter: field \"originPath\" is required."));

        ConversionRequest request = new ConversionRequest(null, null);
        conversionController.convert(request);
    }

    @Test
    public void convertThrowsBadRequestExceptionWithInvalidTargetPath() throws Exception {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage(Matchers.containsString("Extension of media file is invalid: targetPath"));

        ConversionRequest request = new ConversionRequest(new URI("testObject.mkv"), new URI("targetPath"));
        conversionController.convert(request);
    }

    @Test
    public void convertThrowsServiceException() throws Exception {
        thrown.expect(ServiceException.class);
        thrown.expectMessage(Matchers.containsString("Error when saving conversion info to database."));

        PutItemResult result = new PutItemResult();
        HttpResponse response = new HttpResponse(null, null);
        response.setStatusCode(500);
        result.setSdkHttpMetadata(SdkHttpMetadata.from(response));

        when(databaseService.save(any())).thenReturn(result);
        when(mediaService.checkFileExists("testObject.mkv")).thenReturn(true);
        when(mediaService.checkFileExists("targetPath.avi")).thenReturn(false);

        ConversionRequest request = new ConversionRequest(new URI("testObject.mkv"), new URI("targetPath.avi"));
        conversionController.convert(request);
    }

    private static String asJsonString(final Object obj) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
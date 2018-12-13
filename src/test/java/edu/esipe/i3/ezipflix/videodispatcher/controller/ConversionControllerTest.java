package edu.esipe.i3.ezipflix.videodispatcher.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.esipe.i3.ezipflix.videodispatcher.definition.ConversionRequest;
import edu.esipe.i3.ezipflix.videodispatcher.definition.exception.NotFoundException;
import edu.esipe.i3.ezipflix.videodispatcher.service.ConversionService;
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
    private ConversionService conversionService;

    @InjectMocks
    private ConversionController conversionController;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(conversionController).build();
    }

    @Test
    public void convertReturnsAConversionResponse() throws Exception {
        when(conversionService.publish(any())).thenReturn("fakeMessageId");
        when(conversionService.save(any())).thenReturn("fakeOutcome");
        when(conversionService.checkOriginFileExists(anyString())).thenReturn(true);

        ConversionRequest request = new ConversionRequest(new URI("fakePath"));
        mockMvc.perform(post("/convert")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$", Matchers.notNullValue()))
                .andExpect(jsonPath("$.uuid", Matchers.notNullValue()))
                .andExpect(jsonPath("$.messageId", Matchers.is("fakeMessageId")))
                .andExpect(jsonPath("$.dbOutcome", Matchers.is("fakeOutcome")))
                .andExpect(jsonPath("$.date", Matchers.notNullValue()))
                .andExpect(jsonPath("$.date").value(Matchers.lessThanOrEqualTo(new Date().getTime())));
    }

    @Test
    public void convertThrowsException() throws Exception {
        thrown.expect(NotFoundException.class);
        thrown.expectMessage(Matchers.containsString("Object \"testObject\" does not exist."));

        when(conversionService.checkOriginFileExists(anyString())).thenReturn(false);

        ConversionRequest request = new ConversionRequest(new URI("testObject"));
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
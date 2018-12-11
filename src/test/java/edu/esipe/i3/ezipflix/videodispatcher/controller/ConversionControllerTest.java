package edu.esipe.i3.ezipflix.videodispatcher.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.esipe.i3.ezipflix.videodispatcher.definition.ConversionRequest;
import edu.esipe.i3.ezipflix.videodispatcher.service.ConversionService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(MockitoJUnitRunner.class)
public class ConversionControllerTest {

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

        ConversionRequest request = new ConversionRequest(new URI("fakePath"));
        mockMvc.perform(post("/convert")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(jsonPath("$", Matchers.notNullValue()))
                .andExpect(jsonPath("$.uuid", Matchers.notNullValue()))
                .andExpect(jsonPath("$.messageId", Matchers.is("fakeMessageId")))
                .andExpect(jsonPath("$.dbOutcome", Matchers.is("fakeOutcome")));
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
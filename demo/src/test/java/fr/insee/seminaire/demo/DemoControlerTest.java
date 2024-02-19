package fr.insee.seminaire.demo;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = MOCK /*valeur par défaut*/)
@AutoConfigureMockMvc
class DemoControlerTest {

    @MockBean
    private FridgeClient fridgeClient;


    @Test
    void testSteakCache_ok(@Autowired MockMvc mvc) throws Exception {
        long maxAge = 24 * 3600;
        var steak = new Steak(Locale.FRANCE.getCountry(),
                LocalDate.now().plusDays(1),
                10);
        // Mock the service to return the usefull value for the test
        when(fridgeClient.findSteakFromFridge(anyString())).thenReturn(Optional.of(steak));
        // simulate GET to the server
        mvc.perform(get("/steak/" + Locale.FRANCE.getCountry()))
                // check the answer from the server
                .andExpect(status().isOk())
                .andExpect(header().string("cache-control", allOf(containsString("max-age=" + maxAge)
                                                                        , containsString("must-revalidate"))
                ));
    }

    @Test
    void testSteakCache_perime(@Autowired MockMvc mvc) throws Exception {
        var steak = new Steak(Locale.FRANCE.getCountry(),
                LocalDate.now().minusDays(1),
                10);
        // Mock the service to return the usefull value for the test
        when(fridgeClient.findSteakFromFridge(anyString())).thenReturn(Optional.of(steak));
        // simulate GET to the server
        mvc.perform(get("/steak/" + Locale.FRANCE.getCountry()))
                // check the answer from the server
                .andExpect(status().isOk())
                .andExpect(header().string("cache-control", containsString("no-cache")));
    }

    @Test
    void testSteakCache_eTag(@Autowired MockMvc mvc) throws Exception {
        var steak = new Steak(Locale.FRANCE.getCountry(),
                LocalDate.of(2023, 5, 9),
                12);
        // Mock the service to return the usefull value for the test
        when(fridgeClient.findSteakFromFridge(anyString())).thenReturn(Optional.of(steak));
        // simulate GET to the server
        mvc.perform(get("/steak/" + Locale.FRANCE.getCountry()))
                // check the answer from the server
                .andExpect(status().isOk())
                .andExpect(header().string("etag", "\"0a339ac80d98565fdad7332a4529dc1ad\""));
        // Simulate a new GET with If-None-Match:"0a339ac80d98565fdad7332a4529dc1ad"
        var headers=new HttpHeaders();
        headers.put("If-None-Match", List.of("\"0a339ac80d98565fdad7332a4529dc1ad\""));
        mvc.perform(get("/steak/" + Locale.FRANCE.getCountry()).headers(headers))
                .andExpect(status().isNotModified())
                .andExpect(content().string(""));
    }


}
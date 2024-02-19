package fr.insee.seminaire.demo;

import jakarta.json.bind.JsonbBuilder;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
                properties = "spring.main.allow-bean-definition-overriding=true")
class FridgeClientTest {

    @Autowired
    FridgeClient fridgeClient;

    static Cache cache;

    static {
        try {
            cache = new Cache(Files.createTempDirectory("okHttpCacheTest").toFile(), 10 * 1024 * 1024L);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final MockWebServer server = new MockWebServer();
    private static final String serverBaseUrl = server.url("/").toString();

    private final Steak steak = new Steak(Locale.FRANCE.getCountry(),
            LocalDate.of(2023, 5, 12),
            10);

    FridgeClientTest() throws IOException {
    }

    @DynamicPropertySource
    static void mockWebServerBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.openfeign.client.config.fridge.url", () -> serverBaseUrl);
    }

    @Test
    void testSteakInCache() throws InterruptedException {

        enqueueInServerSteakWithCache();
        // First call
        assertEquals(Optional.of(steak), fridgeClient.findSteakFromFridge(Locale.FRANCE.getCountry()));
        assertEquals(1,server.getRequestCount());
        assertEquals(1,cache.requestCount());
        assertEquals(1,cache.networkCount());

        // call again
        assertEquals(Optional.of(steak), fridgeClient.findSteakFromFridge(Locale.FRANCE.getCountry()));
        // and check no more request on server and 1 hit from cache
        assertEquals(1, server.getRequestCount());
        assertEquals(2,cache.requestCount());
        assertEquals(1,cache.hitCount());

        enqueueInServerSteakWithMustRevalidate();
        // call an other resource :
        assertEquals(Optional.of(steak),fridgeClient.findSteakFromFridge(Locale.FRANCE.toString()));
        // and check one more request on server
        assertEquals(2, server.getRequestCount());
        assertEquals(3,cache.requestCount());
        assertEquals(2,cache.networkCount());

        //enqueue 304, and call the previous ressource :
        enqueueInServerNotModified();
        assertEquals(Optional.of(steak),fridgeClient.findSteakFromFridge(Locale.FRANCE.toString()));
        //and check for call :
        RecordedRequest request=null;
        for (int i = 0; i < server.getRequestCount(); i++) {
            request = server.takeRequest();
        }
        assertEquals("blabla", request.getHeader("If-None-Match"));
        // and check one more request
        assertEquals(3, server.getRequestCount());
        assertEquals(4,cache.requestCount());
        assertEquals(3,cache.networkCount());
        assertEquals(2,cache.hitCount());


    }

    private void enqueueInServerNotModified() {
        var response = new MockResponse();
        response.setHeader("cache-control", "no-cache");
        response.setHeader("etag", "blabla");
        response.setResponseCode(304);
        response.setBody("");
        server.enqueue(response);

    }

    private void enqueueInServerSteakWithMustRevalidate() {
        var response = new MockResponse();
        response.setHeader("cache-control", "no-cache");
        response.setHeader("etag", "blabla");
        response.setHeader("content-type", "application/json");
        response.setBody(JsonbBuilder.create().toJson(steak));
        server.enqueue(response);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    private void enqueueInServerSteakWithCache() {
        var response = new MockResponse();
        response.setHeader("cache-control", "max-age=100, must-revalidate");
        response.setHeader("content-type", "application/json");
        response.setBody(JsonbBuilder.create().toJson(steak));
        server.enqueue(response);
    }



    @TestConfiguration
    static class ConfigurationForTests {

        @Bean
        public OkHttpClient.Builder okHttpClientBuilder() {
            var okHttpClientBuilder = new OkHttpClient.Builder();
            okHttpClientBuilder.cache(cache);

            return okHttpClientBuilder;
        }

    }


}

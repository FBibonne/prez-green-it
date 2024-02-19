package fr.insee.seminaire.demo;

import jakarta.servlet.DispatcherType;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.Optional;

@Configuration
public class DemoConfiguration {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> eTagFilter(){
        var registration = new
                FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registration.setDispatcherTypes(EnumSet.allOf(DispatcherType.class));
        return registration;
    }

    @Bean
    public OkHttpClient.Builder okHttpClientBuilder(@Value("${fr.insee.demo.proxy:#{null}}") Optional<String> proxy) throws IOException {
        var okHttpClientBuilder=new OkHttpClient.Builder();
        okHttpClientBuilder.cache(new Cache(Files.createTempDirectory("okHttpCache").toFile(), 10 * 1024 * 1024L));
        proxy.ifPresent(proxyName->okHttpClientBuilder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyName, 8080))));

        return okHttpClientBuilder;
    }

}

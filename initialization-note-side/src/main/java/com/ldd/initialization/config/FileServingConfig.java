package com.ldd.initialization.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Configuration
public class FileServingConfig {

    /** 多个可能的 uploads 根目录（兼容 IDE 和命令行启动） */
    private static final List<String> BASE_DIRS = List.of(
            "uploads",
            "initialization-note-side/uploads",
            "initialization-note-side/target/classes/uploads",
            "target/classes/uploads"
    );

    @Bean
    public FilterRegistrationBean<Filter> fileServingFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                HttpServletRequest req = (HttpServletRequest) request;
                HttpServletResponse res = (HttpServletResponse) response;
                String uri = req.getRequestURI();

                if (uri.startsWith("/file/")) {
                    String relativePath = uri.substring("/file/".length());
                    String userDir = System.getProperty("user.dir");

                    for (String baseDir : BASE_DIRS) {
                        Path filePath = Paths.get(userDir, baseDir, relativePath);
                        if (Files.exists(filePath) && Files.isReadable(filePath)) {
                            String contentType = Files.probeContentType(filePath);
                            res.setContentType(contentType != null ? contentType : "application/octet-stream");
                            res.setHeader("Cache-Control", "max-age=86400");
                            Files.copy(filePath, res.getOutputStream());
                            return;
                        }
                    }
                    log.warn("File not found in any base dir: userDir={}, uri={}", userDir, uri);
                    res.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                chain.doFilter(request, response);
            }
        });
        registration.addUrlPatterns("/file/*");
        registration.setOrder(-100);
        registration.setName("fileServingFilter");
        return registration;
    }
}

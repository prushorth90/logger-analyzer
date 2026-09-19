package dev.loganalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class LogAnalyzerApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogAnalyzerApplication.class, args);
    }
}
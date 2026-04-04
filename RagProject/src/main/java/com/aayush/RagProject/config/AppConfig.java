package com.aayush.RagProject.config;

import com.aayush.RagProject.service.GitService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    CommandLineRunner run(GitService gitService) {
        return args -> {
            String repoUrl = "https://github.com/Aayush987/Java-multithreading-notes.git";
            String localPath = "repo/multithreading";

            gitService.CreateRepoIfNotExists(repoUrl,localPath);
        };
    }
}

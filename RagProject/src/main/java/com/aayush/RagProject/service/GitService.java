package com.aayush.RagProject.service;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

@Slf4j
@Service
public class GitService {

    public void CreateRepoIfNotExists(String repoUrl, String localPath) {
        Path tempDir = Paths.get("github-repo");


        if(!Files.exists(tempDir)) {

        try {
            // 1. Clone repo
            log.info("🚀 Cloning repo into {}", tempDir);
            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(tempDir.toFile()) // JGit needs File
                    .call();

            // 2. Locate Hugo content folder
            Path contentPath = tempDir.resolve("content");

            if (!Files.exists(contentPath)) {
                throw new RuntimeException("No /content folder found in repo");
            }

            // 3. Create destination directory
            Path targetPath = Paths.get(localPath);
            Files.createDirectories(targetPath);

            // 4. Copy all .md files
            try (Stream<Path> paths = Files.walk(contentPath)) {
                paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".md"))
                        .forEach(source -> {
                            try {
                                log.info("File is getting filtered");
                                Path relative = contentPath.relativize(source);
                                Path destination = targetPath.resolve(relative);

                                Files.createDirectories(destination.getParent());
                                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

                            } catch (IOException e) {
                                throw new RuntimeException("Error copying file: " + source, e);
                            }
                        });
            }
        }catch (Exception e) {
            throw new RuntimeException("Error cloning Repo." + e);
        }
        } else {
            log.info("Repo is already there" + tempDir.toAbsolutePath());
        }
    }
}

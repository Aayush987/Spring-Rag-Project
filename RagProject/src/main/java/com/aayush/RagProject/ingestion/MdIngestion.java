package com.aayush.RagProject.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Slf4j
@Service
public class MdIngestion {
    private static final String DIRECTORY =  "repo/";

    public void ingestFiles() {
        Path rootPath = Paths.get(DIRECTORY);
        try (Stream<Path> paths = Files.walk(rootPath)){
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".md"))
                        .filter(path -> !path.getFileName().toString().equals("_index.md"))
                        .forEach(this::ingestSingleFile);
        } catch (IOException e) {
            throw new RuntimeException("Error reading Markdown files",e);
        }
    }

    public void ingestSingleFile(Path filePath) {
        log.info("Reading File {}", filePath.getFileName());
         try{
             String content = Files.readString(filePath);
             log.info("Ingestion for {} done", filePath.getFileName());
         } catch (Exception e) {
             throw new RuntimeException("Error ingesting this file",e);
         }
    }
}

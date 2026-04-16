package com.aayush.RagProject.ingestion;

import com.aayush.RagProject.chunking.model.Chunk;
import com.aayush.RagProject.embedding.model.EmbeddedChunk;
import com.aayush.RagProject.embedding.service.EmbeddingService;
import com.aayush.RagProject.vectorstore.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MdIngestion {
    private static final String DIRECTORY =  "repo/";

    private final EmbeddingService embeddingService;
//    private final VectorStoreService vectorStoreService;
    private final VectorStore vectorStore;

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
        final int BATCH_SIZE = 50;
         try{
             String content = removeFrontmatter(Files.readString(filePath));
             List<String> rawChunks = chunkMarkdown(content);

//
//             content = null;

             int index = 0;
             int totalProcessed = 0;
             List<Document> batch = new ArrayList<>(BATCH_SIZE);

             for (String chunkText : rawChunks) {

                 String section = extractSection(chunkText);

                 Chunk chunk = Chunk.builder()
                         .content(addSectionContext(chunkText, section))
                         .chunkIndex(index++)
                         .source(filePath.toString())
                         .section(section)
                         .metadata(Map.of(
                                 "fileName", filePath.getFileName().toString()
                         ))
                         .build();

                 batch.add(toDocument(chunk));

                 if (batch.size() >= BATCH_SIZE) {
                     vectorStore.add(batch);
                     batch.clear();
                 }

                 totalProcessed++;

                 if (index % 100 == 0) {
                     log.info("Processed {} chunks so far for {}", index, filePath.getFileName());
                 }
             }
             if (!batch.isEmpty()) {
                 vectorStore.add(batch);
             }
//             rawChunks = null;
             log.info("✅ Total chunks processed: {} for {}", totalProcessed, filePath.getFileName());

         }catch (OutOfMemoryError e) {
             log.error("OOM while processing file: {}. File may be too large.", filePath.getFileName());
             throw new RuntimeException("Out of memory processing file: " + filePath.getFileName(), e);
         }
         catch (Exception e) {
             throw new RuntimeException("Error ingesting this file",e);
         }
    }

    private Document toDocument(Chunk chunk) {

        Map<String, Object> metadata = new HashMap<>();
        Map<String,Object> mp = chunk.getMetadata();
        metadata.put("source", chunk.getSource());
        metadata.put("chunkIndex", chunk.getChunkIndex());
        metadata.put("fileName",mp.get("fileName"));

        if (chunk.getMetadata() != null) {
            metadata.put("filename", chunk.getMetadata().get("fileName"));
        }

        return new Document(chunk.getContent(), metadata);
    }

//    private void processChunk(Chunk chunk) {
//        // e.g., vectorStore.save(chunk); or embeddingService.embed(chunk);
//        log.info("Processing chunk [{}] from {}", chunk.getChunkIndex(), chunk.getSource());
//        Map<String,Object> mp = chunk.getMetadata();
//        Document document = new Document(
//                chunk.getContent(),
//                Map.of(
//                        "source",chunk.getSource(),
//                        "chunkIndex",chunk.getChunkIndex(),
//                        "filename",mp.get("fileName")
//                )
//        );
//        vectorStore.add(document);
//
//    }

    /*
        ExtractSection working:
        line = "## Thread Lifecycle"
        line.startsWith("#") → ✅ true
        line.replace("#", "") → " Thread Lifecycle"
        .trim() → "Thread Lifecycle"
     */
    private String extractSection(String text) {
        String[] lines = text.split("\n");
        for (String line : lines) {
            if (line.startsWith("#")) {
                return line.replace("#", "").trim();
            }
        }
        return "General";
    }

    private String addSectionContext(String content, String section) {
        return "[" + section + "]\n\n" + content;
    }

    private List<String> chunkMarkdown(String content) {
        List<String> sections = splitBySections(content);
        List<String> finalChunks = new ArrayList<>();

        int MAX_SIZE = 1000;

        for (String section : sections) {
            if (section.length() > MAX_SIZE) {
                finalChunks.addAll(splitLargeChunk(section, MAX_SIZE));
            } else {
                finalChunks.add(section);
            }
        }

        return finalChunks;
    }

    /**
     * Removes frontmatter (metadata block) from the beginning of a string.
     * Frontmatter is expected to be enclosed between '---' markers.
     * Example:
     * Input:
     * ---
     * title: Hello
     * author: Sam
     * ---
     * This is the actual content.
     *
     * Output:
     * This is the actual content.
     *
     * @param content the full text content that may contain frontmatter
     * @return the content without the frontmatter block, or original content if none found
     */

    private String removeFrontmatter(String content) {
        if (content.startsWith("---")) {
            int end = content.indexOf("\n---", 3);
            if (end != -1) {
                return content.substring(end + 4).trim();
            }
        }
        return content;
    }

public List<String> splitBySections(String content) {
    List<String> sections = new ArrayList<>();

    String[] lines = content.split("\n");
    StringBuilder current = new StringBuilder();

    for (String line : lines) {
        if (line.startsWith("#")) {
            if (!current.isEmpty()) {
                sections.add(current.toString().trim());
                current = new StringBuilder();
            }
        }
        current.append(line).append("\n");
    }

    if (!current.isEmpty()) {
        sections.add(current.toString().trim());
    }

    return sections;
}

    public List<String> splitLargeChunk(String text, int maxSize) {
        List<String> chunks = new ArrayList<>();

        int overlap = 100;
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + maxSize, text.length());
            chunks.add(text.substring(start, end));
            if (end == text.length()) break;

            start = end - overlap;
        }

        return chunks;
    }
}

package com.aayush.RagProject.retrieval;

import com.aayush.RagProject.chunking.model.Chunk;
import com.aayush.RagProject.retrieval.model.RetrievalResult;
import com.aayush.RagProject.vectorstore.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RetrievalService {

    private final VectorStore vectorStore;
//
    public RetrievalResult retrieve(String query) {
        log.info("Retrieving....");
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(5)
//                .similarityThreshold(0.75)
                .build();

        List<Document> results = vectorStore.similaritySearch(searchRequest);
        log.info("results {}", results);
        log.info("Retrieve results of {} size", results.size());

        List<Chunk> chunkResult = results.stream()
                .map(this::toChunk)
                .collect(Collectors.toList());

        log.info("Chunk {}",chunkResult.get(0));

        return new RetrievalResult(chunkResult);
    }

    public Chunk toChunk(Document document) {
        Map<String, Object> metadata = document.getMetadata() != null
                ? new HashMap<>(document.getMetadata())
                : new HashMap<>();

        // Content (Spring AI usually stores in text, not doc_content)
        String content = document.getText();

        // Extract values safely
        String source = String.valueOf(metadata.getOrDefault("source", "unknown"));
        String section = String.valueOf(metadata.getOrDefault("fileName", "unknown"));

        int chunkIndex = 0;
        Object chunkIndexValue = metadata.get("chunkIndex");
        if (chunkIndexValue != null) {
            try {
                chunkIndex = Integer.parseInt(chunkIndexValue.toString());
            } catch (Exception e) {
                chunkIndex = 0;
            }
        }

        Chunk chunk = new Chunk(content, chunkIndex, source, section, metadata);
//        chunk.setSection(section);

        return chunk;
    }

}

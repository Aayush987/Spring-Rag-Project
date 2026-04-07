package com.aayush.RagProject.embedding.service;

import com.aayush.RagProject.chunking.model.Chunk;
import com.aayush.RagProject.embedding.model.EmbeddedChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddedChunk embed(Chunk chunk) {
        log.info("Embedding started");
        float[] vector = embeddingModel.embed(chunk.getContent());
        log.info("Embedded vector {}",vector);
        return new EmbeddedChunk(chunk,vector);
    }

}

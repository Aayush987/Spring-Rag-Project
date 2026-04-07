package com.aayush.RagProject.vectorstore;

import com.aayush.RagProject.chunking.model.Chunk;
import com.aayush.RagProject.embedding.model.EmbeddedChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final VectorStore vectorStore;

    public void store(Chunk chunk) {
        Document document = new Document(
                chunk.getContent(),
                Map.of(
                        "chunkIndex", chunk.getChunkIndex(),
                        "source",     chunk.getSource(),
                        "section",    chunk.getSection(),
                        "fileName",   chunk.getMetadata().get("fileName")
                )
        );
        vectorStore.add(List.of(document));

        log.info("✅ Stored chunk [{}] from section '{}' into Qdrant",
                chunk.getChunkIndex(), chunk.getSection());
    }
}

package com.aayush.RagProject.embedding.model;

import com.aayush.RagProject.chunking.model.Chunk;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmbeddedChunk {
    private final Chunk chunk;
    private final float[] vector;
}

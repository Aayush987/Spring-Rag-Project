package com.aayush.RagProject.chunking.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Chunk {

    private String content;
    private int chunkIndex;
    private String source;
    private String section;
    private Map<String, Object> metadata;


}

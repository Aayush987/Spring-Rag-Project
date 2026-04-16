package com.aayush.RagProject.controllers;

import com.aayush.RagProject.chunking.model.Chunk;
import com.aayush.RagProject.retrieval.RetrievalService;
import com.aayush.RagProject.retrieval.model.RetrievalResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ChatController {

    private final ChatClient chatClient;
    private final RetrievalService retrievalService;
    private final VectorStore vectorStore;


    @GetMapping("/chat")
    String generation(@RequestParam String userInput) {
        RetrievalResult result = retrievalService.retrieve(userInput);
        String context = result.getChunks().stream()
                .map(Chunk::getContent)
                .collect(Collectors.joining("\n\n"));

//        return this.chatClient.prompt()
//                .user(userInput)
////                .advisors(
////                        new QuestionAnswerAdvisor(vectorStore)
////                )
//                .call()
//                .content();
        return chatClient.prompt()
                .user(u -> u.text("""
                Use the following context to answer:

                {context}

                Question: {question}
            """)
                        .param("context", context)
                        .param("question", userInput))
                .call()
                .content();
    }
}

package com.aayush.RagProject.controllers;

import com.aayush.RagProject.chunking.model.Chunk;
import com.aayush.RagProject.retrieval.RetrievalService;
import com.aayush.RagProject.retrieval.model.RetrievalResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@Slf4j
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

        log.info("=== CONTEXT SENT TO LLM ===\n{}", context);
        log.info("=== CONTEXT LENGTH: {} chars ===", context.length());

        return chatClient.prompt()
//                .system("""
//        You are an expert Java multithreading tutor. Your job is to answer questions\s
//        strictly based on the provided study notes context.
//
//        Rules you MUST follow:
//        - Answer ONLY from the context provided. Do NOT use outside knowledge.
//        - If the context does not contain enough information to answer, respond with:
//          "I don't have notes on that topic yet. Try asking something covered in the multithreading notes."
//        - Be concise but thorough. Use bullet points or numbered steps for clarity when helpful.
//        - If the question involves code, include a short Java snippet to illustrate the concept.
//        - Never make up APIs, class names, or behaviors that aren't in the context.
//        - If the question is completely unrelated to Java or multithreading, politely redirect:
//          "This assistant is focused on Java multithreading topics only."
//       \s""")
//                .user(u -> u.text("""
//        Context from notes:
//        ---------------------
//        {context}
//        ---------------------
//
//        Student's question: {question}
//
//        Answer clearly and helpfully based only on the context above.
//        If the context is insufficient, say so honestly.
//        """)
//                        .param("context", context)
//                        .param("question", userInput))
//                .call()
//                .content();
                .system("""
        You are an expert Java multithreading tutor helping students understand their notes.
        
        Rules:
        - The context below contains ACTUAL NOTE CONTENT extracted from the student's notes. 
          Read it carefully and answer from it.
        - IMPORTANT: If the context contains ANY relevant information, use it to answer fully.
          Do NOT say you lack information if the context has relevant content.
//        - Only say you don't have notes if the context is completely empty or 100% unrelated.
        - Use bullet points or numbered steps for clarity.
        - Include short Java snippets when helpful.
        - If the question is unrelated to Java or multithreading, politely redirect.
        """)
                .user(u -> u.text("""
        Here are the actual notes retrieved for this question:
        =====================================================
        {context}
        =====================================================
        
        Based on the notes above, answer this question thoroughly:
        {question}
        
        Important: The notes above are the ACTUAL CONTENT, not just file references.
        Read them carefully and provide a complete answer from what is written there.
        """)
                        .param("context", context)
                        .param("question", userInput))
                .call()
                .content();
    }
}

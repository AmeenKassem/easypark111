package com.example.demo.controller;

import com.example.demo.dto.AiQueryRequest;
import com.example.demo.dto.ParkingSearchFilters;
import com.example.demo.service.ParkingAgent;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/parking-spots")
@CrossOrigin(origins = "*")
public class AiSearchController {

    private final ParkingAgent parkingAgent;

    public AiSearchController(@Value("${langchain4j.googleai.gemini.api-key}") String apiKey) {
        ChatLanguageModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash")
                .temperature(0.0)
                .build();

        this.parkingAgent = AiServices.create(ParkingAgent.class, model);
    }

    @PostMapping("/ai-search")
    public ParkingSearchFilters extractFiltersWithAi(@RequestBody AiQueryRequest request) {

        String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));


        return parkingAgent.extractFilters(request.getQuery(), currentDateTime);
    }
}
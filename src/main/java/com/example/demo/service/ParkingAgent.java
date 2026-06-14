package com.example.demo.service;

import com.example.demo.dto.ParkingSearchFilters;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ParkingAgent {

    @UserMessage("You are a parking search assistant. Today's date and time is: {{currentDate}}.\n" +
            "Extract the search parameters from the user's text: '{{userText}}'.\n" +
            "Return them as a structured object.\n" +
            "CRITICAL RULES:\n" +
            "- ONLY extract fields that are explicitly mentioned by the user.\n" +
            "- If the user does not mention a specific date or time, you MUST leave 'date', 'startTime', and 'endTime' as null. Do NOT default to the current date or time.\n" +
            "- If the user does not mention a max price, leave 'maxPrice' as null.\n" +
            "- For 'date', use YYYY-MM-DD format.\n" +
            "- For 'startTime' and 'endTime', use HH:mm (24-hour) format.\n" +
            "- If the user says 'for 2 hours', calculate the endTime based on the startTime.")
    ParkingSearchFilters extractFilters(@V("userText") String userText, @V("currentDate") String currentDate);
}
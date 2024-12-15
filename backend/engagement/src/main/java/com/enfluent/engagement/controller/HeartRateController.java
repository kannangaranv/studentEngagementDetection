package com.enfluent.engagement.controller;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HeartRateController {

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello, World!";
    }

    @PostMapping("/fetchHeartRate")
    public List<Integer> fetchHeartRate(@RequestBody Map<String, String> requestBody) {
        String startTime = requestBody.get("startTime");
        String endTime = requestBody.get("endTime");
        String accessToken = requestBody.get("accessToken");

        String url = "https://api.hcgateway.shuchir.dev/api/v2/fetch/heartRate";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of("queries", Map.of()), headers);
        @SuppressWarnings("unchecked")
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, (Class<List<Map<String, Object>>>) (Class<?>) List.class);

        List<Integer> heartRates = new ArrayList<>();
        Instant requestStart = Instant.parse(startTime);
        Instant requestEnd = Instant.parse(endTime);

        if (response.getBody() != null) {
            for (Map<String, Object> record : response.getBody()) {
                Instant recordStart = Instant.parse(record.get("start").toString());
                Instant recordEnd = Instant.parse(record.get("end").toString());

                if (requestStart.compareTo(recordStart) <= 0 && requestEnd.compareTo(recordEnd) >= 0) {
                    extractHeartRates(record, heartRates);
                } else if (requestStart.compareTo(recordStart) <= 0 && requestEnd.compareTo(recordEnd) <= 0) {
                    extractHeartRatesUntil(record, heartRates, requestEnd);
                } else if (requestStart.compareTo(recordStart) >= 0 && requestEnd.compareTo(recordEnd) <= 0) {
                    extractHeartRatesBetween(record, heartRates, requestStart, requestEnd);
                } else if (requestStart.compareTo(recordStart) >= 0 && requestEnd.compareTo(recordEnd) >= 0) {
                    extractHeartRatesFrom(record, heartRates, requestStart);
                }
            }
        }
        return heartRates;
    }

    private void extractHeartRates(Map<String, Object> record, List<Integer> heartRates) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) record.get("data");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> samples = (List<Map<String, Object>>) data.get("samples");

        for (Map<String, Object> sample : samples) {
            heartRates.add((Integer) sample.get("beatsPerMinute"));
        }
    }

    private void extractHeartRatesUntil(Map<String, Object> record, List<Integer> heartRates, Instant endTime) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) record.get("data");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> samples = (List<Map<String, Object>>) data.get("samples");

        for (Map<String, Object> sample : samples) {
            Instant sampleTime = Instant.parse(sample.get("time").toString());
            if (sampleTime.compareTo(endTime) <= 0) {
                heartRates.add((Integer) sample.get("beatsPerMinute"));
            }
        }
    }

    private void extractHeartRatesBetween(Map<String, Object> record, List<Integer> heartRates, Instant startTime, Instant endTime) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) record.get("data");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> samples = (List<Map<String, Object>>) data.get("samples");

        for (Map<String, Object> sample : samples) {
            Instant sampleTime = Instant.parse(sample.get("time").toString());
            if (sampleTime.compareTo(startTime) >= 0 && sampleTime.compareTo(endTime) <= 0) {
                heartRates.add((Integer) sample.get("beatsPerMinute"));
            }
        }
    }

    private void extractHeartRatesFrom(Map<String, Object> record, List<Integer> heartRates, Instant startTime) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) record.get("data");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> samples = (List<Map<String, Object>>) data.get("samples");

        for (Map<String, Object> sample : samples) {
            Instant sampleTime = Instant.parse(sample.get("time").toString());
            if (sampleTime.compareTo(startTime) >= 0) {
                heartRates.add((Integer) sample.get("beatsPerMinute"));
            }
        }
    }
} 

package com.enfluent.engagement.controller;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api")
public class HeartRateController {

    @GetMapping("/hello")
    public String sayHello() {
        return "Hello, World!";
    }

    @PostMapping("/fetchHeartRateAndStressScore")
    public List<Map<String, Object>> fetchHeartRate(@RequestBody Map<String, String> requestBody) {
        String startTime = requestBody.get("startTime");
        String endTime = requestBody.get("endTime");
        String accessToken = requestBody.get("accessToken");

        Instant requestStart = Instant.parse(startTime);
        Instant requestEnd = Instant.parse(endTime);

        List<Map<String, Object>> stressScoresByInterval = new ArrayList<>();
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        String url = "https://api.hcgateway.shuchir.dev/api/v2/fetch/heartRate";

        while (requestStart.isBefore(requestEnd)) {
            Instant intervalEnd = requestStart.plus(Duration.ofMinutes(10));
            if (intervalEnd.isAfter(requestEnd)) {
                intervalEnd = requestEnd;
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of("queries", Map.of()), headers);
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, (Class<List<Map<String, Object>>>) (Class<?>) List.class);

            List<Integer> heartRates = new ArrayList<>();

            if (response.getBody() != null) {
                for (Map<String, Object> record : response.getBody()) {
                    Instant recordStart = Instant.parse(record.get("start").toString());
                    Instant recordEnd = Instant.parse(record.get("end").toString());

                    if (requestStart.compareTo(recordStart) <= 0 && intervalEnd.compareTo(recordEnd) >= 0) {
                        extractHeartRates(record, heartRates);
                    } else if (requestStart.compareTo(recordStart) <= 0 && intervalEnd.compareTo(recordEnd) <= 0) {
                        extractHeartRatesUntil(record, heartRates, intervalEnd);
                    } else if (requestStart.compareTo(recordStart) >= 0 && intervalEnd.compareTo(recordEnd) <= 0) {
                        extractHeartRatesBetween(record, heartRates, requestStart, intervalEnd);
                    } else if (requestStart.compareTo(recordStart) >= 0 && intervalEnd.compareTo(recordEnd) >= 0) {
                        extractHeartRatesFrom(record, heartRates, requestStart);
                    }
                }
            }
            double stressScore = calculateStressScore(heartRates);
            stressScoresByInterval.add(Map.of(
                    "intervalStart", requestStart.toString(),
                    "intervalEnd", intervalEnd.toString(),
                    "heartRates", heartRates,
                    "stressScore", stressScore
            ));
            requestStart = intervalEnd;
        }
        return stressScoresByInterval;
    }

    private void extractHeartRates(Map<String, Object> record, List<Integer> heartRates) {
        List<Map<String, Object>> samples = (List<Map<String, Object>>) ((Map<String, Object>) record.get("data")).get("samples");
        for (Map<String, Object> sample : samples) {
            heartRates.add((Integer) sample.get("beatsPerMinute"));
        }
    }

    private void extractHeartRatesUntil(Map<String, Object> record, List<Integer> heartRates, Instant endTime) {
        List<Map<String, Object>> samples = (List<Map<String, Object>>) ((Map<String, Object>) record.get("data")).get("samples");
        for (Map<String, Object> sample : samples) {
            Instant sampleTime = Instant.parse(sample.get("time").toString());
            if (sampleTime.compareTo(endTime) <= 0) {
                heartRates.add((Integer) sample.get("beatsPerMinute"));
            }
        }
    }

    private void extractHeartRatesBetween(Map<String, Object> record, List<Integer> heartRates, Instant startTime, Instant endTime) {
        List<Map<String, Object>> samples = (List<Map<String, Object>>) ((Map<String, Object>) record.get("data")).get("samples");
        for (Map<String, Object> sample : samples) {
            Instant sampleTime = Instant.parse(sample.get("time").toString());
            if (sampleTime.compareTo(startTime) >= 0 && sampleTime.compareTo(endTime) <= 0) {
                heartRates.add((Integer) sample.get("beatsPerMinute"));
            }
        }
    }

    private void extractHeartRatesFrom(Map<String, Object> record, List<Integer> heartRates, Instant startTime) {
        List<Map<String, Object>> samples = (List<Map<String, Object>>) ((Map<String, Object>) record.get("data")).get("samples");
        for (Map<String, Object> sample : samples) {
            Instant sampleTime = Instant.parse(sample.get("time").toString());
            if (sampleTime.compareTo(startTime) >= 0) {
                heartRates.add((Integer) sample.get("beatsPerMinute"));
            }
        }
    }

    private double calculateStressScore(List<Integer> hr) {
        if (hr.isEmpty()) {
            return 0;
        }
        List<Double> rrIntervals = new ArrayList<>();
        for (int bpm : hr) {
            rrIntervals.add(60.0 / bpm);
        }

        double minRR = Collections.min(rrIntervals);
        double maxRR = Collections.max(rrIntervals);
        Map<Double, Long> frequency = new HashMap<>();

        for (double rr : rrIntervals) {
            frequency.put(rr, frequency.getOrDefault(rr, 0L) + 1);
        }

        double mode = rrIntervals.get(0);
        long modeFreq = 0;
        for (Map.Entry<Double, Long> entry : frequency.entrySet()) {
            if (entry.getValue() > modeFreq) {
                modeFreq = entry.getValue();
                mode = entry.getKey();
            }
        }

        double VR = maxRR - minRR;
        double Amode = (modeFreq * mode / rrIntervals.size()) * 100;

        return Math.abs(VR) < 0.0001 ? 0 : Math.sqrt(Math.min(1000, Amode / (2 * VR * mode)));
    }
}

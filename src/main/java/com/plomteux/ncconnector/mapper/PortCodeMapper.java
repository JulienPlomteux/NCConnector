package com.plomteux.ncconnector.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
@Slf4j
@Component
public class PortCodeMapper {

    private static final Map<String, String> portCodeToCityMap = new HashMap<>();

    static {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ClassLoader classLoader = PortCodeMapper.class.getClassLoader();
            try (InputStream inputStream = classLoader.getResourceAsStream("ports.json")) {
                if (inputStream == null) {
                    throw new IOException("Resource not found: ports.json");
                }
                JsonNode rootNode = objectMapper.readTree(inputStream);
                rootNode.fields().forEachRemaining(entry -> {
                    String portCode = entry.getKey();
                    String cityName = entry.getValue().get("city").asText();
                    portCodeToCityMap.put(portCode, cityName);
                });
            }
        } catch (IOException e) {
            log.error("Error reading ports.json", e);
        }
    }

    public static String getCityName(String portCode) {
        return portCodeToCityMap.getOrDefault(portCode, portCode);
    }
}
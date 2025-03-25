package com.example.energybot_weather_app.controller;

import com.example.energybot_weather_app.model.WeatherRecord;
import com.example.energybot_weather_app.service.WeatherDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class WeatherApiController {
    private static final Logger logger = LoggerFactory.getLogger(WeatherApiController.class);
    
    private final WeatherDataService weatherDataService;
    
    public WeatherApiController(WeatherDataService weatherDataService) {
        this.weatherDataService = weatherDataService;
    }
    
    // API documentation endpoint
    @GetMapping
    public ResponseEntity<Map<String, Object>> getApiDocumentation() {
        Map<String, Object> docs = new LinkedHashMap<>();
        
        docs.put("name", "Weather Data API");
        docs.put("description", "API for retrieving NOAA weather station data");
        
        // Endpoints documentation
        Map<String, Object> endpoints = new LinkedHashMap<>();
        
        // Station data endpoint
        Map<String, Object> stationEndpoint = new LinkedHashMap<>();
        stationEndpoint.put("method", "GET");
        stationEndpoint.put("description", "Get weather data for a specific station");
        stationEndpoint.put("url", "/api/station/{stationId}");
        
        // Parameters for station endpoint
        Map<String, Object> stationParams = new LinkedHashMap<>();
        stationParams.put("stationId", "Path parameter - The 11-character station ID");
        stationParams.put("elementType", "Optional query parameter - Filter by element type (e.g., TMAX, PRCP, TMIN)");
        stationParams.put("startDate", "Optional query parameter - Filter by start date in YYYYMMDD format");
        stationParams.put("endDate", "Optional query parameter - Filter by end date in YYYYMMDD format");
        
        stationEndpoint.put("parameters", stationParams);
        
        Map<String, Object> stationExamples = new LinkedHashMap<>();
        stationExamples.put("Basic request", "/api/station/USS0013B25S");
        stationExamples.put("Filter by element", "/api/station/USS0013B25S?elementType=TMAX");
        stationExamples.put("Filter by date range", "/api/station/USS0013B25S?startDate=20240101&endDate=20240131");
        stationExamples.put("Combined filters", "/api/station/USS0013B25S?elementType=TMAX&startDate=20240101&endDate=20240131");
        
        stationEndpoint.put("examples", stationExamples);
        
        endpoints.put("stationData", stationEndpoint);
        docs.put("endpoints", endpoints);
        
        // Data format information
        Map<String, String> elementCodes = new LinkedHashMap<>();
        elementCodes.put("PRCP", "Precipitation (tenths of mm)");
        elementCodes.put("SNOW", "Snowfall (mm)");
        elementCodes.put("SNWD", "Snow depth (mm)");
        elementCodes.put("TMAX", "Maximum temperature (tenths of degrees C)");
        elementCodes.put("TMIN", "Minimum temperature (tenths of degrees C)");
        
        docs.put("elementCodes", elementCodes);
        docs.put("dataSource", "https://www.ncei.noaa.gov/pub/data/ghcn/daily/by_year/2024.csv.gz");
        
        return ResponseEntity.ok(docs);
    }
    
    // Get weather data for a specific station
    @GetMapping("/station/{stationId}")
    public ResponseEntity<?> getStationData(
            @PathVariable String stationId,
            @RequestParam(required = false) String elementType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        logger.info("Received request for station data: {}, elementType: {}, startDate: {}, endDate: {}", 
                stationId, elementType, startDate, endDate);
        
        try {
            List<WeatherRecord> stationData = weatherDataService.getStationData(stationId, elementType, startDate, endDate);
            
            if (stationData == null) {
                logger.warn("No data found for station ID: {}", stationId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No data found for station ID: " + stationId));
            }
            
            if (stationData.isEmpty()) {
                logger.info("No matching records found for station ID: {} with the specified filters", stationId);
                return ResponseEntity.ok()
                        .body(Map.of("message", "No matching records found with the specified filters",
                                "stationId", stationId,
                                "count", 0,
                                "data", stationData));
            }
            
            return ResponseEntity.ok()
                    .body(Map.of("stationId", stationId,
                            "count", stationData.size(),
                            "data", stationData));
        } catch (Exception e) {
            logger.error("Error retrieving data for station {}: {}", stationId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving station data: " + e.getMessage()));
        }
    }
} 
package com.example.energybot_weather_app.service;

import com.example.energybot_weather_app.model.WeatherRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for retrieving weather data from the processed files
 */
@Service
public class WeatherDataService {
    private static final Logger logger = LoggerFactory.getLogger(WeatherDataService.class);
    
    @Value("${weather.data.directory}")
    private String dataDir;
    
    private final ObjectMapper objectMapper;
    
    public WeatherDataService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Retrieve weather data for a specific station with optional filtering
     * 
     * @param stationId The station ID to retrieve data for
     * @return List of WeatherRecord objects for the station, or null if not found
     * @throws IOException if there is an error reading the file
     */
    public List<WeatherRecord> getStationData(String stationId) throws IOException {
        return getStationData(stationId, null, null, null);
    }
    
    /**
     * Retrieve weather data for a specific station with optional filtering
     * 
     * @param stationId The station ID to retrieve data for
     * @param elementType Optional filter for specific element type (e.g., TMAX, PRCP)
     * @param startDate Optional filter for start date (YYYYMMDD format)
     * @param endDate Optional filter for end date (YYYYMMDD format)
     * @return List of WeatherRecord objects for the station, or null if not found
     * @throws IOException if there is an error reading the file
     */
    public List<WeatherRecord> getStationData(String stationId, String elementType, String startDate, String endDate) throws IOException {
        // Normalize station ID to prevent path traversal attacks
        stationId = normalizeStationId(stationId);
        
        Path stationFilePath = Paths.get(dataDir, stationId + ".json");
        File stationFile = stationFilePath.toFile();
        
        if (!stationFile.exists() || !stationFile.isFile()) {
            logger.warn("Station data file not found: {}", stationFilePath);
            return null;
        }
        
        // Check file size to avoid potential OOM for very large files
        long fileSize = Files.size(stationFilePath);
        logger.info("Reading station data file: {} (size: {} bytes)", stationFilePath, fileSize);
        
        // Read and parse the JSON file
        List<WeatherRecord> records = objectMapper.readValue(stationFile, new TypeReference<List<WeatherRecord>>() {});
        
        // Apply filters if provided
        if (elementType != null || startDate != null || endDate != null) {
            records = filterRecords(records, elementType, startDate, endDate);
            logger.info("Applied filters: elementType={}, startDate={}, endDate={}, records after filtering: {}", 
                    elementType, startDate, endDate, records.size());
        }
        
        return records;
    }
    
    /**
     * Filter records based on element type and date range
     */
    private List<WeatherRecord> filterRecords(List<WeatherRecord> records, String elementType, String startDate, String endDate) {
        return records.stream()
            .filter(record -> {
                // Filter by element type if provided
                if (elementType != null && !elementType.isEmpty() && !elementType.equalsIgnoreCase(record.getElement())) {
                    return false;
                }
                
                // Filter by start date if provided
                if (startDate != null && !startDate.isEmpty()) {
                    // Compare strings (YYYYMMDD format allows for string comparison)
                    if (record.getDate().compareTo(startDate) < 0) {
                        return false;
                    }
                }
                
                // Filter by end date if provided
                if (endDate != null && !endDate.isEmpty()) {
                    // Compare strings (YYYYMMDD format allows for string comparison)
                    if (record.getDate().compareTo(endDate) > 0) {
                        return false;
                    }
                }
                
                return true;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Normalize the station ID to prevent path traversal attacks
     * Only allow alphanumeric characters, hyphens, and underscores
     */
    private String normalizeStationId(String stationId) {
        // Remove any path characters and only allow safe characters
        return stationId.replaceAll("[^a-zA-Z0-9\\-_]", "");
    }
} 
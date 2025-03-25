package com.example.energybot_weather_app.service;

import com.example.energybot_weather_app.model.WeatherRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

@Service
public class WeatherDataProcessor {
    private static final Logger logger = LoggerFactory.getLogger(WeatherDataProcessor.class);
    
    @Value("${weather.data.url}")
    private String dataUrl;
    
    @Value("${weather.data.directory}")
    private String dataDir;
    
    private final ObjectMapper objectMapper;
    
    public WeatherDataProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    // Initialize the data processing - called at application startup
    public void initializeDataProcessing() {
        try {
            // Create data directory if it doesn't exist
            Path dataDirectory = Paths.get(dataDir);
            if (!Files.exists(dataDirectory)) {
                logger.info("Creating data directory: {}", dataDirectory);
                Files.createDirectories(dataDirectory);
                
                // Download and process data
                downloadAndProcessData();
            } else {
                logger.info("Data directory already exists at: {}", dataDirectory.toAbsolutePath());
                // Check if the directory is empty, process data if needed
                if (Files.list(dataDirectory).findAny().isEmpty()) {
                    logger.info("Data directory is empty. Downloading and processing data...");
                    downloadAndProcessData();
                } else {
                    logger.info("Data files already exist. Skipping download and processing.");
                }
            }
        } catch (IOException e) {
            logger.error("Error initializing data processing", e);
        }
    }
    
    // Download the weather data file and process it
    private void downloadAndProcessData() {
        logger.info("Starting to download weather data from: {}", dataUrl);
        
        try {
            // Download the gzipped data file
            URL url = new URL(dataUrl);
            Path tempFile = Files.createTempFile("weather_data", ".csv.gz");
            
            try (InputStream in = url.openStream()) {
                Files.copy(in, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            
            logger.info("Download complete. Processing data file...");
            
            // Process the data file
            processWeatherDataFile(tempFile);
            
            // Clean up temp file
            Files.deleteIfExists(tempFile);
            
            logger.info("Data processing complete");
            
        } catch (IOException e) {
            logger.error("Error downloading or processing weather data", e);
        }
    }
    
    // Process the downloaded weather data file
    private void processWeatherDataFile(Path dataFile) throws IOException {
        // Map of file writers for each station
        Map<String, Writer> stationWriters = new HashMap<>();
        // Map to track if we've started a station's array
        Map<String, Boolean> stationStarted = new HashMap<>();
        // Track all stations we've seen
        Set<String> allStations = new HashSet<>();
        
        try (
            InputStream fileStream = Files.newInputStream(dataFile);
            GZIPInputStream gzipStream = new GZIPInputStream(fileStream);
            InputStreamReader reader = new InputStreamReader(gzipStream);
            BufferedReader bufferedReader = new BufferedReader(reader)
        ) {
            String line;
            int lineCount = 0;
            int validLines = 0;
            
            while ((line = bufferedReader.readLine()) != null) {
                lineCount++;
                
                // Handle potential empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    // Split by comma, while preserving empty fields
                    String[] parts = line.split(",", -1);
                    
                    // A valid line should have at least the first 4 required fields
                    if (parts.length < 4) {
                        logger.warn("Line {} has fewer than 4 fields: {}", lineCount, line);
                        continue;
                    }
                    
                    // Extract data from the line according to the format
                    String stationId = parts[0].trim();
                    String dateStr = parts[1].trim();
                    String element = parts[2].trim();
                    String value = parts[3].trim();
                    
                    // Check for required fields
                    if (stationId.isEmpty() || dateStr.isEmpty() || element.isEmpty()) {
                        logger.warn("Line {} is missing required fields: {}", lineCount, line);
                        continue;
                    }
                    
                    // Optional fields - might be empty
                    String mFlag = (parts.length > 4) ? parts[4].trim() : "";
                    String qFlag = (parts.length > 5) ? parts[5].trim() : "";
                    String sFlag = (parts.length > 6) ? parts[6].trim() : "";
                    String obsTime = (parts.length > 7) ? parts[7].trim() : "";
                    
                    // Create a weather record object
                    WeatherRecord record = new WeatherRecord(stationId, dateStr, element, value);
                    if (!mFlag.isEmpty()) record.setmFlag(mFlag);
                    if (!qFlag.isEmpty()) record.setqFlag(qFlag);
                    if (!sFlag.isEmpty()) record.setsFlag(sFlag);
                    if (!obsTime.isEmpty()) record.setObsTime(obsTime);
                    
                    // Get or create the writer for this station
                    Writer writer = getStationWriter(stationId, stationWriters, stationStarted);
                    
                    // Write the record to the station's file
                    String json = objectMapper.writeValueAsString(record);
                    if (stationStarted.get(stationId)) {
                        writer.write(",\n");
                    } else {
                        stationStarted.put(stationId, true);
                    }
                    writer.write(json);
                    
                    // Track this station
                    allStations.add(stationId);
                    validLines++;
                    
                    // Log progress periodically
                    if (lineCount % 100000 == 0) {
                        logger.info("Processed {} lines ({} valid), found {} unique stations", 
                                lineCount, validLines, allStations.size());
                    }
                } catch (Exception e) {
                    logger.warn("Error processing line {}: {}. Error: {}", lineCount, line, e.getMessage());
                }
            }
            
            logger.info("Finished reading data file. Total lines: {}, Valid lines: {}, Unique stations: {}", 
                    lineCount, validLines, allStations.size());
            
            // Close all writers and finalize the JSON files
            for (Map.Entry<String, Writer> entry : stationWriters.entrySet()) {
                String stationId = entry.getKey();
                Writer writer = entry.getValue();
                
                // Write the closing bracket for the JSON array
                writer.write("\n]");
                writer.close();
            }
            
            logger.info("Successfully wrote data files for {} stations in {}", 
                    allStations.size(), Paths.get(dataDir).toAbsolutePath());
            
        } catch (IOException e) {
            logger.error("Error processing weather data file", e);
            
            // Make sure to close any open writers in case of exception
            for (Writer writer : stationWriters.values()) {
                try {
                    writer.close();
                } catch (IOException closeEx) {
                    logger.error("Error closing writer", closeEx);
                }
            }
            
            throw e;
        }
    }
    
    // Gets or creates a writer for a station ID
    private Writer getStationWriter(String stationId, Map<String, Writer> stationWriters, Map<String, Boolean> stationStarted) throws IOException {
        if (!stationWriters.containsKey(stationId)) {
            // Create a new file for this station
            Path stationFile = Paths.get(dataDir, stationId + ".json");
            Writer writer = new BufferedWriter(new FileWriter(stationFile.toFile()));
            
            // Start the JSON array
            writer.write("[\n");
            stationWriters.put(stationId, writer);
            stationStarted.put(stationId, false);
        }
        
        return stationWriters.get(stationId);
    }
} 
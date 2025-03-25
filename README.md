# EnergyBot Weather Data API

This is my submission for EnergyBot's coding project. The application provides a REST API for accessing NOAA weather station data.

## Building and Running

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/energybot-weather-app.git
   cd energybot-weather-app
   ```

2. Build and run with Docker:
   ```bash
   docker build -t energybot-weather-app .
   docker run -p 8080:8080 energybot-weather-app
   ```

3. Or build and run locally:
   ```bash
   ./gradlew build
   java -jar build/libs/energybot-weather-app-0.0.1-SNAPSHOT.jar
   ```

4. Access through API or web interface:
   - Web Interface: Open http://localhost:8080 in your browser
   - API Endpoint: http://localhost:8080/api/weather-data
     - Parameters:
       - stationId (required): The NOAA station ID (e.g., USC00186350)
       - elementType (optional): Type of weather data (PRCP, TMAX, TMIN, SNOW, SNWD)
       - startDate (optional): Start date in YYYY-MM-DD format
       - endDate (optional): End date in YYYY-MM-DD format
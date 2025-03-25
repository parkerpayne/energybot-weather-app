package com.example.energybot_weather_app;

import com.example.energybot_weather_app.service.WeatherDataProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@RestController
@SpringBootApplication
public class EnergybotWeatherAppApplication {

	/**
	 * Home page endpoint - serves the index.html template
	 */
	@GetMapping("/")
	public ModelAndView index() {
		return new ModelAndView("index");
	}

	public static void main(String[] args) {
		SpringApplication.run(EnergybotWeatherAppApplication.class, args);
	}
	
	@Bean
	public CommandLineRunner initializeWeatherData(WeatherDataProcessor weatherDataProcessor) {
		return args -> {
			// Initialize the weather data processor on startup
			weatherDataProcessor.initializeDataProcessing();
		};
	}
}

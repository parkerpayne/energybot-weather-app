package com.example.energybot_weather_app.model;

import com.fasterxml.jackson.annotation.JsonInclude;

// Model class representing a single weather record from the NOAA dataset
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WeatherRecord {
    private String stationId;
    private String date;
    private String element;
    private String value;
    private String mFlag;
    private String qFlag;
    private String sFlag;
    private String obsTime;

    // Default constructor
    public WeatherRecord() {
    }
    
    // Constructor with required fields
    public WeatherRecord(String stationId, String date, String element, String value) {
        this.stationId = stationId;
        this.date = date;
        this.element = element;
        this.value = value;
    }

    // Getters and setters
    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getElement() {
        return element;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getmFlag() {
        return mFlag;
    }

    public void setmFlag(String mFlag) {
        this.mFlag = mFlag;
    }

    public String getqFlag() {
        return qFlag;
    }

    public void setqFlag(String qFlag) {
        this.qFlag = qFlag;
    }

    public String getsFlag() {
        return sFlag;
    }

    public void setsFlag(String sFlag) {
        this.sFlag = sFlag;
    }

    public String getObsTime() {
        return obsTime;
    }

    public void setObsTime(String obsTime) {
        this.obsTime = obsTime;
    }

    @Override
    public String toString() {
        return "WeatherRecord{" +
                "stationId='" + stationId + '\'' +
                ", date='" + date + '\'' +
                ", element='" + element + '\'' +
                ", value='" + value + '\'' +
                ", flags=" + mFlag + qFlag + sFlag +
                ", obsTime='" + obsTime + '\'' +
                '}';
    }
} 
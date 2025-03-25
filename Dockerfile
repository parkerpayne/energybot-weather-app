FROM openjdk:17-jdk-slim

COPY . /app

WORKDIR /app

RUN ./gradlew build --no-daemon -x test

EXPOSE 8080

ENTRYPOINT ["java","-jar","build/libs/energybot-weather-app-0.0.1-SNAPSHOT.jar"]
FROM openjdk:17-jdk-slim
EXPOSE 8080
COPY build/libs/LearnEngBot-0.0.1-SNAPSHOT.jar LearnEngBot.jar
CMD ["java", "-jar", "LearnEngBot.jar"]


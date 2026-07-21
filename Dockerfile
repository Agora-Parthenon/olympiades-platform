FROM openjdk:17-jdk-slim
WORKDIR /app
COPY . /app
RUN ./mvnw -q -DskipTests package || true
EXPOSE 8080
CMD ["java","-jar","target/*.jar"]

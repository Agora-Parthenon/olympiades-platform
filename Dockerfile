FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY . /app
RUN ./mvnw -q -DskipTests package || true
EXPOSE 8080
CMD ["java","-jar","target/*.jar"]

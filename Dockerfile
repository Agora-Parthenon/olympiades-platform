FROM maven:3.8.8-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . /workspace
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=build /workspace/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]

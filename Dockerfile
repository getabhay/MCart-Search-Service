FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*-exec.jar /app/mcart-product-search-service.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/mcart-product-search-service.jar"]
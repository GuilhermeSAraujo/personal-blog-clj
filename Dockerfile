FROM clojure:temurin-21-lein AS build
WORKDIR /app
COPY project.clj .
RUN lein deps
COPY src src
COPY resources resources
RUN lein uberjar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/uberjar/blog-0.1.0-SNAPSHOT-standalone.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

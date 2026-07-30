# Build the shaded API jar
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY common ./common
COPY domain ./domain
COPY database ./database
COPY api ./api
RUN mvn -pl api -am package -DskipTests -B

# Runtime image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /src/api/target/api-1.0-SNAPSHOT.jar /app/app.jar
# Pre-generated admin reports (committed in git; do not run verify on Render)
COPY --from=build /src/api/published-allure /app/published-allure
COPY --from=build /src/api/published-javadoc /app/published-javadoc
ENV SERVER_HOST=0.0.0.0
ENV JAVA_OPTS="-Xms64m -Xmx384m"
EXPOSE 8090
CMD ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

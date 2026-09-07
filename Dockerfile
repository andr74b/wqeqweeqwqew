# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk-noble AS build

WORKDIR /workspace

# Resolve Java dependencies in a cacheable layer before copying application code.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw \
    && ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

COPY src/ src/
RUN ./mvnw --batch-mode --no-transfer-progress clean package -DskipTests

FROM eclipse-temurin:25-jre-noble AS runtime

WORKDIR /app
COPY --from=build /workspace/target/blackjack.jar ./blackjack.jar

# Render supplies PORT at runtime. The default also makes the image convenient locally.
ENV PORT=10000 \
    SERVER_ADDRESS=0.0.0.0
EXPOSE 10000

# The application only needs read access to its packaged JAR.
USER 10001:10001

CMD ["java", "-jar", "/app/blackjack.jar", "--vaadin.productionMode=true"]

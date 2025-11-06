## Stage 1: Build with Mandrel (native)
FROM quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21 AS build

COPY --chown=quarkus:quarkus --chmod=0755 mvnw /code/mvnw
COPY --chown=quarkus:quarkus .mvn /code/.mvn
COPY --chown=quarkus:quarkus pom.xml /code/
USER quarkus
WORKDIR /code

RUN ./mvnw -B org.apache.maven.plugins:maven-dependency-plugin:3.8.1:go-offline
COPY src /code/src
RUN ./mvnw package -Dnative -DskipTests

## Stage 2: Final minimal image (UBI 9 minimal)
FROM registry.access.redhat.com/ubi9/ubi-minimal

# curl-minimal is already installed, so do NOT install curl
# RUN microdnf install -y curl   <-- remove this

WORKDIR /work/
COPY --from=build /code/target/*-runner /work/application

# Create non-root user
RUN useradd -r -u 1001 quarkus && \
    chmod 775 /work /work/application && \
    chown -R quarkus:quarkus /work

EXPOSE 8080
EXPOSE 9000

USER quarkus

CMD ["./application", "-Dquarkus.http.host=0.0.0.0"]

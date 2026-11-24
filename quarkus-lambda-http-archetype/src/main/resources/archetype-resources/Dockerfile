# Local native build in a container, the CI builds with GraalVM directly (mvn package -Pnative)
FROM quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-25 AS build

COPY --chown=quarkus:quarkus --chmod=0755 mvnw /code/mvnw
COPY --chown=quarkus:quarkus .mvn /code/.mvn
COPY --chown=quarkus:quarkus pom.xml /code/

USER quarkus
WORKDIR /code
RUN ./mvnw -B org.apache.maven.plugins:maven-dependency-plugin:3.8.1:go-offline

COPY src /code/src
RUN ./mvnw -Pnative -DskipTests package

# Same runtime as the Lambda (provided.al2023), which looks for an executable named bootstrap
FROM public.ecr.aws/lambda/provided:al2023 AS runtime

COPY --from=build /code/target/*-runner /var/runtime/bootstrap

RUN chmod +x /var/runtime/bootstrap
CMD [ "bootstrap" ]

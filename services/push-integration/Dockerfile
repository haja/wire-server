# Requires docker >= 17.05 (requires support for multi-stage builds)
FROM maven:3.6.1-jdk-11-slim as build

WORKDIR /home/app
COPY m2-docker /root/.m2/
COPY pom.xml /home/app/

# build all dependencies
RUN mvn dependency:go-offline -B

COPY src src

RUN mvn package

#
# minimize
#
FROM openjdk:11-jre-slim

COPY --from=build /home/app/target/wire-push-integration-0.0.1-SNAPSHOT.war /usr/local/lib/service.war

ENTRYPOINT ["java","-jar","/usr/local/lib/service.war"]

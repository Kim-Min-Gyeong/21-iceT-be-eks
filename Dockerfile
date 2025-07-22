# 1️⃣ 빌드 스테이지: Microsoft OpenJDK 21 (Ubuntu 기반)
FROM mcr.microsoft.com/openjdk/jdk:21-ubuntu AS builder

WORKDIR /build

# Gradle 프로젝트 복사
COPY Koco/ .

# Gradle Wrapper로 빌드 (테스트 제외)
RUN ./gradlew clean build -x test


# 2️⃣ 런타임 스테이지: Microsoft OpenJDK 21 JRE (Ubuntu 기반)
FROM mcr.microsoft.com/openjdk/jdk:21-ubuntu

# Elastic APM Agent 버전 지정
ARG APM_AGENT_VERSION=1.43.0
ENV APM_AGENT_VERSION=${APM_AGENT_VERSION}

# Elastic APM Agent 다운로드
RUN apt-get update && \
    apt-get install -y wget && \
    wget https://search.maven.org/remotecontent?filepath=co/elastic/apm/elastic-apm-agent/${APM_AGENT_VERSION}/elastic-apm-agent-${APM_AGENT_VERSION}.jar -O /opt/elastic-apm-agent.jar

# Spring Boot JAR 복사
WORKDIR /app
COPY --from=builder /build/build/libs/*-SNAPSHOT.jar app.jar

# 컨테이너 포트 오픈
EXPOSE 8080

# APM Agent와 함께 애플리케이션 실행
ENTRYPOINT ["java", \
  "-javaagent:/opt/elastic-apm-agent.jar", \
  "-Delastic.apm.service_name=springboot-prod", \
  "-Delastic.apm.server_urls=http://apm-server.elk.svc.cluster.local:8200", \
  "-Delastic.apm.environment=production", \
  "-Delastic.apm.application_packages=com.koco", \
  "-Delastic.apm.enable_log_correlation=true", \
  "-Delastic.apm.log_level=INFO", \
  "-jar", "app.jar"]

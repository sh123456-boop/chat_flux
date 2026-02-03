# 빌드 스테이지 : gradle + jdk 21 환경
FROM gradle:8.10.2-jdk21 AS build

# 작업 디렉토리 설정
WORKDIR /app

# gradle 래퍼 및 설정 파일 복사 (의존성 캐시 최적화용)
COPY gradlew gradlew.bat build.gradle settings.gradle gradle /app/

# wrapper 누락 시 빌드가 실패하므로 명시적으로 한 번 더 복사
COPY gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.properties /app/gradle/wrapper/

# gradle 동작 확인 및 캐시 워밍업
RUN ./gradlew --no-daemon --version

# 소스 코드 복사
COPY src /app/src

# 애플리케이션 빌드
RUN ./gradlew --no-daemon clean build -x test


# 런타임 스테이지 : JRE 21만 포함해 이미지 슬림하게
FROM eclipse-temurin:21-jre

# 실행 디렉토리 설정
WORKDIR /app

# 빌드된 jar 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 컨테이너가 열 포트 선언
EXPOSE 8081

# 애플리케이션 실행 명령
ENTRYPOINT ["java", "-jar", "app.jar"]

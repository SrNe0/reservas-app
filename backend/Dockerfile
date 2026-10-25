# Etapa 1: compilar con JDK y Maven Wrapper
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Primero solo lo que define las dependencias, para que Docker cachee esta capa
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline

COPY src ./src
RUN ./mvnw -q -DskipTests package

# Etapa 2: imagen final solo con JRE y el .jar
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /app/target/*.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

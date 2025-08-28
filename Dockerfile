# Используем базовый образ OpenJDK 17 (slim-вариант для минимального размера)
FROM openjdk:17-jdk-slim

# Устанавливаем рабочую директорию внутри контейнера
WORKDIR /app

# Копируем скомпилированный JAR-файл из Maven в контейнер
COPY target/ZebraPRJ-0.0.1-SNAPSHOT.jar app.jar

# Указываем порт, который будет использовать приложение
EXPOSE 8081
EXPOSE 9090

# Команда для запуска Spring Boot приложения
ENTRYPOINT ["java", "-jar", "app.jar"]
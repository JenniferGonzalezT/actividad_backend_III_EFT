# Imagen ligera de Java 17
FROM eclipse-temurin:17-jdk-jammy

# Directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiar el archivo JAR compilado al contenedor
COPY target/*.jar app.jar

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]

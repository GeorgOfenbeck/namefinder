FROM openjdk:21-jdk-alpine
WORKDIR /app
COPY . /app
RUN ./mill -i app.assembly
ENTRYPOINT ["java", "-jar", "out/app/assembly/dest/out.jar"]
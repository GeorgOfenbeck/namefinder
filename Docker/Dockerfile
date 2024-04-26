FROM eclipse-temurin
WORKDIR /app
COPY . /app
RUN ./mill -i namefinder.assembly
ENTRYPOINT ["java", "-jar", "out/namefinder/assembly/dest/out.jar"]
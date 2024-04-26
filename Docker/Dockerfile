FROM eclipse-temurin
WORKDIR /app
COPY . /app
RUN ls -la /app
RUN /app/mill -i namefinder.assembly
ENTRYPOINT ["java", "-jar", "/app/out/namefinder/assembly.dest/out.jar"]

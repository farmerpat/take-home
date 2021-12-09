FROM openjdk:8-alpine

COPY target/uberjar/take-home.jar /take-home/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/take-home/app.jar"]

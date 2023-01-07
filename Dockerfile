FROM openjdk:17-alpine

WORKDIR /app

### Set entrypoint to script running Java application
#ENTRYPOINT ["java -jar app.jar"]

# tzdata for timzone
RUN apk add tzdata

# timezone env with default
ENV TZ Europe/Paris

### Add Java application artifact
COPY build/libs/*.jar ./app.jar

CMD ["java", "-cp", "app.jar", "fr.enimaloc.esportlinebot.ESportLineBot"]
FROM java:8

EXPOSE 8082
EXPOSE 8080

ADD target/eve-vertx-micro-1.0-SNAPSHOT-fat.jar app.jar
ADD src/config/config.json src/config/config.json

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -jar app.jar
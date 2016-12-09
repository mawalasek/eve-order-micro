FROM java:8

WORKDIR /home/
# Make sure that the service's port is 8080
EXPOSE 8080

ADD target/eve-vertx-micro-1.0-SNAPSHOT-fat.jar app.jar

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -jar app.jar
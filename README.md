# EVE order micro
A Vertx-based microservice for querying the order database.

## How to Configure
Specify the HTTP port and MongoDB connection details in `src/config/config.json`. 

## How To Run
Build fat JAR via 

`mvn clean package`

Then, issue the following Docker commands to run it in a container

`docker build . -t myimage`

`docker run -p 8080:8080 myimage`

If not using Docker, you can run it standalone via

`java -jar <JAR-NAME>.jar`

## How To Push

`docker build . -t myimage`

`docker push myimage`

package de.bahr;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import java.util.List;

public class OrderVerticle extends AbstractVerticle {

    private Logger logger;

    @Override
    public void start(Future<Void> future) {

        BasicConfigurator.configure();
        logger = Logger.getLogger(this.getClass().getName());

        logger.debug("Initialising Mongo client. Config: " + config());
        MongoClient mongoClient = MongoClient.createShared(vertx, config());

        Router router = Router.router(vertx);

        router.route("/orders").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.exceptionHandler(routingContext::fail);

            mongoClient.find("order", new JsonObject(), results -> {
                    List<JsonObject> jsonOrders = results.result();
                    response.putHeader("content-type", "application/json")
                            .end(Json.encodePrettily(jsonOrders));
                });
            });

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(config().getInteger("http.port", 8080), result -> {
                    if (result.succeeded()) {
                        future.complete();
                    } else {
                        future.fail(result.cause());
                    }
                });
    }
}

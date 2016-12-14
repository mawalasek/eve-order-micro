package de.bahr;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class OrderVerticle extends AbstractVerticle {

    private MongoClient mongoClient;
    private Logger logger;
    private Router router;

    @Override
    public void start(Future<Void> future) {

        initLogger();
        initMongoClient();
        initRouter();
        initHttpServer(future);
    }

    private void initLogger() {
        BasicConfigurator.configure();
        logger = Logger.getLogger(this.getClass().getName());
    }

    private void initMongoClient() {
        logger.debug("Initialising Mongo client. Config: " + config());
        mongoClient = MongoClient.createShared(vertx, config());
    }

    private void initRouter() {
        router = Router.router(vertx);
        router.get("/orders").handler(this::handleGetAllOrders);
        router.get("/orders/month/:year/:month/:client").handler(this::handleGetMonthlyAggregatePrice);
    }

    private void handleGetAllOrders(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        response.exceptionHandler(routingContext::fail);

        mongoClient.find("order", new JsonObject(), results -> {
            List<JsonObject> jsonOrders = results.result();
            response.putHeader("content-type", "application/json")
                    .end(Json.encodePrettily(jsonOrders));
        });
    }

    private void handleGetMonthlyAggregatePrice(RoutingContext routingContext) {
        String client = routingContext.request().getParam("client");
        int year = Integer.valueOf(routingContext.request().getParam("year"));
        int month = Integer.valueOf(routingContext.request().getParam("month"));

        HttpServerResponse response = routingContext.response();
        response.exceptionHandler(routingContext::fail);

        getMonthlyAggregatePriceFromDb(client, year, month, as -> {
            response.putHeader("content-type", "application/json")
                    .end(Json.encodePrettily(as.result()));
        });
    }

    private void getMonthlyAggregatePriceFromDb(String client, int year, int month, Handler<AsyncResult<Integer>> handler) {
        JsonObject query = new JsonObject()
                .put("client", client)
                .put("status", "contracted")
                .put("expectedPrice", new JsonObject().put("$gt", 0))
                .put("completed", new JsonObject().put("$exists", true));

        mongoClient.find("order", query, results -> {
            List<JsonObject> orders = results.result();
            int sum = orders.stream()
                    .filter(order -> orderCompletedDateMatches(order, year, month))
                    .mapToInt(this::getOrderExpectedPrice)
                    .sum();
            handler.handle(Future.succeededFuture(sum));
        });
    }

    private boolean orderCompletedDateMatches(JsonObject order, int year, int month) {
        String dateStr = order.getJsonObject("completed").getString("$date");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = new GregorianCalendar();
        try {
            Date date = df.parse(dateStr);
            cal.setTime(date);
            return cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) +1 == month;
        } catch (ParseException e) {
            logger.error("Could not parse date string: " + dateStr);
            return false;
        }
    }

    private int getOrderExpectedPrice(JsonObject order) {
        return order.getInteger("expectedPrice");
    }

    private void initHttpServer(Future<Void> future) {
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

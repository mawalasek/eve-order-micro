package de.bahr;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(VertxUnitRunner.class)
public class OrderVerticleTest {

    public static final int MONGO_PORT = 12345;
    public static final int VERTICLE_PORT = 8081;
    public static final String MONGO_DB_NAME = "eve";
    private static final String ORDERS = "order";

    private Vertx vertx;
    private static MongodProcess mongodProcess;
    private MongoClient mongoClient;

    @Before
    public void setUp(TestContext testContext) throws Exception {
        setupVertx();
        setupInMemoryMongoService();
        setupMongoClient();
        setupVerticle(testContext);
    }

    private void setupVertx() {
        vertx = Vertx.vertx();
    }

    private void setupInMemoryMongoService() throws IOException {
        MongodStarter mongodStarter = MongodStarter.getDefaultInstance();
        IMongodConfig mongodConfig = new MongodConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .net(new Net(MONGO_PORT, Network.localhostIsIPv6()))
                .build();
        MongodExecutable mongodExecutable = mongodStarter.prepare(mongodConfig);
        mongodProcess = mongodExecutable.start();
    }

    private void setupMongoClient() {
        mongoClient = MongoClient.createNonShared(vertx, testConfig());
    }

    private void setupVerticle(TestContext testContext) {
        DeploymentOptions deploymentOptions = new DeploymentOptions().setConfig(testConfig());
        vertx.deployVerticle(OrderVerticle.class.getName(), deploymentOptions, testContext.asyncAssertSuccess());
    }

    private JsonObject testConfig() {
        return new JsonObject()
                .put("http.port", VERTICLE_PORT)
                .put("db_name", MONGO_DB_NAME)
                .put("connection_string", "mongodb://localhost:" + MONGO_PORT);
    }

    @After
    public void tearDown(TestContext testContext) throws Exception {
        mongodProcess.stop();
        vertx.close(testContext.asyncAssertSuccess());
    }

    @Test
    public void testGetAllOrders_empty(TestContext testContext) throws Exception {
        final Async async = testContext.async();

        vertx.createHttpClient().getNow(VERTICLE_PORT, "localhost", "/orders", response -> {
                    testContext.assertTrue(response.statusCode() == 200);
                    response.bodyHandler(body -> {
                        testContext.assertTrue(body != null);
                        testContext.assertEquals(body.toJsonArray().size(), 0);
                    });
                    async.complete();
                }
        );
    }

    @Test
    public void testGetAllOrders_threeEntries(TestContext testContext) {
        final Async async = testContext.async();

        JsonObject order1 = new JsonObject()
                .put("client", "John Doe")
                .put("items", new JsonArray()
                        .add(new JsonObject()
                                .put("name", "Item1"))
                        .add(new JsonObject()
                                .put("name", "Item2")));

        JsonObject order2 = new JsonObject()
                .put("client", "Foo Bar")
                .put("items", new JsonArray()
                        .add(new JsonObject()
                                .put("name", "Item3"))
                        .add(new JsonObject()
                                .put("name", "Item4"))
                        .add(new JsonObject()
                                .put("name", "Item5")));

        JsonObject order3 = new JsonObject()
                .put("client", "Hello World")
                .put("items", new JsonArray());

        Future<String> future1 = Future.future();
        Future<String> future2 = Future.future();
        Future<String> future3 = Future.future();

        mongoClient.insert(ORDERS, order1, future1.completer())
                .insert(ORDERS, order2, future2.completer())
                .insert(ORDERS, order3, future3.completer());

        CompositeFuture.all(future1, future2, future3).setHandler(event ->
                vertx.createHttpClient().getNow(VERTICLE_PORT, "localhost", "/orders", response -> {
                    testContext.assertTrue(response.statusCode() == 200);
                    response.bodyHandler(body -> {
                        testContext.assertEquals(body.toJsonArray().size(), 3);
                    });
                    async.complete();
                }));
    }

    @Test
    public void testGetMonthlyValueByClient(TestContext testContext) {
        final Async async = testContext.async();

        Map<JsonObject, Future> ordersAndFutures = insertOrders(createTestOrders());

        CompositeFuture.all(ordersAndFutures.values().stream().collect(Collectors.toList())).setHandler(event ->
                vertx.createHttpClient().getNow(VERTICLE_PORT, "localhost", "/orders/month/2016/8?client=John%20Doe", response -> {
                    testContext.assertTrue(response.statusCode() == 200);
                    response.bodyHandler(body -> testContext.assertEquals(body.toString(), "579"));
                    async.complete();
                })
        );
    }

    @Test
    public void testGetMonthlyValueByAssignee(TestContext testContext) {
        final Async async = testContext.async();

        Map<JsonObject, Future> ordersAndFutures = insertOrders(createTestOrders());

        CompositeFuture.all(ordersAndFutures.values().stream().collect(Collectors.toList())).setHandler(event ->
                vertx.createHttpClient().getNow(VERTICLE_PORT, "localhost", "/orders/month/2016/8?assignee=service", response -> {
                    testContext.assertTrue(response.statusCode() == 200);
                    response.bodyHandler(body -> testContext.assertEquals(body.toString(), "912"));
                    async.complete();
                })
        );
    }

    @Test
    public void testGetMonthlyValueByAssigneeAndClient(TestContext testContext) {
        final Async async = testContext.async();

        Map<JsonObject, Future> ordersAndFutures = insertOrders(createTestOrders());

        CompositeFuture.all(ordersAndFutures.values().stream().collect(Collectors.toList())).setHandler(event ->
                vertx.createHttpClient().getNow(VERTICLE_PORT, "localhost", "/orders/month/2016/8?assignee=service&client=John%20Doe", response -> {
                    testContext.assertTrue(response.statusCode() == 200);
                    response.bodyHandler(body -> testContext.assertEquals(body.toString(), "123"));
                    async.complete();
                })
        );
    }

    @Test
    public void testGetMonthlyValueAll(TestContext testContext) {
        final Async async = testContext.async();

        Map<JsonObject, Future> ordersAndFutures = insertOrders(createTestOrders());

        CompositeFuture.all(ordersAndFutures.values().stream().collect(Collectors.toList())).setHandler(event ->
                vertx.createHttpClient().getNow(VERTICLE_PORT, "localhost", "/orders/month/2016/8", response -> {
                    testContext.assertTrue(response.statusCode() == 200);
                    response.bodyHandler(body -> testContext.assertEquals(body.toString(), "1368"));
                    async.complete();
                })
        );
    }

    /**
     * Inserts the given list of test order data into the database.
     * @param orders the data to be inserted
     * @return a map mapping each order to a future which completes once the the order
     * is successfully inserted into the database. This facilitates the use of CompositeFuture.all()
     * in the tests, i.e. waiting for all orders to be inserted before performing the respective test query.
     */
    private Map<JsonObject, Future> insertOrders(List<JsonObject> orders) {
        Map<JsonObject, Future> ordersAndFutures = new HashMap<>();

        orders.forEach(order -> {
            Future orderInsertedFuture = Future.future();
            ordersAndFutures.put(order, orderInsertedFuture);
            mongoClient.insert(ORDERS, order, orderInsertedFuture.completer());
        });

        return ordersAndFutures;
    }

    /**
     * Creates an list of orders for testing purposes.
     * @return
     */
    private static List<JsonObject> createTestOrders() {
        List<JsonObject> orders = new ArrayList<>();

        orders.add(new JsonObject()
                .put("client", "John Doe")
                .put("assignee", "service")
                .put("status", "contracted")
                .put("expectedPrice", 123)
                .put("completed", new JsonObject().put("$date", "2016-08-30T23:59:59.999Z")));

        orders.add(new JsonObject()
                .put("client", "John Doe")
                .put("status", "contracted")
                .put("expectedPrice", 456)
                .put("completed", new JsonObject().put("$date", "2016-08-01T00:00:00.000Z")));

        orders.add(new JsonObject()
                .put("client", "John Doe")
                .put("assignee", "service")
                .put("status", "contracted")
                .put("expectedPrice", 789)
                .put("completed", new JsonObject().put("$date", "2016-07-31T23:59:59.999Z")));

        orders.add(new JsonObject()
                .put("client", "John Doe")
                .put("assignee", "service")
                .put("status", "contracted")
                .put("expectedPrice", 456));

        orders.add(new JsonObject()
                .put("client", "Alice Doe")
                .put("assignee", "service")
                .put("status", "contracted")
                .put("expectedPrice", 789)
                .put("completed", new JsonObject().put("$date", "2016-08-30T23:59:59.999Z")));

        orders.add(new JsonObject()
                .put("client", "John Doe")
                .put("assignee", "service")
                .put("status", "not_contracted")
                .put("expectedPrice", 789)
                .put("completed", new JsonObject().put("$date", "2016-08-30T23:59:59.999Z")));

        orders.add(new JsonObject()
                .put("client", "John Doe")
                .put("assignee", "service")
                .put("status", "contracted")
                .put("expectedPrice", 0)
                .put("completed", new JsonObject().put("$date", "2016-08-30T23:59:59.999Z")));

        return orders;
    }
}

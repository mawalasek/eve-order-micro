package de.bahr;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(VertxUnitRunner.class)
public class OrderVerticleTest {

    public static final int MONGO_PORT = 12345;
    public static final int VERTICLE_PORT = 8081;
    public static final String MONGO_DB_NAME = "eve";

    private Vertx vertx;
    private static MongodProcess mongodProcess;

    @Before
    public void setUp(TestContext testContext) throws Exception {
        setupMongo();
        setupVerticle(testContext);
    }

    private void setupMongo() throws IOException {
        MongodStarter mongodStarter = MongodStarter.getDefaultInstance();
        IMongodConfig mongodConfig = new MongodConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .net(new Net(MONGO_PORT, Network.localhostIsIPv6()))
                .build();
        MongodExecutable mongodExecutable = mongodStarter.prepare(mongodConfig);
        mongodProcess = mongodExecutable.start();
    }

    private void setupVerticle(TestContext testContext) {
        vertx = Vertx.vertx();

        JsonObject testConfig = new JsonObject()
                .put("http.port", VERTICLE_PORT)
                .put("db_name", MONGO_DB_NAME)
                .put("connection_string", "mongodb://localhost:" + MONGO_PORT);
        DeploymentOptions deploymentOptions = new DeploymentOptions().setConfig(testConfig);

        vertx.deployVerticle(OrderVerticle.class.getName(), deploymentOptions, testContext.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext testContext) throws Exception {
        mongodProcess.stop();
        vertx.close(testContext.asyncAssertSuccess());
    }

    @Test
    public void testOrderVerticle(TestContext testContext) throws Exception {
        final Async async = testContext.async();

        vertx.createHttpClient().getNow(VERTICLE_PORT, "localhost", "/orders", response -> {
                    testContext.assertTrue(response.statusCode() == 200);
                    response.bodyHandler(body -> {
                        testContext.assertTrue(body != null);
                    });
                    async.complete();
                }
        );
    }
}

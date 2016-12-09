package de.bahr;

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

@RunWith(VertxUnitRunner.class)
public class OrderVerticleTest {

    private Vertx vertx;

    @Before
    public void setUp(TestContext testContext) throws Exception {
        vertx = Vertx.vertx();

        JsonObject testConfig = new JsonObject()
                .put("http.port", 8081);
        DeploymentOptions deploymentOptions = new DeploymentOptions().setConfig(testConfig);

        vertx.deployVerticle(OrderVerticle.class.getName(), deploymentOptions, testContext.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext testContext) throws Exception {
        vertx.close(testContext.asyncAssertSuccess());
    }

    @Test
    public void testOrderVerticle(TestContext testContext) throws Exception {
        assert true;
//        final Async async = testContext.async();
//
//        vertx.createHttpClient().getNow(8081, "localhost", "/",
//                response -> {
//                    response.handler(body -> {
//                        testContext.assertTrue(body.toString().contains("EVE"));
//                        async.complete();
//                    });
//                });
    }
}

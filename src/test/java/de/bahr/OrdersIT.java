package de.bahr;

import com.jayway.restassured.RestAssured;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.get;

public class OrdersIT {

    @BeforeClass
    public static void configureRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = Integer.getInteger("http.port", 8081);
    }

    @Test
    public void ITTest() {
        get("/orders").then()
                .assertThat()
                .statusCode(200);
    }

    @AfterClass
    public static void unconfigureRestAssured() {
        RestAssured.reset();
    }
}

package com.example.ZebraPRJ;

import io.restassured.RestAssured;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class ZebraPrjControllerTest {

    @BeforeClass
    public void setUp(){
        //Указываем базовый URL для тестов
        RestAssured.baseURI = "http://localhost:8081";
    }

    @Test (description = "Check GET /hello returns 200 and 'Hello'",
            groups = "Positive")
    public void testGetHelloSuccess(){
        given()
                .when()
                .get("/hello")
                .then()
                .statusCode(200)
                .body(equalTo("Hello"));
    }

    @Test (description = "Check Content-Type for GET /hello",
            groups = "Positive")
    public void testGetHelloContentType(){
        given()
                .when()
                .get("/hello")
                .then()
                .statusCode(200)
                .header("Content-Type", "text/plain;charset=UTF-8");
    }

    @Test (description = "Check GET /invalid returns 404",
            groups = "Negative")
    public void testGetInvalidEndpoint(){
        given()
                .when()
                .get("/invalid")
                .then()
                .statusCode(404);
    }


}

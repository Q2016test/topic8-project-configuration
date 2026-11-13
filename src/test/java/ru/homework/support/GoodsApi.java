package ru.homework.support;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;
import ru.homework.config.TestConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.preemptive;
import static org.hamcrest.Matchers.*;

public final class GoodsApi {
    private final TestConfig config;
    private final List<Long> createdIds = new ArrayList<>();

    public GoodsApi(TestConfig config) {
        this.config = config;
    }

    private RequestSpecification specification() {
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(config.apiUrl())
                .setContentType(ContentType.JSON)
                .setAuth(preemptive().basic(config.admin().login(), config.admin().password()));
        if (config.loggingMode() == TestConfig.LoggingMode.DEBUG) {
            builder.addFilter(new RequestLoggingFilter(LogDetail.URI));
        }
        if (config.loggingMode() != TestConfig.LoggingMode.OFF) {
            builder.addFilter(new ResponseLoggingFilter(LogDetail.STATUS));
        }
        return builder.build();
    }

    public Product createProduct() {
        String name = config.productName();
        long id = given().spec(specification()).body(Map.of("name", name, "price", config.productPrice()))
                .when().post("/goods/add")
                .then().statusCode(200).body("data.id", notNullValue())
                .extract().jsonPath().getLong("data.id");
        createdIds.add(id);
        return new Product(id, name);
    }

    public void cleanup() {
        for (long id : createdIds) {
            given().spec(specification()).when().delete("/goods/{id}", id)
                    .then().statusCode(anyOf(is(200), is(404)));
        }
        createdIds.clear();
    }

    public record Product(long id, String name) {
    }
}

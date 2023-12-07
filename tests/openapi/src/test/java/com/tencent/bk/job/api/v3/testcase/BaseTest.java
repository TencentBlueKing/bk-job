package com.tencent.bk.job.api.v3.testcase;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import org.junit.jupiter.api.BeforeAll;

import java.text.SimpleDateFormat;

abstract class BaseTest {

    @BeforeAll
    static void setUp() {
        RestAssured.config = RestAssuredConfig.config().objectMapperConfig(
            new ObjectMapperConfig().jackson2ObjectMapperFactory(
                (cls, charset) -> {
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                    objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
                    objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
                    objectMapper.registerModule(new JodaModule());
                    objectMapper.findAndRegisterModules();
                    return objectMapper;
                }
            )
        );
    }

}

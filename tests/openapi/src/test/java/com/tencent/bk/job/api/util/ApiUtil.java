package com.tencent.bk.job.api.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.exception.TestInitialException;
import com.tencent.bk.job.api.props.TestProps;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.security.PrivateKey;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.config;
import static io.restassured.config.LogConfig.logConfig;
import static org.hamcrest.Matchers.equalTo;

public class ApiUtil {
    /**
     * API JWT, 用于Job 网关请求认证
     */
    public static final String HEADER_BK_API_JWT = "X-Bkapi-JWT";
    /**
     * API 测试标志 Header, 用于 Job 网关识别测试请求，并进行特殊处理
     */
    public static final String HEADER_JOB_OPENAPI_TEST = "X-JOB-OPENAPI-TEST";
    /**
     * 语言
     */
    public static final String HEADER_BK_LANG = "Blueking-Language";

    /**
     * 请求通用设置
     *
     * @param username 用户名
     */
    public static RequestSpecification requestSpec(String username) {
        RequestSpecBuilder builder = new RequestSpecBuilder();
        builder.addHeaders(buildRequestHeaders(username));
        builder.setBaseUri(TestProps.API_HOST_BASE_URL);
        builder.setContentType(ContentType.JSON);

        if (TestProps.IS_PROXY_ENABLE && StringUtils.isNotBlank(TestProps.HTTP_PROXY)
            && TestProps.HTTP_PROXY_PORT != null) {
            builder.setProxy(TestProps.HTTP_PROXY, TestProps.HTTP_PROXY_PORT);
        }
        RequestSpecification requestSpecification = builder.build();
        requestSpecification.config(config().logConfig(
            logConfig()
                .blacklistHeaders(Collections.singleton(HEADER_BK_API_JWT))
                .enableLoggingOfRequestAndResponseIfValidationFails()
                .enablePrettyPrinting(true)
        ));
        return requestSpecification;
    }

    /**
     * 成功响应通用设置
     */
    public static ResponseSpecification successResponseSpec() {
        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("result", equalTo(true));
        builder.expectBody("code", equalTo(ErrorCode.RESULT_OK));
        return builder.build();
    }

    /**
     * 失败响应通用设置
     */
    public static ResponseSpecification failResponseSpec(Integer code) {
        ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectStatusCode(200);
        builder.expectBody("result", equalTo(false));
        builder.expectBody("code", equalTo(code));
        return builder.build();
    }

    private static Map<String, String> buildRequestHeaders(String username) {
        Map<String, String> headers = new HashMap<>();
        headers.put(HEADER_BK_API_JWT, buildEsbJwt(TestProps.APP_CODE, username));
        // API 测试标志 flag
        headers.put(HEADER_JOB_OPENAPI_TEST, "true");
        headers.put(HEADER_BK_LANG, "zh-cn");
        return headers;
    }

    private static String buildEsbJwt(String appCode, String username) {
        try {
            PrivateKey privateKey = RSAUtils.getPrivateKey(TestProps.ESB_PRIVATE_KEY_BASE64);

            long nowMillis = System.currentTimeMillis();//生成JWT的时间
            Date now = new Date(nowMillis);
            Map<String, Object> claims = new HashMap<>();
            ApiUtil.App app = new ApiUtil.App();
            app.setAppCode(appCode);
            app.setVerified(true);
            claims.put("app", app);
            ApiUtil.User user = new ApiUtil.User();
            user.setUsername(username);
            claims.put("user", user);
            JwtBuilder builder = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setIssuer("APIGW")
                .signWith(SignatureAlgorithm.RS512, privateKey);
            long expMillis = nowMillis + 9_000_000_000L;
            Date exp = new Date(expMillis);
            builder.setExpiration(exp);

            return builder.compact();
        } catch (Exception e) {
            throw new TestInitialException(e);
        }

    }


    @Data
    private static class App {
        @JsonProperty("app_code")
        private String appCode;
        private boolean verified;
    }

    @Data
    private static class User {
        private String username;
    }

}

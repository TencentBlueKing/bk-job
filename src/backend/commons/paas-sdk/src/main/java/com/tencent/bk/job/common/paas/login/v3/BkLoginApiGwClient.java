package com.tencent.bk.job.common.paas.login.v3;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.HttpMethodEnum;
import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.BkApiGatewayProperties;
import com.tencent.bk.job.common.esb.exception.BkOpenApiException;
import com.tencent.bk.job.common.esb.model.BkApiAuthorization;
import com.tencent.bk.job.common.esb.model.OpenApiRequestInfo;
import com.tencent.bk.job.common.esb.model.OpenApiResponse;
import com.tencent.bk.job.common.esb.sdk.BkApiV2Client;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import com.tencent.bk.job.common.util.http.HttpMetricUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.http.message.BasicHeader;

import java.util.function.Function;

import static com.tencent.bk.job.common.metrics.CommonMetricNames.BK_LOGIN_API;
import static com.tencent.bk.job.common.metrics.CommonMetricNames.BK_LOGIN_API_HTTP;

/**
 * bk-login APIGW 客户端
 */
@Slf4j
public class BkLoginApiGwClient extends BkApiV2Client {

    public static final String API_URL_USERINFO = "/login/api/v3/open/bk-tokens/userinfo";

    protected final BkApiAuthorization authorization;


    public BkLoginApiGwClient(BkApiGatewayProperties bkApiGatewayProperties,
                              AppProperties appProperties,
                              MeterRegistry meterRegistry,
                              TenantEnvService tenantEnvService) {
        super(
            meterRegistry,
            BK_LOGIN_API,
            bkApiGatewayProperties.getBkLogin().getUrl(),
            HttpHelperFactory.createHttpHelper(
                httpClientBuilder -> httpClientBuilder.addInterceptorLast(getLogBkApiRequestIdInterceptor())
            ),
            tenantEnvService
        );
        this.authorization = BkApiAuthorization.appAuthorization(
            appProperties.getCode(), appProperties.getSecret());
    }


    /**
     * 根据 token 获取用户信息
     *
     * @param token 用户登录 token
     * @return 用户信息
     */
    public OpenApiBkUser getBkUserByToken(String token) {
        OpenApiResponse<OpenApiBkUser> response = requestBkLoginApi(
            "get_bk_token_userinfo",
            OpenApiRequestInfo
                .builder()
                .method(HttpMethodEnum.GET)
                .uri(API_URL_USERINFO)
                .queryParams("bk_token=" + token)
                .body(null)
                // 该API 与租户无关，写死租户 ID 为 system以便通过蓝鲸网关验证
                .addHeader(new BasicHeader(JobCommonHeaders.BK_TENANT_ID, "system"))
                .authorization(authorization)
                .build(),
            request -> {
                try {
                    return doRequest(request, new TypeReference<OpenApiResponse<OpenApiBkUser>>() {
                    });
                } catch (BkOpenApiException e) {
                    if (e.getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
                        // http status = 400 表示登录态已过期
                        return OpenApiResponse.success(null);
                    } else {
                        throw e;
                    }
                }
            }
        );

        return response.getData();
    }


    protected <T, R> OpenApiResponse<R> requestBkLoginApi(
        String apiName,
        OpenApiRequestInfo<T> request,
        Function<OpenApiRequestInfo<T>, OpenApiResponse<R>> requestHandler) {

        try {
            HttpMetricUtil.setHttpMetricName(BK_LOGIN_API_HTTP);
            HttpMetricUtil.addTagForCurrentMetric(Tag.of("api_name", apiName));
            return requestHandler.apply(request);
        } catch (Throwable e) {
            String errorMsg = "Fail to request bk-login api|method=" + request.getMethod()
                + "|uri=" + request.getUri() + "|queryParams="
                + request.getQueryParams() + "|body="
                + JsonUtils.toJsonWithoutSkippedFields(JsonUtils.toJsonWithoutSkippedFields(request.getBody()));
            log.error(errorMsg, e);
            throw new InternalException(e.getMessage(), e, ErrorCode.BK_LOGIN_API_ERROR);
        } finally {
            HttpMetricUtil.clearHttpMetric();
        }
    }

}

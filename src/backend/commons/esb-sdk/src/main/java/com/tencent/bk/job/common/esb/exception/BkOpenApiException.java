package com.tencent.bk.job.common.esb.exception;

import com.tencent.bk.job.common.esb.model.OpenApiError;
import com.tencent.bk.job.common.esb.model.OpenApiV1Error;
import lombok.Getter;

/**
 * 蓝鲸 Open API 调用异常
 */
@Getter
public class BkOpenApiException extends RuntimeException {

    /**
     * http 状态码
     */
    private final int statusCode;

    /**
     * 蓝鲸 v2(新版）错误信息
     */
    private OpenApiError error;

    /**
     * 蓝鲸 v1 版本错误信息
     */
    private OpenApiV1Error openApiV1Error;


    public BkOpenApiException(int statusCode) {
        this.statusCode = statusCode;
    }

    public BkOpenApiException(int statusCode, OpenApiError error) {
        this.statusCode = statusCode;
        this.error = error;
    }

    public BkOpenApiException(int statusCode, OpenApiV1Error openApiV1Error) {
        this.statusCode = statusCode;
        this.openApiV1Error = openApiV1Error;
    }

}

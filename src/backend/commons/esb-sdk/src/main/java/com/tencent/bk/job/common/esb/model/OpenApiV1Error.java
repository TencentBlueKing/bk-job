package com.tencent.bk.job.common.esb.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tencent.bk.job.common.esb.constants.BkErrorCodeEnum;
import com.tencent.bk.job.common.esb.model.iam.OpenApiApplyPermissionDTO;
import lombok.Data;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 蓝鲸旧版 http open api 协议定义的标准响应
 */
@Data
public class OpenApiV1Error {

    /**
     * 错误码
     * @see com.tencent.bk.job.common.constant.ErrorCode
     */
    private Integer code;

    /**
     * 给用户看到的错误说明
     */
    private String message;
}

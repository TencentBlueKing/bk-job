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
 * 蓝鲸新版 http open api 协议定义的标准响应 - 错误信息
 */
@Data
@JsonDeserialize(using = OpenApiError.OpenApiErrorDeserializer.class)
public class OpenApiError {

    /**
     * 语义化的错误英文标识, 整个蓝鲸会定义一套通用的错误大分类; 作用: 上游编码基于这个做代码层面的逻辑判断(所以必须是确定的枚举);
     *
     * @see BkErrorCodeEnum
     */
    private String code;

    /**
     * 给用户看到的错误说明, 需要支持国际化
     */
    private String message;

    /**
     * 返回的数据用于给调用方针对这个code做相应的一些处理, 例如无权限返回申请权限信息, 登录认证失败返回跳转url等
     */
    private Object data;

    /**
     * 错误详情
     */
    private List<Map<String, Object>> details;

    /**
     * OpenApiError 自定义反序列化
     */
    static class OpenApiErrorDeserializer extends JsonDeserializer<OpenApiError> {
        @Override
        public OpenApiError deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            JsonNode node = mapper.readTree(p);

            OpenApiError error = new OpenApiError();
            error.setCode(node.get("code").asText());
            error.setMessage(node.get("message").asText());
            List<Map<String, Object>> details = mapper.convertValue(
                node.get("details"),
                mapper.getTypeFactory().constructCollectionType(List.class, Map.class)
            );
            error.setDetails(details);

            // 根据 code 字段的值来处理 data 字段
            String code = error.getCode();
            if (BkErrorCodeEnum.IAM_NO_PERMISSION.getErrorCode().equals(code)) {
                error.setData(mapper.treeToValue(node.get("data"), OpenApiApplyPermissionDTO.class));
            } else {
                // 处理其他类型的 data，蓝鲸 API 规范暂未定义具体的 schema，所以直接设置为原始的 JsonNode
                error.setData(node.get("data"));
            }

            return error;
        }
    }
}

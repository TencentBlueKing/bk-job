package com.tencent.bk.job.common.esb.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.common.esb.model.iam.OpenApiApplyPermissionDTO;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 蓝鲸新版 http open api 协议定义的标准响应
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Slf4j
public class OpenApiResponse<T> {

    private T data;

    private OpenApiError error;

    /**
     * 无权限返回数据
     */
    private OpenApiApplyPermissionDTO permission;

    private OpenApiResponse(T data) {
        this.data = data;
    }


    public static <T> OpenApiResponse<T> success(T data) {
        return new OpenApiResponse<>(data);
    }

}

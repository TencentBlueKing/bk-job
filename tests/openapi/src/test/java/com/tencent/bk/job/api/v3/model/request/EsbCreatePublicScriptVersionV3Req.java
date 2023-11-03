package com.tencent.bk.job.api.v3.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 创建公共脚本版本请求
 */
@Data
public class EsbCreatePublicScriptVersionV3Req {
    /**
     * 脚本ID
     */
    @JsonProperty("script_id")
    private String scriptId;

    /**
     * 脚本内容，需Base64编码
     */
    private String content;

    /**
     * 脚本版本
     */
    private String version;

    /**
     * 版本描述
     */
    @JsonProperty("version_desc")
    private String versionDesc;
}

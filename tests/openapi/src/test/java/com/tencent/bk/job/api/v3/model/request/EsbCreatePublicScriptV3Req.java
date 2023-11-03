package com.tencent.bk.job.api.v3.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 创建公共脚本请求
 */
@Data
public class EsbCreatePublicScriptV3Req{
    /**
     * 脚本名称
     */
   private String name;

    /**
     * 脚本描述
     */
    private String description;

    @JsonProperty("script_language")
    private Integer type;

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

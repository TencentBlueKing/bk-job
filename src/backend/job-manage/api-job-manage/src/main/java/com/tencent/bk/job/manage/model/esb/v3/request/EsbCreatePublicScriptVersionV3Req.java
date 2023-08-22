package com.tencent.bk.job.manage.model.esb.v3.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbJobReq;
import com.tencent.bk.job.common.validation.Create;
import com.tencent.bk.job.common.validation.NotBlankField;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Pattern;

/**
 * 创建公共脚本版本请求
 */
@Data
@ApiModel("创建公共脚本版本请求报文")
@EqualsAndHashCode(callSuper = true)
public class EsbCreatePublicScriptVersionV3Req extends EsbJobReq {
    /**
     * 脚本ID
     */
    @NotBlankField(fieldName = "name", groups = Create.class)
    @JsonProperty("script_id")
    private String scriptId;

    /**
     * 脚本内容，需Base64编码
     */
    @NotBlankField(fieldName = "content", groups = Create.class)
    private String content;

    /**
     * 脚本版本
     */
    @NotBlankField(fieldName = "version", groups = Create.class)
    @Length(max = 60, message = "{validation.constraints.ScriptVersion_outOfLength.message}", groups = Create.class)
    @Pattern(regexp = "^[A-Za-z0-9_\\-#@.]+$", message = "{validation.constraints.ScriptVersion_illegal.message}",
        groups = Create.class)
    private String version;

    /**
     * 版本描述
     */
    @JsonProperty("version_desc")
    private String versionDesc;
}

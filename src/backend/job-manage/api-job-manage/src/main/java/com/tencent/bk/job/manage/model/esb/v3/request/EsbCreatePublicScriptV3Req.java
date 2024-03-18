package com.tencent.bk.job.manage.model.esb.v3.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbJobReq;
import com.tencent.bk.job.common.validation.CheckEnum;
import com.tencent.bk.job.common.validation.Create;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 创建公共脚本请求
 */
@Data
@ApiModel("创建公共脚本请求报文")
@EqualsAndHashCode(callSuper = true)
public class EsbCreatePublicScriptV3Req extends EsbJobReq {
    /**
     * 脚本名称
     */
    @NotEmpty(message = "{validation.constraints.ScriptName_empty.message}", groups = Create.class)
    @Length(max = 60, message = "{validation.constraints.ScriptName_outOfLength.message}", groups = Create.class)
    @Pattern(regexp = "^[^\\\\|/:*<>\"?]+$", message = "{validation.constraints.ScriptName_illegal.message}",
        groups = Create.class)
    private String name;

    /**
     * 脚本描述
     */
    private String description;

    /**
     * 脚本类型
     * @see com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum
     */
    @JsonProperty("script_language")
    @NotNull(message = "{validation.constraints.ScriptType_empty.message}",groups = Create.class)
    @CheckEnum(enumClass = ScriptTypeEnum.class, enumMethod = "isValid",
        message = "{validation.constraints.ScriptType_illegal.message}", groups = Create.class)
    private Integer type;

    /**
     * 脚本内容，需Base64编码
     */
    @NotEmpty(message = "{validation.constraints.ScriptContent_empty.message}", groups = Create.class)
    private String content;

    /**
     * 脚本版本
     */
    @NotEmpty(message = "{validation.constraints.ScriptVersion_empty.message}", groups = Create.class)
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

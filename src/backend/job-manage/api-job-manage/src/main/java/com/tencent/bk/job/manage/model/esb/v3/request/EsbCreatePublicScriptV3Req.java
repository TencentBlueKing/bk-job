package com.tencent.bk.job.manage.model.esb.v3.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbJobReq;
import com.tencent.bk.job.common.validation.CheckEnum;
import com.tencent.bk.job.common.validation.Create;
import com.tencent.bk.job.common.validation.NotBlankField;
import com.tencent.bk.job.common.validation.NotContainSpecialChar;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

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
    @NotBlankField(fieldName = "name", groups = Create.class)
    @Length(max = 60, message = "{validation.constraints.ScriptName_outOfLength.message}")
    @NotContainSpecialChar(fieldName = "name")
    private String name;

    /**
     * 脚本描述
     */
    private String description;

    /**
     * 脚本类型
     * @see com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum
     */
    @JsonProperty("script_language")
    @CheckEnum(enumClass = ScriptTypeEnum.class, enumMethod = "isValid")
    private Integer type;

    /**
     * 脚本内容，需Base64编码
     */
    @NotBlankField(fieldName = "content", groups = Create.class)
    private String content;

    /**
     * 脚本版本
     */
    @NotBlankField(fieldName = "version", groups = Create.class)
    @Length(max = 60, message = "{validation.constraints.ScriptVersion_outOfLength.message}")
    @Pattern(regexp = "^[A-Za-z0-9_\\-#@\\.]+$", message = "{validation.constraints.ScriptVersion_illegal.message}")
    private String version;

    /**
     * 版本描述
     */
    @JsonProperty("version_desc")
    private String versionDesc;
}

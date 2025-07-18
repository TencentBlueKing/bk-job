package com.tencent.bk.job.file_gateway.model.req.esb.v3;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.common.validation.NotBlankField;
import com.tencent.bk.job.common.validation.NotContainSpecialChar;
import com.tencent.bk.job.file_gateway.consts.FileSourceInfoConsts;
import com.tencent.bk.job.file_gateway.consts.FileSourceTypeEnum;
import com.tencent.bk.job.file_gateway.validate.ValidFileSourceInfo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class EsbCreateOrUpdateFileSourceV3Req extends EsbAppScopeReq {

    /**
     * 文件源Code
     */
    @ApiModelProperty(value = "文件源Code")
    @NotBlankField(fieldName = "code")
    @NotContainSpecialChar(fieldName = "code")
    private String code;
    /**
     * 文件源别名
     */
    @ApiModelProperty(value = "文件源名称")
    @NotBlankField(fieldName = "alias")
    @NotContainSpecialChar(fieldName = "alias")
    private String alias;
    /**
     * 文件源类型
     */
    @ApiModelProperty(value = "文件源类型")
    @NotBlankField(fieldName = "type")
    @NotContainSpecialChar(fieldName = "type")
    private String type;

    /**
     * 文件源信息Map
     */
    @ApiModelProperty(value = "文件源信息Map")
    @JsonProperty(value = "access_params")
    @ValidFileSourceInfo
    private Map<String, Object> accessParams = new HashMap<>();
    /**
     * 文件源凭证Id
     */
    @ApiModelProperty(value = "文件源凭证Id")
    @JsonProperty(value = "credential_id")
    @NotBlankField(fieldName = "credential_id")
    @NotContainSpecialChar(fieldName = "credential_id")
    private String credentialId;
    /**
     * 文件前缀
     */
    @ApiModelProperty(value = "文件前缀：后台自动生成UUID传${UUID}，自定义字符串直接传")
    @JsonProperty(value = "file_prefix")
    private String filePrefix = "";

    /**
     * 判断是否为蓝鲸制品库类型的文件源
     *
     * @return 布尔值
     */
    @JsonIgnore
    public boolean isBlueKingArtifactoryType() {
        return FileSourceTypeEnum.isBlueKingArtifactory(type);
    }

    /**
     * 获取蓝鲸制品库根地址
     *
     * @return 蓝鲸制品库根地址
     */
    @JsonIgnore
    public String getBkArtifactoryBaseUrl() {
        return (String) accessParams.get(FileSourceInfoConsts.KEY_BK_ARTIFACTORY_BASE_URL);
    }
}

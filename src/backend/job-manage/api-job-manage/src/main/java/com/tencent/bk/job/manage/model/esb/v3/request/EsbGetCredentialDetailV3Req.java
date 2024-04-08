package com.tencent.bk.job.manage.model.esb.v3.request;

import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.common.validation.NotBlankField;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class EsbGetCredentialDetailV3Req extends EsbAppScopeReq {

    /**
     * 凭证id
     */
    @ApiModelProperty(value = "凭证id")
    @NotBlankField(fieldName = "id")
    private String id;
}

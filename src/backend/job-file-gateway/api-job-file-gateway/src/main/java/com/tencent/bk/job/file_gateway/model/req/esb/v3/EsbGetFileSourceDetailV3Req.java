package com.tencent.bk.job.file_gateway.model.req.esb.v3;

import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.common.validation.NotBlankField;
import com.tencent.bk.job.common.validation.NotContainSpecialChar;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class EsbGetFileSourceDetailV3Req extends EsbAppScopeReq {

    /**
     * 文件源Code
     */
    @Schema(description = "文件源Code")
    @NotBlankField(fieldName = "code")
    @NotContainSpecialChar(fieldName = "code")
    private String code;
}

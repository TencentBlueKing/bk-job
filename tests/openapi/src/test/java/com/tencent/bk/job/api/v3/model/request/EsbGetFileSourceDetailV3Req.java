package com.tencent.bk.job.api.v3.model.request;

import com.tencent.bk.job.api.model.EsbAppScopeReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class EsbGetFileSourceDetailV3Req extends EsbAppScopeReq {

    /**
     * 文件源Code
     */
    private String code;
}

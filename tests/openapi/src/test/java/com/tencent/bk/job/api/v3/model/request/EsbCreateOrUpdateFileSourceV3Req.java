package com.tencent.bk.job.api.v3.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.api.model.EsbAppScopeReq;
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
    private String code;
    /**
     * 文件源别名
     */
    private String alias;
    /**
     * 文件源类型
     */
    private String type;

    /**
     * 文件源信息Map
     */
    @JsonProperty(value = "access_params")
    private Map<String, Object> accessParams = new HashMap<>();

    /**
     * 文件源凭证Id
     */
    @JsonProperty(value = "credential_id")
    private String credentialId;
    /**
     * 文件前缀
     */
    @JsonProperty(value = "file_prefix")
    private String filePrefix = "";
}

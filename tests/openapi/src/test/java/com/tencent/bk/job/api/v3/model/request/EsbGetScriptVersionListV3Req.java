package com.tencent.bk.job.api.v3.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.api.model.EsbAppScopeReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询脚本版本列表请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EsbGetScriptVersionListV3Req extends EsbAppScopeReq {
    /**
     * 脚本ID
     */
    @JsonProperty("script_id")
    private String scriptId;
    /**
     * 是否需要返回脚本内容。true:返回脚本内容；false：不返回脚本内容。默认为false。
     */
    @JsonProperty("return_script_content")
    private Boolean returnScriptContent;

    /**
     * 起始位置
     */
    private Integer start;

    /**
     * 起始位置
     */
    private Integer length;
}

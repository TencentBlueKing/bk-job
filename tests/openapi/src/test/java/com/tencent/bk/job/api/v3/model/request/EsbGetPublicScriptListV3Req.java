package com.tencent.bk.job.api.v3.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 查询公共脚本列表请求
 */
@Data
public class EsbGetPublicScriptListV3Req {
    /**
     * 脚本名称，支持模糊查询
     */
    private String name;
    /**
     * 脚本类型。0：所有脚本类型，1：shell，2：bat，3：perl，4：python，5：powershell，6：sql。默认值为0
     */
    @JsonProperty("script_language")
    private Integer scriptLanguage;

    /**
     * 起始位置
     */
    private Integer start;

    /**
     * 起始位置
     */
    private Integer length;
}

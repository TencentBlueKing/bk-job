package com.tencent.bk.job.api.v3.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.api.model.EsbAppScopeReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EsbBaseListRequest extends EsbAppScopeReq {

    /**
     * 业务 ID
     */
    @JsonProperty("bk_biz_id")
    private Long appId;

    /**
     * 名称
     */
    private String name;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 最后修改人
     */
    @JsonProperty("last_modify_user")
    private String lastModifyUser;

    /**
     * 创建时间起始点
     */
    @JsonProperty("create_time_start")
    private Long createTimeStart;

    /**
     * 创建时间结束点
     */
    @JsonProperty("create_time_end")
    private Long createTimeEnd;

    /**
     * 最后修改时间起始点
     */
    @JsonProperty("last_modify_time_start")
    private Long lastModifyTimeStart;

    /**
     * 最后修改时间结束点
     */
    @JsonProperty("last_modify_time_end")
    private Long lastModifyTimeEnd;

    /**
     * 分页起始
     */
    @JsonProperty("start")
    private Integer start;

    /**
     * 分页每页个数
     */
    @JsonProperty("length")
    private Integer length;

}

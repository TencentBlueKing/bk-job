package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * GSE 请求基础类
 */
@Data
public class GseReq {


    /**
     * 是否是GSE V2 Task; 根据gseV2Task判断请求GSE V1/v2
     */
    @JsonIgnore
    private boolean gseV2Task;

}

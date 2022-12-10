package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import lombok.Data;

/**
 * GSE 任务的上下文信息
 */
@Data
public class GseExecutionContext {
    /**
     * GSE 任务对应的资源范围
     */
    @JsonIgnore
    private ResourceScope resourceScope;
}

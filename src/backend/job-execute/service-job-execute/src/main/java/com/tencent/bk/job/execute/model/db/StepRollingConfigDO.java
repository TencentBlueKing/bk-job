package com.tencent.bk.job.execute.model.db;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.annotation.PersistenceObject;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 步骤滚动配置
 */
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@NoArgsConstructor
@PersistenceObject
public class StepRollingConfigDO {

    /**
     * 是否分批
     */
    @JsonProperty("batch")
    private boolean batch;

    public StepRollingConfigDO(boolean batch) {
        this.batch = batch;
    }
}

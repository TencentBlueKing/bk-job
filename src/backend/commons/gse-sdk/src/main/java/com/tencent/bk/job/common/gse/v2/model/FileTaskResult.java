package com.tencent.bk.job.common.gse.v2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GSE 文件分发结果
 */
@Data
@NoArgsConstructor
public class FileTaskResult {
    /**
     * 执行结果
     */
    @JsonProperty("result")
    private List<AtomicFileTaskResult> atomicFileTaskResults;
}

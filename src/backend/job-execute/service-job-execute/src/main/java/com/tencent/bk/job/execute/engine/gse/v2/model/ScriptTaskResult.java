package com.tencent.bk.job.execute.engine.gse.v2.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GSE 脚本执行结果
 */
@Data
@NoArgsConstructor
public class ScriptTaskResult {
    /**
     * 执行结果
     */
    private List<ScriptAgentTaskResult> result;
}

package com.tencent.bk.job.execute.dao.sharding;

import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.sharding.ReadModeEnum;
import com.tencent.bk.job.common.sharding.WriteModeEnum;
import com.tencent.bk.job.common.util.toggle.ToggleEvaluateContext;
import com.tencent.bk.job.common.util.toggle.ToggleStrategyContextParams;
import com.tencent.bk.job.common.util.toggle.prop.PropToggle;
import com.tencent.bk.job.common.util.toggle.prop.PropToggleStore;
import com.tencent.bk.job.execute.common.context.JobExecuteContext;
import com.tencent.bk.job.execute.common.context.JobExecuteContextThreadLocalRepo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ShardingMigrationRwModeMgr {

    private final PropToggleStore propToggleStore;

    private static final String PROP_NAME_READ_MODE = "job_execute_sharding_migrate_read_mode";

    private static final String PROP_NAME_WRITE_MODE = "job_execute_sharding_migrate_write_mode";

    public ShardingMigrationRwModeMgr(PropToggleStore propToggleStore) {
        this.propToggleStore = propToggleStore;
    }

    public ReadModeEnum evaluateReadMode() {
        ToggleEvaluateContext toggleEvaluateContext = buildToggleEvaluateContext();
        String propValue = evaluatePropValue(PROP_NAME_READ_MODE, toggleEvaluateContext);
        return ReadModeEnum.valOf(Integer.parseInt(propValue));
    }

    public WriteModeEnum evaluateWriteMode() {
        ToggleEvaluateContext toggleEvaluateContext = buildToggleEvaluateContext();
        String propValue = evaluatePropValue(PROP_NAME_WRITE_MODE, toggleEvaluateContext);
        return WriteModeEnum.valOf(Integer.parseInt(propValue));
    }

    private ToggleEvaluateContext buildToggleEvaluateContext() {
        ToggleEvaluateContext toggleEvaluateContext;

        JobExecuteContext jobExecuteContext = JobExecuteContextThreadLocalRepo.get();
        ResourceScope resourceScope = jobExecuteContext == null ? null : jobExecuteContext.getResourceScope();
        if (resourceScope != null) {
            toggleEvaluateContext = ToggleEvaluateContext.builder()
                .addContextParam(ToggleStrategyContextParams.CTX_PARAM_RESOURCE_SCOPE, resourceScope);
        } else {
            log.info("ShardingDbMigration : EmptyResourceScope!");
            toggleEvaluateContext = ToggleEvaluateContext.EMPTY;
        }

        return toggleEvaluateContext;
    }


    private String evaluatePropValue(String propName, ToggleEvaluateContext toggleEvaluateContext) {
        PropToggle propToggle = propToggleStore.getPropToggle(propName);
        return propToggle.evaluateValue(propName, toggleEvaluateContext);
    }
}

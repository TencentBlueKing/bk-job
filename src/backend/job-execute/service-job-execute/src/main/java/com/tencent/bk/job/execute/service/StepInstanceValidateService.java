package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.common.model.ValidateResult;

public interface StepInstanceValidateService {

    ValidateResult checkStepInstance(long appId, Long taskInstanceId, Long stepInstanceId);

}

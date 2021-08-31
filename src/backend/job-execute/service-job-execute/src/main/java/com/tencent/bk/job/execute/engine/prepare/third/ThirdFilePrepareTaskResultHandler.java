package com.tencent.bk.job.execute.engine.prepare.third;

import com.tencent.bk.job.execute.engine.prepare.JobTaskContext;

public interface ThirdFilePrepareTaskResultHandler {

    void onSuccess(JobTaskContext taskContext);

    void onStopped(JobTaskContext taskContext);

    void onFailed(JobTaskContext taskContext);

}

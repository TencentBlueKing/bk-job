package com.tencent.bk.job.execute.engine.prepare.local;


import com.tencent.bk.job.execute.engine.prepare.JobTaskContext;

public interface LocalFilePrepareTaskResultHandler {

    void onSuccess(JobTaskContext taskContext);

    void onStopped(JobTaskContext taskContext);

    void onFailed(JobTaskContext taskContext);

}

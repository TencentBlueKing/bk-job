package com.tencent.bk.job.execute.engine.prepare.local;

import com.tencent.bk.job.execute.engine.prepare.JobTaskContext;

public class NFSLocalFilePrepareTask implements JobTaskContext {

    boolean isForRetry;

    public NFSLocalFilePrepareTask(boolean isForRetry) {
        this.isForRetry = isForRetry;
    }

    @Override
    public boolean isForRetry() {
        return isForRetry;
    }
}

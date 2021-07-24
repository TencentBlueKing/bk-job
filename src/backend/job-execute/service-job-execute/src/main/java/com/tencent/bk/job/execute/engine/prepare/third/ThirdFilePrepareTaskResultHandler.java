package com.tencent.bk.job.execute.engine.prepare.third;

public interface ThirdFilePrepareTaskResultHandler {

    void onSuccess(ThirdFilePrepareTask prepareTask);

    void onStopped(ThirdFilePrepareTask prepareTask);

    void onFailed(ThirdFilePrepareTask prepareTask);

}

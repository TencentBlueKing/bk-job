package com.tencent.bk.job.execute.engine.prepare.local;


public interface LocalFilePrepareTaskResultHandler {

    void onSuccess(LocalFilePrepareTask prepareTask);

    void onStopped(LocalFilePrepareTask prepareTask);

    void onFailed(LocalFilePrepareTask prepareTask);

}

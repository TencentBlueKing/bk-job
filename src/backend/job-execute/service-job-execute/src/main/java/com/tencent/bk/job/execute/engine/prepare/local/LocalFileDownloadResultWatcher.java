package com.tencent.bk.job.execute.engine.prepare.local;

import com.tencent.bk.job.common.util.ListUtil;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class LocalFileDownloadResultWatcher implements Runnable {

    private final ArtifactoryLocalFilePrepareTask artifactoryLocalFilePrepareTask;
    private final StepInstanceDTO stepInstance;
    private final List<Future<Boolean>> futureList;
    private final LocalFilePrepareTaskResultHandler resultHandler;

    LocalFileDownloadResultWatcher(
        ArtifactoryLocalFilePrepareTask artifactoryLocalFilePrepareTask,
        StepInstanceDTO stepInstance,
        List<Future<Boolean>> futureList,
        LocalFilePrepareTaskResultHandler resultHandler
    ) {
        this.artifactoryLocalFilePrepareTask = artifactoryLocalFilePrepareTask;
        this.stepInstance = stepInstance;
        this.futureList = futureList;
        this.resultHandler = resultHandler;
    }

    @Override
    public void run() {
        List<Boolean> resultList = new ArrayList<>();
        for (Future<Boolean> future : futureList) {
            try {
                resultList.add(future.get(30, TimeUnit.MINUTES));
            } catch (InterruptedException e) {
                log.info("[{}]:task stopped", stepInstance.getUniqueKey());
                resultHandler.onStopped(artifactoryLocalFilePrepareTask);
                return;
            } catch (ExecutionException e) {
                log.info("[{}]:task download failed", stepInstance.getUniqueKey());
                resultHandler.onFailed(artifactoryLocalFilePrepareTask);
                return;
            } catch (TimeoutException e) {
                log.info("[{}]:task download timeout", stepInstance.getUniqueKey());
                resultHandler.onFailed(artifactoryLocalFilePrepareTask);
                return;
            }
        }
        if (ListUtil.isAllTrue(resultList)) {
            log.info("[{}]:All {} localFile download task(s) success", stepInstance.getUniqueKey(), futureList.size());
            resultHandler.onSuccess(artifactoryLocalFilePrepareTask);
        } else {
            int failCount = 0;
            for (Boolean result : resultList) {
                if (!result) {
                    failCount++;
                }
            }
            log.warn(
                "[{}]:{}/{} localFile download tasks failed",
                stepInstance.getUniqueKey(),
                failCount,
                resultList.size()
            );
            resultHandler.onFailed(artifactoryLocalFilePrepareTask);
        }
    }
}


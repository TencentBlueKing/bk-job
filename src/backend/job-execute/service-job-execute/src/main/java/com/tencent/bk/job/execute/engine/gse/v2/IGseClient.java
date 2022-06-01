package com.tencent.bk.job.execute.engine.gse.v2;

import com.tencent.bk.job.execute.engine.gse.v2.model.ExecuteScriptRequest;

/**
 * GSE API 客户端
 */
public interface IGseClient {

    void asyncExecuteScript(ExecuteScriptRequest request);


}

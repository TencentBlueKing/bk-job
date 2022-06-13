package com.tencent.bk.job.execute.engine.gse.v2;

import com.tencent.bk.job.execute.engine.gse.v2.model.Agent;
import com.tencent.bk.job.execute.engine.gse.v2.model.ExecuteScriptRequest;
import com.tencent.bk.job.execute.engine.gse.v2.model.ExecuteScriptResult;
import com.tencent.bk.job.execute.engine.gse.v2.model.GetExecuteScriptResultRequest;
import com.tencent.bk.job.execute.engine.model.GseTaskResponse;

import java.util.Collection;
import java.util.List;

/**
 * GSE API 客户端
 */
public interface IGseClient {

    /**
     * 执行脚本
     *
     * @param request 执行脚本请求
     * @return GSE 任务ID
     */
    GseTaskResponse asyncExecuteScript(ExecuteScriptRequest request);

    /**
     * 获取 GSE 脚本任务结果
     *
     * @param request 查询请求
     */
    ExecuteScriptResult getExecuteScriptResult(GetExecuteScriptResultRequest request);


    /**
     * 构建目标Agent
     *
     * @param agentIds agnetId列表
     * @param user     用户
     * @param password 密码
     * @return Agent
     */
    List<Agent> buildAgents(Collection<String> agentIds, String user, String password);


}

package com.tencent.bk.job.common.gse.v2;

import com.tencent.bk.job.common.gse.model.GseTaskResponse;
import com.tencent.bk.job.common.gse.v2.model.Agent;
import com.tencent.bk.job.common.gse.v2.model.ExecuteScriptRequest;
import com.tencent.bk.job.common.gse.v2.model.FileTaskResult;
import com.tencent.bk.job.common.gse.v2.model.GetExecuteScriptResultRequest;
import com.tencent.bk.job.common.gse.v2.model.GetTransferFileResultRequest;
import com.tencent.bk.job.common.gse.v2.model.ScriptTaskResult;
import com.tencent.bk.job.common.gse.v2.model.TransferFileRequest;

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
     * @return 下发任务响应
     */
    GseTaskResponse asyncExecuteScript(ExecuteScriptRequest request);

    /**
     * 获取 GSE 脚本任务结果
     *
     * @param request 查询请求
     */
    ScriptTaskResult getExecuteScriptResult(GetExecuteScriptResultRequest request);


    /**
     * 批量构建目标Agent
     *
     * @param agentIds agentId列表
     * @param user     用户
     * @param password 密码
     * @return Agent
     */
    List<Agent> buildAgents(Collection<String> agentIds, String user, String password);

    /**
     * 构建目标Agent
     *
     * @param agentIds agent列表
     * @param user     用户
     * @param password 密码
     * @return Agent
     */
    Agent buildAgent(String agentId, String user, String password);

    /**
     * 分发文件
     *
     * @param request 分发文件请求
     * @return GSE 下发任务响应
     */
    GseTaskResponse asyncTransferFile(TransferFileRequest request);

    /**
     * 文件任务结果查询
     *
     * @param request 文件任务结果查询请求
     */
    FileTaskResult getTransferFileResult(GetTransferFileResultRequest request);


}

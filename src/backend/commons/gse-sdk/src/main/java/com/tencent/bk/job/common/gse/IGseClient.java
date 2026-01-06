package com.tencent.bk.job.common.gse;

import com.tencent.bk.job.common.gse.v2.model.Agent;
import com.tencent.bk.job.common.gse.v2.model.ExecuteScriptRequest;
import com.tencent.bk.job.common.gse.v2.model.FileTaskResult;
import com.tencent.bk.job.common.gse.v2.model.GetExecuteScriptResultRequest;
import com.tencent.bk.job.common.gse.v2.model.GetTransferFileResultRequest;
import com.tencent.bk.job.common.gse.v2.model.GseTaskResponse;
import com.tencent.bk.job.common.gse.v2.model.ScriptTaskResult;
import com.tencent.bk.job.common.gse.v2.model.TerminateGseTaskRequest;
import com.tencent.bk.job.common.gse.v2.model.TransferFileRequest;
import com.tencent.bk.job.common.gse.v2.model.req.ListAgentStateReq;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import org.apache.zookeeper.common.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
     * 批量获取Agent状态
     *
     * @param req 请求体
     * @return Agent状态列表
     */
    List<AgentState> listAgentState(ListAgentStateReq req);

    /**
     * 设置 Agent 认证信息
     *
     * @param agents   agent列表
     * @param user     用户
     * @param defaultPwd 密码
     * @return Agent
     */
    default List<Agent> fillAgentAuthInfo(Collection<Agent> agents, String user, String defaultPwd) {
        return agents.stream().peek(agent -> {
            agent.setUser(user);
            if (StringUtils.isEmpty(agent.getPwd())) {
                agent.setPwd(defaultPwd);
            }
        }).collect(Collectors.toList());
    }

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

    /**
     * 终止GSE文件任务
     *
     * @param request 终止GSE文件任务请求
     */
    GseTaskResponse terminateGseFileTask(TerminateGseTaskRequest request);

    /**
     * 终止GSE脚本任务
     *
     * @param request 终止GSE脚本任务请求
     */
    GseTaskResponse terminateGseScriptTask(TerminateGseTaskRequest request);


}

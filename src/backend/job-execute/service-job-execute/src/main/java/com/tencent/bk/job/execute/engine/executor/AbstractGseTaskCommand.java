/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.execute.engine.executor;

import brave.Tracing;
import com.tencent.bk.job.execute.model.AccountDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.execute.service.AgentService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * GSE 任务命令基础实现
 */
@Slf4j
public abstract class AbstractGseTaskCommand implements GseTaskCommand {

    private final Tracing tracing;
    private final AgentService agentService;
    private final AccountService accountService;

    public AbstractGseTaskCommand(Tracing tracing, AgentService agentService,
                                  AccountService accountService) {
        this.tracing = tracing;
        this.agentService = agentService;
        this.accountService = accountService;
    }

    /**
     * 生成GSE trace 信息
     */
    protected Map<String, String> buildGseTraceInfo(TaskInstanceDTO taskInstance,
                                                    StepInstanceDTO stepInstance) {
        // 捕获所有异常，避免影响任务下发主流程
        Map<String, String> traceInfoMap = new HashMap<>();
        try {
            traceInfoMap.put("CALLER_NAME", "JOB");
            traceInfoMap.put("JOB_ID", stepInstance.getTaskInstanceId().toString());
            traceInfoMap.put("STEP_ID", stepInstance.getId().toString());
            traceInfoMap.put("EXECUTE_COUNT", String.valueOf(stepInstance.getExecuteCount()));
            traceInfoMap.put("JOB_BIZ_ID", taskInstance.getAppId().toString());
            if (tracing != null) {
                traceInfoMap.put("REQUEST_ID", tracing.currentTraceContext().get().traceIdString());
            }
            if (org.apache.commons.lang3.StringUtils.isNotEmpty(taskInstance.getAppCode())) {
                traceInfoMap.put("APP_CODE", taskInstance.getAppCode());
            }
            traceInfoMap.put("CALLER_IP", agentService.getLocalAgentBindIp());
            traceInfoMap.put("TASK_ACCOUNT", stepInstance.getOperator());
        } catch (Throwable e) {
            log.error("Build trace info map for gse failed");
        }
        return traceInfoMap;
    }

    /**
     * 获取账号信息
     *
     * @param accountId    账号ID
     * @param accountAlias 账号别名
     * @param appId        业务ID
     * @return 账号
     */
    protected AccountDTO getAccountBean(Long accountId, String accountAlias, Long appId) {
        AccountDTO accountInfo = null;
        if (accountId != null && accountId > 0) {
            accountInfo = accountService.getAccountById(accountId);
        } else if (StringUtils.isNotBlank(accountAlias)) { //原account传的是account,改为支持alias，减少用户API调用增加参数的成本
            accountInfo = accountService.getSystemAccountByAlias(accountAlias, appId);
        }
        // 可能帐号已经被删除了的情况：如从执行历史中点重做/克隆的方式。
        if (accountInfo == null && StringUtils.isNotBlank(accountAlias)) {//兼容老的传参，直接传递没有密码的只有帐号名称的认证
            accountInfo = new AccountDTO();
            accountInfo.setAccount(accountAlias);
            accountInfo.setAlias(accountAlias);
        }
        return accountInfo;
    }


    /**
     * 执行命令
     */
    public abstract void execute();
}

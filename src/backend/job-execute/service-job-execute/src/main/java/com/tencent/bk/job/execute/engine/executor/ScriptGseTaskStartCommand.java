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

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.gse.GseClient;
import com.tencent.bk.job.common.gse.util.ScriptRequestBuilder;
import com.tencent.bk.job.common.gse.v2.model.Agent;
import com.tencent.bk.job.common.gse.v2.model.ExecuteScriptRequest;
import com.tencent.bk.job.common.gse.v2.model.GseTaskResponse;
import com.tencent.bk.job.common.service.VariableResolver;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.util.TaskCostCalculator;
import com.tencent.bk.job.execute.common.util.VariableValueResolver;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.engine.consts.AgentTaskStatusEnum;
import com.tencent.bk.job.execute.engine.evict.TaskEvictPolicyExecutor;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.engine.model.TaskVariablesAnalyzeResult;
import com.tencent.bk.job.execute.engine.result.ResultHandleManager;
import com.tencent.bk.job.execute.engine.result.ScriptResultHandleTask;
import com.tencent.bk.job.execute.engine.result.ha.ResultHandleTaskKeepaliveManager;
import com.tencent.bk.job.execute.engine.util.MacroUtil;
import com.tencent.bk.job.execute.engine.util.TimeoutUtils;
import com.tencent.bk.job.execute.engine.variable.JobBuildInVariableResolver;
import com.tencent.bk.job.execute.engine.variable.VariableResolveContext;
import com.tencent.bk.job.execute.engine.variable.VariableResolveResult;
import com.tencent.bk.job.execute.engine.variable.VariableResolveUtils;
import com.tencent.bk.job.execute.model.AccountDTO;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.VariableValueDTO;
import com.tencent.bk.job.execute.monitor.metrics.ExecuteMonitor;
import com.tencent.bk.job.execute.monitor.metrics.GseTasksExceptionCounter;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.execute.service.AgentService;
import com.tencent.bk.job.execute.service.GseTaskService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.ScriptAgentTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.StepInstanceVariableValueService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import com.tencent.bk.job.logsvr.model.service.ServiceScriptLogDTO;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.sleuth.Tracer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ScriptGseTaskStartCommand extends AbstractGseTaskStartCommand {

    private final ScriptAgentTaskService scriptAgentTaskService;

    private final JobBuildInVariableResolver jobBuildInVariableResolver;

    /**
     * 下发到gse的脚本文件根目录
     */
    protected String scriptFilePath;
    /**
     * 下发到gse的脚本文件名前缀
     */
    protected String scriptFileNamePrefix;

    private final String GSE_SCRIPT_FILE_NAME_PREFIX = "bk_gse_script_";

    public ScriptGseTaskStartCommand(ResultHandleManager resultHandleManager,
                                     TaskInstanceService taskInstanceService,
                                     StepInstanceService stepInstanceService,
                                     GseTaskService gseTaskService,
                                     ScriptAgentTaskService scriptAgentTaskService,
                                     AccountService accountService,
                                     TaskInstanceVariableService taskInstanceVariableService,
                                     StepInstanceVariableValueService stepInstanceVariableValueService,
                                     AgentService agentService,
                                     LogService logService,
                                     TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
                                     ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager,
                                     ExecuteMonitor executeMonitor,
                                     JobExecuteConfig jobExecuteConfig,
                                     TaskEvictPolicyExecutor taskEvictPolicyExecutor,
                                     GseTasksExceptionCounter gseTasksExceptionCounter,
                                     JobBuildInVariableResolver jobBuildInVariableResolver,
                                     Tracer tracer,
                                     GseClient gseClient,
                                     String requestId,
                                     TaskInstanceDTO taskInstance,
                                     StepInstanceDTO stepInstance,
                                     GseTaskDTO gseTask) {
        super(resultHandleManager,
            taskInstanceService,
            gseTaskService,
            scriptAgentTaskService,
            accountService,
            taskInstanceVariableService,
            stepInstanceVariableValueService,
            agentService,
            logService,
            taskExecuteMQEventDispatcher,
            resultHandleTaskKeepaliveManager,
            executeMonitor,
            jobExecuteConfig,
            taskEvictPolicyExecutor,
            gseTasksExceptionCounter,
            tracer,
            gseClient,
            requestId,
            taskInstance,
            stepInstance,
            gseTask, stepInstanceService);
        this.scriptAgentTaskService = scriptAgentTaskService;
        this.jobBuildInVariableResolver = jobBuildInVariableResolver;
        this.scriptFileNamePrefix = buildScriptFileNamePrefix(stepInstance);
    }

    private String buildScriptFileNamePrefix(StepInstanceDTO stepInstance) {
        // job 下发到 GSE 的所有脚本，需要以 "bk_gse_script_" 作为前缀，GSE 会自动清理过期的脚本
        return GSE_SCRIPT_FILE_NAME_PREFIX + stepInstance.getTaskInstanceId() +
            "_" + stepInstance.getId();
    }

    @Override
    protected void preExecute() {
        scriptFilePath = buildScriptFilePath(jobExecuteConfig.getGseScriptFileRootPath(),
            stepInstance.getAccount());
    }

    private String buildScriptFilePath(String gseScriptFileRootPath, String account) {
        if (gseScriptFileRootPath.endsWith("/")) {
            return gseScriptFileRootPath + account;
        } else {
            return gseScriptFileRootPath + "/" + account;
        }
    }


    @Override
    protected GseTaskResponse startGseTask() {
        return gseClient.asyncExecuteScript(buildScriptRequest());
    }

    protected ExecuteScriptRequest buildScriptRequest() {
        ExecuteScriptRequest request;
        // shell 脚本需要支持全局变量传参，需要特殊的处理逻辑
        if (stepInstance.getScriptType().equals(ScriptTypeEnum.SHELL.getValue())
            && taskInstance.isPlanInstance()) {
            // 执行方案脚本执行步骤，需要处理变量
            request = buildShellScriptRequestForPlan();
        } else {
            request = buildRequestWithoutAnyParam(stepInstance);
        }
        request.setGseV2Task(gseV2Task);

        return request;
    }

    protected String buildScriptFileName(StepInstanceDTO stepInstance) {
        return this.scriptFileNamePrefix + ScriptTypeEnum.getExtByValue(stepInstance.getScriptType());
    }

    private ExecuteScriptRequest buildShellScriptRequestForPlan() {
        ExecuteScriptRequest request;
        boolean containsAnyImportedVariable = false;
        List<String> importVariables = null;
        if (shouldParseBuildInVariables(stepInstance)) {
            importVariables = VariableResolver.resolveJobImportVariables(stepInstance.getScriptContent());
            if (CollectionUtils.isNotEmpty(importVariables)) {
                log.info("Parse imported variables, stepInstanceId:{}, variables: {}", stepInstance.getId(),
                    importVariables);
                containsAnyImportedVariable = true;
            }
        }
        if (!containsAnyImportedVariable && !taskVariablesAnalyzeResult.isExistAnyVar()) {
            request = buildRequestWithoutAnyParam(stepInstance);
        } else if (taskVariablesAnalyzeResult.isExistOnlyConstVar()) {
            request = buildRequestWithConstParamOnly(stepInstance, taskVariablesAnalyzeResult.getTaskVars(),
                importVariables);
        } else {
            request = buildRequestWithChangeableParam(stepInstance, taskVariablesAnalyzeResult, importVariables);
        }
        return request;
    }

    private boolean shouldParseBuildInVariables(StepInstanceBaseDTO stepInstance) {
        // 只有执行方案才有stepId，并且需要解析变量
        return stepInstance.getStepId() != null && stepInstance.getStepId() > 0;
    }

    /**
     * 解析脚本参数中的变量
     *
     * @param scriptParam 脚本参数
     * @return 解析之后的脚本参数
     */
    private String resolveScriptParamVariables(String scriptParam) {
        if (StringUtils.isBlank(scriptParam) || stepInputVariables == null) {
            return scriptParam;
        }

        String resolvedScriptParam = VariableValueResolver.resolve(scriptParam,
            buildStringGlobalVarKV(stepInputVariables));
        resolvedScriptParam = resolvedScriptParam.replace("\n", " ");
        log.info("Origin script param:{}, resolved script param:{}", scriptParam, resolvedScriptParam);
        updateResolvedScriptParamIfNecessary(scriptParam, resolvedScriptParam);
        return resolvedScriptParam;
    }

    private void updateResolvedScriptParamIfNecessary(String originParam, String resolvedScriptParam) {
        // 只有存在变量解析之后才需要update
        if (!resolvedScriptParam.equals(originParam)) {
            taskInstanceService.updateResolvedScriptParam(stepInstance.getId(), resolvedScriptParam);
        }
    }

    /**
     * 创建下发请求-不带任何全局参数
     */
    private ExecuteScriptRequest buildRequestWithoutAnyParam(StepInstanceDTO stepInstance) {
        String scriptContent = stepInstance.getScriptContent();
        String scriptFileName = buildScriptFileName(stepInstance);

        String scriptParam = MacroUtil.resolveDateWithStrfTime(stepInstance.getScriptParam());
        String resolvedScriptParam = resolveScriptParamVariables(scriptParam);
        int timeout = TimeoutUtils.adjustTaskTimeout(stepInstance.getTimeout());

        ScriptRequestBuilder builder = new ScriptRequestBuilder();
        builder.addScriptFile(scriptFilePath, scriptFileName, scriptContent);

        AccountDTO accountInfo = getAccountBean(stepInstance.getAccountId(), stepInstance.getAccount(),
            stepInstance.getAppId());
        List<Agent> agents = gseClient.buildAgents(targetAgentTaskMap.keySet(),
            accountInfo.getAccount(), accountInfo.getPassword());

        builder.addScriptTask(agents, scriptFilePath, scriptFileName, resolvedScriptParam, timeout);
        return builder.build();
    }

    /**
     * 创建下发请求-仅包含常量
     */
    private ExecuteScriptRequest buildRequestWithConstParamOnly(StepInstanceDTO stepInstance,
                                                                List<TaskVariableDTO> taskVars,
                                                                List<String> importVariables) {
        ScriptRequestBuilder builder = new ScriptRequestBuilder();
        //用户原始脚本
        String userScriptContent = stepInstance.getScriptContent();
        String userScriptFileName = buildScriptFileName(stepInstance);
        builder.addScriptFile(scriptFilePath, userScriptFileName, userScriptContent);

        //声明初始变量的脚本
        String declareVarFileName = this.scriptFileNamePrefix + "_params_input.env";
        String declareVarScriptContent = buildConstVarDeclareScript(taskVars, importVariables);
        builder.addScriptFile(scriptFilePath, declareVarFileName, declareVarScriptContent);

        //封装用户脚本
        String wrapperScriptFileName = this.scriptFileNamePrefix + "_wrapper.sh";
        String wrapperScriptContent = buildWrapperScriptWithConstParamOnly(declareVarFileName, userScriptFileName);
        builder.addScriptFile(scriptFilePath, wrapperScriptFileName, wrapperScriptContent);

        String scriptParam = MacroUtil.resolveDateWithStrfTime(stepInstance.getScriptParam());
        String resolvedScriptParam = resolveScriptParamVariables(scriptParam);
        int timeout = TimeoutUtils.adjustTaskTimeout(stepInstance.getTimeout());

        AccountDTO accountInfo = getAccountBean(stepInstance.getAccountId(), stepInstance.getAccount(),
            stepInstance.getAppId());
        List<Agent> agentList = gseClient.buildAgents(targetAgentTaskMap.keySet(),
            accountInfo.getAccount(), accountInfo.getPassword());
        builder.addScriptTask(agentList, scriptFilePath, wrapperScriptFileName, resolvedScriptParam, timeout);
        return builder.build();
    }

    private String buildConstVarDeclareScript(List<TaskVariableDTO> taskVars, List<String> importVariables) {
        StringBuffer sb = new StringBuffer(1024);
        sb.append("#!/bin/bash\n");
        sb.append("set -e\n");
        for (TaskVariableDTO taskVar : taskVars) {
            buildDeclareScript(taskVar, sb);
        }
        appendImportVariablesDeclareScript(sb, taskVars, importVariables);
        sb.append("set +e\n");
        return sb.toString();
    }

    private void buildDeclareScript(TaskVariableDTO var, StringBuffer sb) {
        String paramName = var.getName();
        String paramValue = var.getValue();
        int varType = var.getType();
        if (varType == TaskVariableTypeEnum.STRING.getType() || varType == TaskVariableTypeEnum.CIPHER.getType()
            || varType == TaskVariableTypeEnum.NAMESPACE.getType()) {
            appendStringVariableDeclareScript(sb, paramName, paramValue);
        } else if (varType == TaskVariableTypeEnum.ASSOCIATIVE_ARRAY.getType()) {
            sb.append("declare -A ").append(paramName);
            if (StringUtils.isNotBlank(paramValue)) {
                sb.append("=").append(paramValue);
            }
            sb.append("\n");
        } else if (varType == TaskVariableTypeEnum.INDEX_ARRAY.getType()) {
            sb.append("declare -a ");
            sb.append(paramName);
            if (StringUtils.isNotBlank(paramValue)) {
                sb.append("=").append(paramValue);
            }
            sb.append("\n");
        }
    }

    private void appendStringVariableDeclareScript(StringBuffer sb, String variableName, String variableValue) {
        sb.append("declare ").append(variableName).append("=");
        sb.append("'").append(escapeSingleQuote(StringUtils.isEmpty(variableValue) ? "" : variableValue)).append("'\n");
    }

    private void appendImportVariablesDeclareScript(StringBuffer sb, List<TaskVariableDTO> taskVars,
                                                    List<String> importVariables) {
        if (CollectionUtils.isEmpty(importVariables)) {
            return;
        }
        Map<String, TaskVariableDTO> hostVariableMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(taskVars)) {
            taskVars.stream().filter(taskVar -> taskVar.getType() == TaskVariableTypeEnum.HOST_LIST.getType())
                .forEach(taskVar -> hostVariableMap.put(taskVar.getName(), taskVar));
        }
        Map<String, String> variableValues = new HashMap<>();
        for (String importVariable : importVariables) {
            if (hostVariableMap.containsKey(importVariable)) {
                TaskVariableDTO hostVariableValue = hostVariableMap.get(importVariable);
                String formattedHosts = "";
                if (hostVariableValue.getTargetServers() != null
                    && hostVariableValue.getTargetServers().getIpList() != null) {
                    formattedHosts = VariableResolveUtils.formatHosts(hostVariableValue.getTargetServers().getIpList());
                }
                variableValues.put(importVariable, formattedHosts);
            } else {
                VariableResolveResult resolveResult =
                    jobBuildInVariableResolver.resolve(new VariableResolveContext(taskInstance, stepInstance,
                        globalVariables, stepInputVariables), importVariable);
                if (resolveResult.isResolved()) {
                    variableValues.put(importVariable, StringUtils.isEmpty(resolveResult.getValue()) ? "" :
                        resolveResult.getValue());
                }
            }
        }
        variableValues.forEach((variableName, variableValue) ->
            appendStringVariableDeclareScript(sb, variableName, variableValue));
    }

    private String escapeSingleQuote(String value) {
        //单引号替换成'\''
        if (value == null) {
            Exception e = new RuntimeException("Unexpected value:null");
            log.warn("null given, return blank string", e);
            return "";
        }
        return value.replaceAll("'", "'\\\\''");
    }

    private String buildWrapperScriptWithConstParamOnly(String declareFileName, String userScriptFileName) {
        StringBuffer sb = new StringBuffer(1024);
        sb.append("#!/bin/bash\n");
        sb.append("BASE_PATH=\"\"\n");
        sb.append("OS_TYPE=`uname -s`\n");
        sb.append("if [ \"`echo ${OS_TYPE}|grep -i 'CYGWIN'`\" ];then\n");
        sb.append("BASE_PATH=\"/cygdrive/c\"\n");
        sb.append("fi\n");
        sb.append(". ").append("${BASE_PATH}").append(scriptFilePath).append(File.separator).
            append(declareFileName).append("\n");
        sb.append(". ").append("${BASE_PATH}").append(scriptFilePath).
            append(File.separator).append(userScriptFileName).append("\n");
        return sb.toString();
    }

    /**
     * 构造shell请求参数-包含可变参数
     *
     * @param stepInstance               步骤实例
     * @param taskVariablesAnalyzeResult 参数
     */
    private ExecuteScriptRequest buildRequestWithChangeableParam(StepInstanceDTO stepInstance,
                                                                 TaskVariablesAnalyzeResult taskVariablesAnalyzeResult,
                                                                 List<String> importVariables) {
        String scriptParam = MacroUtil.resolveDateWithStrfTime(stepInstance.getScriptParam());
        String resolvedScriptParam = resolveScriptParamVariables(scriptParam);
        int timeout = TimeoutUtils.adjustTaskTimeout(stepInstance.getTimeout());

        AccountDTO accountInfo = getAccountBean(stepInstance.getAccountId(), stepInstance.getAccount(),
            stepInstance.getAppId());
        List<Agent> agentList = gseClient.buildAgents(targetAgentTaskMap.keySet(),
            accountInfo.getAccount(), accountInfo.getPassword());

        ScriptRequestBuilder builder = new ScriptRequestBuilder();

        //用户原生脚本
        String userScriptContent = stepInstance.getScriptContent();
        String userScriptFileName = buildScriptFileName(stepInstance);

        //参数声明脚本
        String declareVarFileName = this.scriptFileNamePrefix + "_params_input.env";
        String declareVarScriptContent = buildChangeableVarDeclareScript(taskVariablesAnalyzeResult,
            importVariables);

        //封装用户脚本
        String wrapperScriptFileName = this.scriptFileNamePrefix + "_wrapper.sh";
        //所有参数导出临时文件
        String paramsOutputFileName = this.scriptFileNamePrefix + "_params_output.env";
        //命名空间参数导出文件
        String namespaceParamOutputFile = getNamespaceParamOutputFile();
        String wrapperScriptContent = buildWrapperScriptWhenExistChangeableVar(taskVariablesAnalyzeResult,
            userScriptFileName, declareVarFileName, namespaceParamOutputFile, paramsOutputFileName,
            resolvedScriptParam);

        //获取参数脚本
        String getJobParamScriptContent = buildGetJobParamsScript(paramsOutputFileName);
        String getJobParamScriptFileName = this.scriptFileNamePrefix + "_get_params.sh";

        builder.addScriptFile(scriptFilePath, userScriptFileName, userScriptContent);
        builder.addScriptFile(scriptFilePath, declareVarFileName, declareVarScriptContent);
        builder.addScriptFile(scriptFilePath, wrapperScriptFileName, wrapperScriptContent);
        builder.addScriptFile(scriptFilePath, getJobParamScriptFileName, getJobParamScriptContent);

        builder.addScriptTask(agentList, scriptFilePath, wrapperScriptFileName, resolvedScriptParam, timeout);
        builder.addScriptTask(agentList, scriptFilePath, getJobParamScriptFileName, null, timeout);
        return builder.build();
    }

    private String getNamespaceParamOutputFile() {
        return GSE_SCRIPT_FILE_NAME_PREFIX + stepInstance.getTaskInstanceId() + "_namespace_params_output.env";
    }

    /*
     * 生成变量声明和初始化脚本
     */
    private String buildChangeableVarDeclareScript(TaskVariablesAnalyzeResult taskVariablesAnalyzeResult,
                                                   List<String> importVariables) {
        List<TaskVariableDTO> globalVars = taskVariablesAnalyzeResult.getTaskVars();
        StringBuffer sb = new StringBuffer(1024);
        sb.append("#!/bin/bash\n");
        sb.append("set -e\n");

        //从作业参数中初始化输入参数
        buildInitialGlobalVarScript(globalVars, sb);
        //从之前步骤返回的参数中生成输入参数
        if (taskVariablesAnalyzeResult.isExistChangeableGlobalVar()) {
            buildChangeGlobalVariablesDeclareScript(sb);
        }
        if (taskVariablesAnalyzeResult.isExistNamespaceVar()) {
            buildNamespaceParamsDeclareScript(sb);
        }
        appendImportVariablesDeclareScript(sb, globalVars, importVariables);
        sb.append("set +e\n");
        return sb.toString();
    }

    /*
     *生成参数声明脚本-初始参数
     */
    private void buildInitialGlobalVarScript(List<TaskVariableDTO> taskVars, StringBuffer sb) {
        for (TaskVariableDTO taskVar : taskVars) {
            if (taskVar.getType() != TaskVariableTypeEnum.HOST_LIST.getType()) {
                buildDeclareScript(taskVar, sb);
            }
        }
    }

    private void buildChangeGlobalVariablesDeclareScript(StringBuffer sb) {
        if (CollectionUtils.isEmpty(stepInputVariables.getGlobalParams())) {
            return;
        }
        for (VariableValueDTO param : stepInputVariables.getGlobalParams()) {
            // 对于可赋值变量，需要重新声明赋值语句，覆盖默认值
            if (!taskVariablesAnalyzeResult.isConstVar(param.getName())) {
                TaskVariableDTO var = new TaskVariableDTO();
                var.setName(param.getName());
                var.setType(param.getType());
                var.setValue(param.getValue());
                buildDeclareScript(var, sb);
            }
        }
    }

    private void buildNamespaceParamsDeclareScript(StringBuffer sb) {
        String namespaceParamFilePath = getNamespaceParamsOutputFilePath();
        sb.append("BASE_PATH=\"\"\n");
        sb.append("OS_TYPE=`uname -s`\n");
        sb.append("if [ \"`echo ${OS_TYPE}|grep -i 'CYGWIN'`\" ];then\n");
        sb.append("BASE_PATH=\"/cygdrive/c\"\n");
        sb.append("fi\n");
        sb.append("namespace_param_output_file_path=\"").append(namespaceParamFilePath).append("\"\n");
        sb.append("if [ -f $namespace_param_output_file_path ];then\n");
        sb.append("  . ${BASE_PATH}$namespace_param_output_file_path\n");
        sb.append("  if [ $? != 0 ];then\n");
        sb.append("    exit $?\n");
        sb.append("  fi\n");
        sb.append("fi\n");
    }

    private String buildWrapperScriptWhenExistChangeableVar(TaskVariablesAnalyzeResult taskVariablesAnalyzeResult,
                                                            String scriptFileName,
                                                            String varInputFileName,
                                                            String namespaceParamsOutputFileName,
                                                            String allParamsOutputFileName,
                                                            String scriptParam) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("#!/bin/bash\n");
        sb.append("BASE_PATH=\"\"\n");
        sb.append("OS_TYPE=`uname -s`\n");
        sb.append("if [ \"`echo ${OS_TYPE}|grep -i 'CYGWIN'`\" ];then\n");
        sb.append("BASE_PATH=\"/cygdrive/c\"\n");
        sb.append("fi\n");
        sb.append(buildOutputVarsOnExitFunction(taskVariablesAnalyzeResult, namespaceParamsOutputFileName,
            allParamsOutputFileName));
        sb.append("trap 'outputVarOnExit;' EXIT\n");
        sb.append(". ").append("${BASE_PATH}").append(scriptFilePath).append(File.separator).
            append(varInputFileName).append("\n");
        sb.append(". ").append("${BASE_PATH}").append(scriptFilePath).
            append(File.separator).append(scriptFileName);
        if (StringUtils.isNotEmpty(scriptParam)) {
            sb.append(" ").append(scriptParam).append("\n");
        } else {
            sb.append("\n");
        }
        return sb.toString();
    }

    private String buildOutputVarsOnExitFunction(TaskVariablesAnalyzeResult taskVariablesAnalyzeResult, String
        namespaceParamsOutputFileName,
                                                 String allParamsOutputFileName) {
        StringBuilder sb = new StringBuilder();
        sb.append("function outputVarOnExit(){\n");
        sb.append("  exit_code=$?\n");
        if (taskVariablesAnalyzeResult.isExistAnyVar()) {
            sb.append("  set|egrep '");
            int paramCount = taskVariablesAnalyzeResult.getAllVarNames().size();
            for (String paramName : taskVariablesAnalyzeResult.getAllVarNames()) {
                sb.append("^").append(paramName).append("=");
                if (paramCount-- != 1) {
                    sb.append("|");
                }
            }

            sb.append("' > ");
            sb.append("${BASE_PATH}").append(scriptFilePath).append(File.separator)
                .append(allParamsOutputFileName).append("\n");
        }
        if (taskVariablesAnalyzeResult.isExistNamespaceVar()) {
            String namespaceParamOutputPath =
                "${BASE_PATH}" + scriptFilePath + File.separator + namespaceParamsOutputFileName;
            sb.append("  if [ ! -f ").append(namespaceParamOutputPath).append(" ];then\n");
            sb.append("    touch ").append(namespaceParamOutputPath).append("\n");
            sb.append("    chmod 700 ").append(namespaceParamOutputPath).append("\n");
            sb.append("  fi\n");
            sb.append("  sed -i 's/^[#]*/#/g' ").append(namespaceParamOutputPath).append("\n");
            sb.append("  sed -i '$a #step_instance_id:").append(stepInstanceId).append("' ")
                .append(namespaceParamOutputPath).append("\n");
            sb.append("  set|egrep '");
            int paramCount = taskVariablesAnalyzeResult.getNamespaceVarNames().size();
            for (String paramName : taskVariablesAnalyzeResult.getNamespaceVarNames()) {
                sb.append("^").append(paramName).append("=");
                if (paramCount-- != 1) {
                    sb.append("|");
                }
            }

            sb.append("' >> ");
            sb.append(namespaceParamOutputPath).append("\n");
        }
        sb.append("  return $exit_code\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String buildGetJobParamsScript(String varOutputFileName) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("#!/bin/bash\n");
        sb.append("BASE_PATH=\"\"\n");
        sb.append("OS_TYPE=`uname -s`\n");
        sb.append("if [ \"`echo ${OS_TYPE}|grep -i 'CYGWIN'`\" ];then\n");
        sb.append("BASE_PATH=\"/cygdrive/c\"\n");
        sb.append("fi\n");
        sb.append("\n");
        String catFilePath = "${BASE_PATH}" + scriptFilePath + File.separator + varOutputFileName;
        sb.append("declare -i total_time=10\n");
        sb.append("declare -i cost_time=0\n");
        sb.append("while [ $cost_time -le $total_time ];do\n");
        sb.append("  if [ -f ").append(catFilePath).append(" ];then\n");
        sb.append("    cat ").append(catFilePath).append("\n");
        sb.append("    break\n");
        sb.append("  else\n");
        sb.append("    cost_time=$(($cost_time+1))\n");
        sb.append("    sleep 1\n");
        sb.append("  fi\n");
        sb.append("done\n");
        return sb.toString();
    }

    private String getNamespaceParamsOutputFilePath() {
        return scriptFilePath + "/" + getNamespaceParamOutputFile();
    }

    @Override
    protected final void addResultHandleTask() {
        ScriptResultHandleTask scriptResultHandleTask =
            new ScriptResultHandleTask(
                taskInstanceService,
                gseTaskService,
                logService,
                taskInstanceVariableService,
                stepInstanceVariableValueService,
                taskExecuteMQEventDispatcher,
                resultHandleTaskKeepaliveManager,
                taskEvictPolicyExecutor,
                scriptAgentTaskService,
                stepInstanceService,
                gseClient,
                taskInstance,
                stepInstance,
                taskVariablesAnalyzeResult,
                targetAgentTaskMap,
                gseTask,
                requestId);
        resultHandleManager.handleDeliveredTask(scriptResultHandleTask);
    }


    @Override
    protected final void handleStartGseTaskError(GseTaskResponse gseTaskResponse) {
        long now = DateUtils.currentTimeMillis();
        updateGseTaskExecutionInfo(null, RunStatusEnum.FAIL, null, now, now - gseTask.getStartTime());

        String errorMsg = "GSE Job failed:" + gseTaskResponse.getErrorMessage();
        int errorMsgLength = errorMsg.length();

        List<ServiceScriptLogDTO> scriptLogs = new ArrayList<>(targetAgentTaskMap.size());
        for (AgentTaskDTO agentTask : targetAgentTaskMap.values()) {
            // 日志输出
            ServiceScriptLogDTO scriptLog = logService.buildSystemScriptLog(agentTask.getHost().getHostId(), errorMsg,
                agentTask.getScriptLogOffset() + errorMsgLength, now);
            scriptLogs.add(scriptLog);

            // AgentTask 结果更新
            agentTask.setGseTaskId(gseTask.getId());
            agentTask.setStartTime(gseTask.getStartTime());
            agentTask.setEndTime(now);
            agentTask.setTotalTime(TaskCostCalculator.calculate(gseTask.getStartTime(), now, null));
            agentTask.setStatus(AgentTaskStatusEnum.SUBMIT_FAILED);
        }
        logService.batchWriteScriptLog(taskInstance.getCreateTime(), stepInstanceId, executeCount, batch, scriptLogs);
        scriptAgentTaskService.batchUpdateAgentTasks(targetAgentTaskMap.values());
    }
}

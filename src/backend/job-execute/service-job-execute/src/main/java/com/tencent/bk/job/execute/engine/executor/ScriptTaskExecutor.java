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

import com.tencent.bk.gse.taskapi.api_agent;
import com.tencent.bk.gse.taskapi.api_script_request;
import com.tencent.bk.gse.taskapi.api_stop_task_request;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.service.VariableResolver;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.util.VariableValueResolver;
import com.tencent.bk.job.execute.engine.consts.GseConstants;
import com.tencent.bk.job.execute.engine.gse.GseRequestUtils;
import com.tencent.bk.job.execute.engine.gse.ScriptRequestBuilder;
import com.tencent.bk.job.execute.engine.model.GseTaskExecuteResult;
import com.tencent.bk.job.execute.engine.model.GseTaskResponse;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.engine.model.TaskVariablesAnalyzeResult;
import com.tencent.bk.job.execute.engine.result.ScriptResultHandleTask;
import com.tencent.bk.job.execute.engine.util.MacroUtil;
import com.tencent.bk.job.execute.engine.util.TimeoutUtils;
import com.tencent.bk.job.execute.engine.variable.JobBuildInVariableResolver;
import com.tencent.bk.job.execute.engine.variable.VariableResolveContext;
import com.tencent.bk.job.execute.engine.variable.VariableResolveResult;
import com.tencent.bk.job.execute.engine.variable.VariableResolveUtils;
import com.tencent.bk.job.execute.model.AccountDTO;
import com.tencent.bk.job.execute.model.GseTaskLogDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.VariableValueDTO;
import com.tencent.bk.job.execute.monitor.metrics.GseTasksExceptionCounter;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * GSE-脚本任务执行
 */
@Slf4j
public class ScriptTaskExecutor extends AbstractGseTaskExecutor {
    /**
     * 下发到gse的脚本文件根目录
     */
    protected final String scriptFilePath;

    private final JobBuildInVariableResolver jobBuildInVariableResolver;

    /**
     * ScriptTaskExecutor Constructor
     *
     * @param requestId                  请求ID
     * @param gseTasksExceptionCounter   GSE任务异常计数
     * @param taskInstance               作业实例
     * @param stepInstance               步骤实例
     * @param executeIps                 目标IP
     * @param jobBuildInVariableResolver 内置变量解析
     */
    public ScriptTaskExecutor(String requestId, GseTasksExceptionCounter gseTasksExceptionCounter,
                              TaskInstanceDTO taskInstance,
                              StepInstanceDTO stepInstance,
                              Set<String> executeIps,
                              JobBuildInVariableResolver jobBuildInVariableResolver) {
        super(requestId, gseTasksExceptionCounter, taskInstance, stepInstance, executeIps);
        this.jobBuildInVariableResolver = jobBuildInVariableResolver;
        scriptFilePath = GseConstants.SCRIPT_PATH + "/" + stepInstance.getAccount();
    }

    @Override
    protected void initExecutionContext() {
        super.initExecutionContext();
    }


    @Override
    protected GseTaskResponse startGseTask(StepInstanceDTO stepInstance) {
        return GseRequestUtils.sendScriptTaskRequest(stepInstanceId, getScriptRequest(stepInstance));
    }

    protected api_script_request getScriptRequest(StepInstanceDTO stepInstance) {
        // shell 脚本需要支持全局变量传参，需要特殊的处理逻辑
        if (stepInstance.getScriptType().equals(ScriptTypeEnum.SHELL.getValue())) {
            api_script_request request = buildShellScriptRequest(stepInstance);
            request.setM_caller(buildTraceInfoMap());
            return request;
        }
        String scriptContent = stepInstance.getScriptContent();
        String scriptFileName = buildScriptFileName(stepInstance);

        String scriptParam = MacroUtil.resolveDateWithStrfTime(stepInstance.getScriptParam());
        String resolvedScriptParam = resolveScriptParamVariables(scriptParam);
        int timeout = TimeoutUtils.adjustTaskTimeout(stepInstance.getTimeout());

        AccountDTO accountInfo = getAccountBean(stepInstance.getAccountId(), stepInstance.getAccount(),
            stepInstance.getAppId());
        List<api_agent> agentList = GseRequestUtils.buildAgentList(jobIpSet, accountInfo.getAccount(),
            accountInfo.getPassword());
        api_script_request request = GseRequestUtils.buildScriptRequest(agentList, scriptContent, scriptFileName,
            scriptFilePath, resolvedScriptParam, timeout);
        request.setM_caller(buildTraceInfoMap());
        return request;
    }

    private String buildScriptFileName(StepInstanceDTO stepInstance) {
        String scriptFileNamePrefix = buildScriptFileNamePrefix(stepInstance);
        return scriptFileNamePrefix + ScriptTypeEnum.getExtByValue(stepInstance.getScriptType());
    }

    public String buildScriptFileNamePrefix(StepInstanceDTO stepInstance) {
        return "task_" + stepInstance.getTaskInstanceId() +
            "_" + stepInstance.getId();
    }

    /**
     * 构建shell脚本下发的请求
     */
    private api_script_request buildShellScriptRequest(StepInstanceDTO stepInstance) {
        api_script_request request;
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
            buildReferenceGlobalVarValueMap(stepInputVariables));
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
    private api_script_request buildRequestWithoutAnyParam(StepInstanceDTO stepInstance) {
        String scriptContent = stepInstance.getScriptContent();
        String scriptFileName = buildScriptFileName(stepInstance);

        String scriptParam = MacroUtil.resolveDateWithStrfTime(stepInstance.getScriptParam());
        String resolvedScriptParam = resolveScriptParamVariables(scriptParam);
        int timeout = TimeoutUtils.adjustTaskTimeout(stepInstance.getTimeout());

        ScriptRequestBuilder builder = new ScriptRequestBuilder();
        builder.addScriptFile(scriptFilePath, scriptFileName, scriptContent);

        AccountDTO accountInfo = getAccountBean(stepInstance.getAccountId(), stepInstance.getAccount(),
            stepInstance.getAppId());
        List<api_agent> agentList = GseRequestUtils.buildAgentList(jobIpSet, accountInfo.getAccount(),
            accountInfo.getPassword());

        builder.addScriptTask(agentList, scriptFilePath, scriptFileName, resolvedScriptParam, timeout);
        return builder.build();
    }

    /**
     * 创建下发请求-仅包含常量
     */
    private api_script_request buildRequestWithConstParamOnly(StepInstanceDTO stepInstance,
                                                              List<TaskVariableDTO> taskVars,
                                                              List<String> importVariables) {
        ScriptRequestBuilder builder = new ScriptRequestBuilder();
        //用户原始脚本
        String userScriptContent = stepInstance.getScriptContent();
        String userScriptFileName = buildScriptFileName(stepInstance);
        builder.addScriptFile(scriptFilePath, userScriptFileName, userScriptContent);

        String scriptFileNamePrefix = buildScriptFileNamePrefix(stepInstance);

        //声明初始变量的脚本
        String declareVarFileName = "stepInstanceId_" + stepInstance.getId() + "_params_input.env";
        String declareVarScriptContent = buildConstVarDeclareScript(taskVars, importVariables);
        builder.addScriptFile(scriptFilePath, declareVarFileName, declareVarScriptContent);

        //封装用户脚本
        String wrapperScriptFileName = scriptFileNamePrefix + "_wrapper.sh";
        String wrapperScriptContent = buildWrapperScriptWithConstParamOnly(declareVarFileName, userScriptFileName);
        builder.addScriptFile(scriptFilePath, wrapperScriptFileName, wrapperScriptContent);

        String scriptParam = MacroUtil.resolveDateWithStrfTime(stepInstance.getScriptParam());
        String resolvedScriptParam = resolveScriptParamVariables(scriptParam);
        int timeout = TimeoutUtils.adjustTaskTimeout(stepInstance.getTimeout());

        AccountDTO accountInfo = getAccountBean(stepInstance.getAccountId(), stepInstance.getAccount(),
            stepInstance.getAppId());
        List<api_agent> agentList = GseRequestUtils.buildAgentList(jobIpSet, accountInfo.getAccount(),
            accountInfo.getPassword());
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
     * @return
     */
    private api_script_request buildRequestWithChangeableParam(StepInstanceDTO stepInstance,
                                                               TaskVariablesAnalyzeResult taskVariablesAnalyzeResult,
                                                               List<String> importVariables) {
        String scriptParam = MacroUtil.resolveDateWithStrfTime(stepInstance.getScriptParam());
        String resolvedScriptParam = resolveScriptParamVariables(scriptParam);
        int timeout = TimeoutUtils.adjustTaskTimeout(stepInstance.getTimeout());

        AccountDTO accountInfo = getAccountBean(stepInstance.getAccountId(), stepInstance.getAccount(),
            stepInstance.getAppId());
        List<api_agent> agentList = GseRequestUtils.buildAgentList(jobIpSet, accountInfo.getAccount(),
            accountInfo.getPassword());

        ScriptRequestBuilder builder = new ScriptRequestBuilder();

        //用户原生脚本
        String userScriptContent = stepInstance.getScriptContent();
        String userScriptFileName = buildScriptFileName(stepInstance);

        String scriptFileNamePrefix = buildScriptFileNamePrefix(stepInstance);

        //参数声明脚本
        String declareVarFileName = scriptFileNamePrefix + "_params_input.env";
        String declareVarScriptContent = buildChangeableVarDeclareScript(taskVariablesAnalyzeResult,
            importVariables);

        //封装用户脚本
        String wrapperScriptFileName = scriptFileNamePrefix + "_wrapper.sh";
        //所有参数导出临时文件
        String paramsOutputFileName = scriptFileNamePrefix + "_params_output.env";
        //命名空间参数导出文件
        String namespaceParamOutputFile = getNamespaceParamOutputFile();
        String wrapperScriptContent = buildWrapperScriptWhenExistChangeableVar(taskVariablesAnalyzeResult,
            userScriptFileName, declareVarFileName, namespaceParamOutputFile, paramsOutputFileName,
            resolvedScriptParam);

        //获取参数脚本
        String getJobParamScriptContent = buildGetJobParamsScript(paramsOutputFileName);
        String getJobParamScriptFileName = scriptFileNamePrefix + "_get_params.sh";

        builder.addScriptFile(scriptFilePath, userScriptFileName, userScriptContent);
        builder.addScriptFile(scriptFilePath, declareVarFileName, declareVarScriptContent);
        builder.addScriptFile(scriptFilePath, wrapperScriptFileName, wrapperScriptContent);
        builder.addScriptFile(scriptFilePath, getJobParamScriptFileName, getJobParamScriptContent);

        builder.addScriptTask(agentList, scriptFilePath, wrapperScriptFileName, resolvedScriptParam, timeout);
        builder.addScriptTask(agentList, scriptFilePath, getJobParamScriptFileName, null, timeout);
        return builder.build();
    }

    private String getNamespaceParamOutputFile() {
        return "taskInstanceId_" + stepInstance.getTaskInstanceId() + "_namespace_params_output.env";
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
    public GseTaskExecuteResult stopGseTask() {
        AccountDTO accountInfo = getAccountBean(stepInstance.getAccountId(), stepInstance.getAccount(),
            stepInstance.getAppId());
        List<api_agent> agentList = GseRequestUtils.buildAgentList(jobIpSet, accountInfo.getAccount(),
            accountInfo.getPassword());
        api_stop_task_request stopTaskRequest = new api_stop_task_request();
        GseTaskLogDTO gseTaskLog = gseTaskLogService.getGseTaskLog(stepInstanceId, executeCount);
        if (gseTaskLog == null || StringUtils.isEmpty(gseTaskLog.getGseTaskId())) {
            log.warn("Gse Task not send to gse server, not support stop");
            return new GseTaskExecuteResult(GseTaskExecuteResult.RESULT_CODE_STOP_FAILED, "Termination failed");
        }
        stopTaskRequest.setStop_task_id(gseTaskLog.getGseTaskId());
        stopTaskRequest.setAgents(agentList);
        stopTaskRequest.setType(StepExecuteTypeEnum.EXECUTE_SCRIPT.getValue());
        stopTaskRequest.setM_caller(buildTraceInfoMap());

        GseTaskResponse gseTaskResponse = GseRequestUtils.sendForceStopTaskRequest(stepInstance.getId(),
            stopTaskRequest);
        if (GseTaskResponse.ERROR_CODE_SUCCESS != gseTaskResponse.getErrorCode()) {
            log.info("sendForceStopTaskRequest response failed!");
            return new GseTaskExecuteResult(GseTaskExecuteResult.RESULT_CODE_STOP_FAILED,
                "Termination failed， msg:" + gseTaskResponse.getErrorMessage());
        } else {
            log.info("sendForceStopTaskRequest response success!");
            return new GseTaskExecuteResult(GseTaskExecuteResult.RESULT_CODE_STOP_SUCCESS, "Termination successfully");
        }
    }

    @Override
    void addExecutionResultHandleTask() {
        ScriptResultHandleTask scriptResultHandleTask =
            new ScriptResultHandleTask(taskInstance, stepInstance, taskVariablesAnalyzeResult, ipLogMap, gseTaskLog,
                jobIpSet, requestId);
        scriptResultHandleTask.initDependentService(taskInstanceService, gseTaskLogService, logService,
            taskInstanceVariableService, stepInstanceVariableValueService,
            taskManager, resultHandleTaskKeepaliveManager, exceptionStatusManager);
        resultHandleManager.handleDeliveredTask(scriptResultHandleTask);
    }

    @Override
    public void resume() {
        log.info("Resume script task from snapshot, stepInstanceId: {}", stepInstanceId);
    }

    @Override
    public void saveSnapshot() {
        log.info("Save script task snapshot, stepInstanceId: {}", stepInstanceId);
    }

    @Override
    protected boolean checkHostExecutable() {
        return !jobIpSet.isEmpty();
    }
}

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

package com.tencent.bk.job.execute.engine.result;

import com.tencent.bk.job.common.gse.constants.GSECode;
import com.tencent.bk.job.common.gse.v2.model.ExecuteObjectGseKey;
import com.tencent.bk.job.common.gse.v2.model.GetExecuteScriptResultRequest;
import com.tencent.bk.job.common.gse.v2.model.ScriptExecuteObjectTaskResult;
import com.tencent.bk.job.common.gse.v2.model.ScriptTaskResult;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.CollectionUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.file.FileSizeUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.constants.VariableValueTypeEnum;
import com.tencent.bk.job.execute.engine.EngineDependentServiceHolder;
import com.tencent.bk.job.execute.engine.consts.ExecuteObjectTaskStatusEnum;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.engine.model.GseLogBatchPullResult;
import com.tencent.bk.job.execute.engine.model.GseTaskExecuteResult;
import com.tencent.bk.job.execute.engine.model.GseTaskResult;
import com.tencent.bk.job.execute.engine.model.LogPullProgress;
import com.tencent.bk.job.execute.engine.model.ScriptGseTaskResult;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.engine.model.TaskVariablesAnalyzeResult;
import com.tencent.bk.job.execute.engine.util.GseUtils;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.HostVariableValuesDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepInstanceVariableValuesDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.VariableValueDTO;
import com.tencent.bk.job.execute.service.ScriptExecuteObjectTaskService;
import com.tencent.bk.job.logsvr.model.service.ServiceExecuteObjectScriptLogDTO;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 脚本任务执行结果处理
 */
@Slf4j
public class ScriptResultHandleTask extends AbstractResultHandleTask<ScriptTaskResult> {
    /**
     * GSE日志查询支持的每一批次的最大Agent数目
     */
    private static final int MAX_QUERY_AGENT_TASK_SIZE = 1000;
    /**
     * GSE日志查询支持的每一批次的脚本执行输出日志长度(单位byte)
     */
    private long maxQueryContentSizeLimit;
    /**
     * GSE 每个脚本执行原子任务对应的最大日志大小，5MB
     */
    private static final int MAX_GSE_ATOMIC_TASK_CONTENT_BYTES = 5 * 1024 * 1024;
    /**
     * 脚本任务执行日志进度
     */
    private final Map<ExecuteObjectGseKey, LogPullProgress> logPullProgressMap = new HashMap<>();
    /**
     * 保存命名空间参数，Map<ExecuteObjectGseKey,Map<paramName,paramValue>>
     */
    private final Map<ExecuteObjectGseKey, Map<String, String>> namespaceParamValues = new HashMap<>();
    /**
     * 保存可变全局参数，Map<ExecuteObjectGseKey,Map<paramName,paramValue>>
     */
    private final Map<ExecuteObjectGseKey, Map<String, String>> changeableGlobalParamValues = new HashMap<>();
    /**
     * 脚本任务结果处理调度策略
     */
    private volatile ScheduleStrategy scheduleStrategy;
    /**
     * 目标Agent分批
     */
    private List<List<ExecuteObjectGseKey>> pullExecuteObjectGseKeyBatches = new LinkedList<>();
    /**
     * 每一轮拉取的批次序号
     */
    private AtomicInteger pullResultBatchesIndex = new AtomicInteger(1);


    /**
     * 任务基本信息，用于日志输出
     */
    private String taskInfo;

    public ScriptResultHandleTask(EngineDependentServiceHolder engineDependentServiceHolder,
                                  ScriptExecuteObjectTaskService scriptExecuteObjectTaskService,
                                  JobExecuteConfig jobExecuteConfig,
                                  TaskInstanceDTO taskInstance,
                                  StepInstanceDTO stepInstance,
                                  TaskVariablesAnalyzeResult taskVariablesAnalyzeResult,
                                  Map<ExecuteObjectGseKey, ExecuteObjectTask> executeObjectTaskMap,
                                  GseTaskDTO gseTask,
                                  String requestId,
                                  List<ExecuteObjectTask> executeObjectTasks) {
        super(engineDependentServiceHolder,
            scriptExecuteObjectTaskService,
            taskInstance,
            stepInstance,
            taskVariablesAnalyzeResult,
            executeObjectTaskMap,
            gseTask,
            requestId,
            executeObjectTasks);
        this.maxQueryContentSizeLimit =
            FileSizeUtil.parseFileSizeBytes(jobExecuteConfig.getScriptTaskQueryContentSizeLimit());
        initLogPullProcess(executeObjectTaskMap.values());
    }

    private void initLogPullProcess(Collection<ExecuteObjectTask> executeObjectTasks) {
        executeObjectTasks.forEach(executeObjectTask -> {
            ExecuteObjectGseKey executeObjectGseKey =
                executeObjectTask.getExecuteObject().toExecuteObjectGseKey();
            LogPullProgress process = new LogPullProgress();
            process.setExecuteObjectGseKey(executeObjectGseKey);
            process.setByteOffset(executeObjectTask.getScriptLogOffset());
            process.setAtomicTaskId(0);
            logPullProgressMap.put(executeObjectGseKey, process);
        });
    }

    @Override
    GseLogBatchPullResult<ScriptTaskResult> pullGseTaskResultInBatches() {
        if (pullExecuteObjectGseKeyBatches.isEmpty()) {
            List<ExecuteObjectGseKey> queryExecuteObjectGseKeyList
                = new ArrayList<>(notFinishedTargetExecuteObjectGseKeys);
            pullExecuteObjectGseKeyBatches = CollectionUtil.partitionList(
                queryExecuteObjectGseKeyList, MAX_QUERY_AGENT_TASK_SIZE);
        }
        List<ExecuteObjectGseKey> pullLogExecuteObjectGseKeys
            = pullExecuteObjectGseKeyBatches.get(pullResultBatchesIndex.get() - 1);
        ScriptTaskResult result = pullGseTaskResult(pullLogExecuteObjectGseKeys);
        boolean isLastBatch = pullResultBatchesIndex.get() == pullExecuteObjectGseKeyBatches.size();
        GseLogBatchPullResult<ScriptTaskResult> batchPullResult = new GseLogBatchPullResult<>(
            isLastBatch, new ScriptGseTaskResult(result));
        if (isLastBatch) {
            resetBatch();
        } else {
            pullResultBatchesIndex.incrementAndGet();
        }
        return batchPullResult;
    }

    private void resetBatch() {
        pullResultBatchesIndex = new AtomicInteger(1);
        pullExecuteObjectGseKeyBatches.clear();
    }

    private ScriptTaskResult pullGseTaskResult(List<ExecuteObjectGseKey> executeObjectGseKeys) {
        GetExecuteScriptResultRequest request = new GetExecuteScriptResultRequest();
        request.setGseV2Task(gseV2Task);
        request.setTaskId(gseTask.getGseTaskId());

        int executeObjectSize = executeObjectGseKeys.size();
        long limit = maxQueryContentSizeLimit / executeObjectSize;
        // 如果计算出来的 limit 值大于 GSE 本身的单任务输出内容大小限制，不需要传入 limit
        boolean enableLimitContentRequestParam = limit < MAX_GSE_ATOMIC_TASK_CONTENT_BYTES;

        executeObjectGseKeys.forEach(executeObjectGseKey -> {
            LogPullProgress progress = logPullProgressMap.get(executeObjectGseKey);
            if (progress == null) {
                request.addAgentTaskQuery(executeObjectGseKey, 0, 0,
                    enableLimitContentRequestParam ? limit : null);
            } else {
                request.addAgentTaskQuery(executeObjectGseKey, progress.getAtomicTaskId(), progress.getByteOffset(),
                    enableLimitContentRequestParam ? limit : null);
            }
        });

        return gseClient.getExecuteScriptResult(request);
    }

    @Override
    GseTaskExecuteResult analyseGseTaskResult(GseTaskResult<ScriptTaskResult> taskDetail) {
        if (taskDetail == null || taskDetail.isEmptyResult()) {
            log.info("[{}] Analyse gse task result, result is empty!", gseTaskInfo);
            return analyseExecuteResult();
        }
        long currentTime = DateUtils.currentTimeMillis(); // 当前时间
        List<ServiceExecuteObjectScriptLogDTO> scriptLogs = new ArrayList<>();
        StopWatch watch = new StopWatch("analyse-gse-script-task");
        watch.start("analyse");
        for (ScriptExecuteObjectTaskResult executeObjectTaskResult : taskDetail.getResult().getResult()) {
            ExecuteObjectGseKey executeObjectGseKey = executeObjectTaskResult.getExecuteObjectGseKey();
            if (!shouldAnalyse(executeObjectTaskResult)) {
                continue;
            }

            ExecuteObjectTask executeObjectTask = targetExecuteObjectTasks.get(executeObjectGseKey);
            if (executeObjectTask == null) {
                log.warn("[{}] No execute object task found for executeObjectGseKey {}. result: {}",
                    gseTaskInfo, executeObjectGseKey, JsonUtils.toJson(executeObjectTaskResult));
                continue;
            }

            log.info("[{}]: Analyse execute object task result, result: {}", gseTaskInfo, executeObjectTaskResult);

            /*为了解决shell上下文传参的问题，在下发用户脚本的时候，实际上下发两个脚本。第一个脚本是用户脚本，第二个脚本
             *是获取上下文参数的脚本。所以m_id=0的是用户脚本的执行日志，需要分析记录；m_id=1的，则是获取上下文参数
             *输出的日志内容，不需要记录，仅需要从日志分析提取上下文参数*/
            boolean isUserScriptResult = executeObjectTaskResult.getAtomicTaskId() == 0;
            if (isUserScriptResult) {
                addScriptLogsAndRefreshPullProgress(scriptLogs, executeObjectTaskResult,
                    executeObjectGseKey, executeObjectTask, currentTime);
            }

            analyseAgentResult(executeObjectTaskResult, executeObjectTask, executeObjectGseKey, isUserScriptResult,
                currentTime);
        }
        watch.stop();

        watch.start("saveScriptContent");
        saveScriptLogContent(scriptLogs);
        watch.stop();

        watch.start("saveChangedExecuteObjectTasks");
        batchSaveChangedExecuteObjectTasks(targetExecuteObjectTasks.values());
        watch.stop();

        log.info("[{}] Analyse gse task result -> notFinishedTargetExecuteObjectGseKeys={}" +
                ", analyseFinishedTargetExecuteObjectGseKeys={}",
            this.gseTaskInfo, this.notFinishedTargetExecuteObjectGseKeys,
            this.analyseFinishedTargetExecuteObjectGseKeys);

        GseTaskExecuteResult rst = analyseExecuteResult();
        if (!rst.getResultCode().equals(GseTaskExecuteResult.RESULT_CODE_RUNNING)) {
            watch.start("saveVariables");
            saveStepInstanceVariables();
            watch.stop();
        }

        if (watch.getTotalTimeMillis() > 1000L) {
            log.info("[{}] Analyse script gse task is slow, statistics: {}", gseTaskInfo, watch.prettyPrint());
        }
        return rst;
    }

    private boolean shouldAnalyse(ScriptExecuteObjectTaskResult executeObjectTaskResult) {
        ExecuteObjectGseKey executeObjectGseKey = executeObjectTaskResult.getExecuteObjectGseKey();
        // 该Agent已经日志分析结束，不要再分析
        if (this.analyseFinishedTargetExecuteObjectGseKeys.contains(executeObjectGseKey)) {
            return false;
        }
        if (!this.targetExecuteObjectGseKeys.contains(executeObjectGseKey)) {
            log.warn("[{}] Unexpected target executeObjectGseKey {}. result: {}", gseTaskInfo, executeObjectGseKey,
                JsonUtils.toJson(executeObjectTaskResult));
            return false;
        }
        return true;
    }

    private void addScriptLogsAndRefreshPullProgress(List<ServiceExecuteObjectScriptLogDTO> logs,
                                                     ScriptExecuteObjectTaskResult executeObjectTaskResult,
                                                     ExecuteObjectGseKey executeObjectGseKey,
                                                     ExecuteObjectTask executeObjectTask,
                                                     long currentTime) {
        ExecuteObject executeObject = findExecuteObject(executeObjectGseKey);
        if (GSECode.AtomicErrorCode.getErrorCode(executeObjectTaskResult.getErrorCode())
            == GSECode.AtomicErrorCode.ERROR) {
            if (StringUtils.isNotEmpty(executeObjectTaskResult.getErrorMsg())) {
                logs.add(logService.buildSystemScriptLog(stepInstance, executeObject,
                    executeObjectTaskResult.getErrorMsg(), executeObjectTask.getScriptLogOffset(), currentTime));
            }
        } else {
            String content = executeObjectTaskResult.getScreen();
            if (StringUtils.isEmpty(content)) {
                return;
            }
            int offset = executeObjectTask.getScriptLogOffset();
            int contentSizeBytes = 0;
            if (StringUtils.isNotEmpty(content)) {
                contentSizeBytes = content.getBytes(StandardCharsets.UTF_8).length;
                offset += contentSizeBytes;
                executeObjectTask.setScriptLogOffset(offset);
            }
            logs.add(logService.buildScriptLog(stepInstance, executeObject,
                executeObjectTaskResult.getScreen(), contentSizeBytes, offset));
        }
        // 刷新日志拉取偏移量
        refreshPullLogProgress(executeObjectTaskResult.getScreen(), executeObjectGseKey,
            executeObjectTaskResult.getAtomicTaskId());
    }

    private ExecuteObject findExecuteObject(ExecuteObjectGseKey executeObjectGseKey) {
        return targetExecuteObjectTasks.get(executeObjectGseKey).getExecuteObject();
    }

    private void saveScriptLogContent(List<ServiceExecuteObjectScriptLogDTO> logs) {
        logService.batchWriteScriptLog(taskInstance.getCreateTime(), stepInstanceId, stepInstance.getExecuteCount(),
            stepInstance.getBatch(), logs);
    }

    private void analyseAgentResult(ScriptExecuteObjectTaskResult executeObjectTaskResult,
                                    ExecuteObjectTask executeObjectTask,
                                    ExecuteObjectGseKey executeObjectGseKey,
                                    boolean isUserScriptResult,
                                    long currentTime) {
        boolean isShellScript = stepInstance.getScriptType() == ScriptTypeEnum.SHELL;
        if (executeObjectTask.getStartTime() == null) {
            executeObjectTask.setStartTime(currentTime);
        }
        executeObjectTask.setErrorCode(executeObjectTaskResult.getErrorCode());
        if (GSECode.AtomicErrorCode.getErrorCode(executeObjectTaskResult.getErrorCode())
            == GSECode.AtomicErrorCode.ERROR) {
            // 脚本执行失败
            dealExecuteObjectFinish(executeObjectGseKey, executeObjectTaskResult, executeObjectTask);
            executeObjectTask.setStatus(GseUtils.getStatusByGseErrorCode(executeObjectTaskResult.getErrorCode()));
        } else if (GSECode.AtomicErrorCode.getErrorCode(executeObjectTaskResult.getErrorCode())
            == GSECode.AtomicErrorCode.TERMINATE) {
            dealExecuteObjectFinish(executeObjectGseKey, executeObjectTaskResult, executeObjectTask);
            executeObjectTask.setStatus(ExecuteObjectTaskStatusEnum.GSE_TASK_TERMINATE_SUCCESS);
            this.isTerminatedSuccess = true;
        } else {
            // 分析GSE的返回状态
            GSECode.Status status = GSECode.Status.getStatus(executeObjectTaskResult.getStatus());
            switch (status) {
                case UNSTART:
                    // 0：原子任务已派发；
                case RUNNING:
                    // 1：原子任务执行中；
                    executeObjectTask.setStatus(ExecuteObjectTaskStatusEnum.RUNNING);
                    break;
                case SUCCESS:
                    if (isShellScript && isUserScriptResult) {
                        if (!taskVariablesAnalyzeResult.isExistOnlyConstVar()
                            && (taskVariablesAnalyzeResult.isExistChangeableGlobalVar()
                            || taskVariablesAnalyzeResult.isExistNamespaceVar())) {
                            //对于包含云参或者上下文参数的任务，下发任务的时候包含了2个任务；第一个是执行用户脚本；第二个获取参数的值
                            executeObjectTask.setStatus(ExecuteObjectTaskStatusEnum.RUNNING);
                            refreshPullLogProgress("", executeObjectGseKey, 1);
                        } else {
                            //普通任务，拉取日志，设置为成功
                            dealExecuteObjectFinish(executeObjectGseKey, executeObjectTaskResult, executeObjectTask);
                            executeObjectTask.setStatus(ExecuteObjectTaskStatusEnum.SUCCESS);
                            if (this.targetExecuteObjectGseKeys.contains(executeObjectGseKey)) {
                                successTargetExecuteObjectGseKeys.add(executeObjectGseKey);
                            }
                        }
                    } else {
                        //获取输出参数的任务执行完成，需要分析日志
                        dealExecuteObjectFinish(executeObjectGseKey, executeObjectTaskResult, executeObjectTask);
                        executeObjectTask.setStatus(ExecuteObjectTaskStatusEnum.SUCCESS);
                        if (this.targetExecuteObjectGseKeys.contains(executeObjectGseKey)) {
                            successTargetExecuteObjectGseKeys.add(executeObjectGseKey);
                        }
                        parseVariableValueFromResult(executeObjectTaskResult, executeObjectGseKey);
                    }
                    if (isUserScriptResult) {
                        executeObjectTask.setTag(executeObjectTaskResult.getTag());
                    }
                    break;
                case TIMEOUT:
                    dealExecuteObjectFinish(executeObjectGseKey, executeObjectTaskResult, executeObjectTask);
                    executeObjectTask.setStatus(ExecuteObjectTaskStatusEnum.SCRIPT_TIMEOUT);
                    break;
                case DISCARD:
                    dealExecuteObjectFinish(executeObjectGseKey, executeObjectTaskResult, executeObjectTask);
                    executeObjectTask.setStatus(ExecuteObjectTaskStatusEnum.SCRIPT_TERMINATE);
                    break;
                default:
                    dealExecuteObjectFinish(executeObjectGseKey, executeObjectTaskResult, executeObjectTask);
                    int errCode = executeObjectTaskResult.getErrorCode();
                    int exitCode = executeObjectTaskResult.getExitCode();
                    if (errCode == 0) {
                        if (exitCode != 0) {
                            executeObjectTask.setStatus(ExecuteObjectTaskStatusEnum.SCRIPT_NOT_ZERO_EXIT_CODE);
                        } else {
                            executeObjectTask.setStatus(ExecuteObjectTaskStatusEnum.SCRIPT_FAILED);
                        }
                        executeObjectTask.setTag(executeObjectTaskResult.getTag());
                    }
                    break;
            }
        }
    }

    private void parseVariableValueFromResult(ScriptExecuteObjectTaskResult executeObjectTaskResult,
                                              ExecuteObjectGseKey executeObjectGseKey) {
        if (executeObjectTaskResult.getAtomicTaskId() == 1
            && executeObjectTaskResult.getStatus() == GSECode.Status.SUCCESS.getValue()) {
            String paramsContent = executeObjectTaskResult.getScreen();
            if (!StringUtils.isEmpty(paramsContent)) {
                String[] varKeyAndValueStrArray = paramsContent.split("\n");
                for (String varKeyAndValueStr : varKeyAndValueStrArray) {
                    //用#注释掉的不需要处理
                    if (StringUtils.isBlank(varKeyAndValueStr) || varKeyAndValueStr.startsWith("#")) {
                        continue;
                    }
                    int assignOpPos = varKeyAndValueStr.indexOf("=");//赋值操作符位置(=)
                    if (assignOpPos == -1) {
                        continue;
                    }
                    String paramName = varKeyAndValueStr.substring(0, assignOpPos).trim();
                    String paramValue = varKeyAndValueStr.substring(assignOpPos + 1).trim();
                    TaskVariableDTO taskVar = taskVariablesAnalyzeResult.getTaskVarByVarName(paramName);
                    if (taskVar == null) {
                        continue;
                    }
                    if (taskVariablesAnalyzeResult.isNamespaceVar(paramName)) {
                        Map<String, String> param2Values = namespaceParamValues.computeIfAbsent(
                            executeObjectGseKey, k -> new HashMap<>());
                        param2Values.put(paramName, parseShellEscapeValue(paramValue));
                    } else if (taskVariablesAnalyzeResult.isChangeableGlobalVar(paramName)) {
                        Map<String, String> param2Values = changeableGlobalParamValues.computeIfAbsent(
                            executeObjectGseKey, k -> new HashMap<>());
                        param2Values.put(paramName, parseShellEscapeValue(paramValue));
                    }
                }

            }
        }
    }

    private String parseShellEscapeValue(String value) {
        //从shell转义后的变量值中获取显示值
        if (StringUtils.isBlank(value)) {
            return value;
        }
        if (value.startsWith(("'")) && value.endsWith("'")) {
            value = value.substring(1, value.length() - 1);
        }

        return value.replaceAll("'\\\\''", "'");
    }

    private void saveStepInstanceVariables() {
        boolean mayBeReassigned = (stepInstance.getScriptType() == ScriptTypeEnum.SHELL)
            && (taskVariablesAnalyzeResult.isExistChangeableGlobalVar()
            || taskVariablesAnalyzeResult.isExistNamespaceVar());

        // 仅当shell脚本且包含可变参数的时候，变量值才可能在运行期被改变，才需要保存步骤参数
        if (mayBeReassigned) {
            StepInstanceVariableValuesDTO variableValues = buildStepInstanceVariableValuesResult();
            stepInstanceVariableValueService.saveVariableValues(variableValues);
        }
    }

    private StepInstanceVariableValuesDTO buildStepInstanceVariableValuesResult() {
        StepInstanceVariableValuesDTO result = new StepInstanceVariableValuesDTO();
        List<HostVariableValuesDTO> namespaceVariableValues = buildNamespaceVariableValues();
        List<VariableValueDTO> changeableGlobalVariableValues = buildChangeableGlobalVarValues();
        result.setNamespaceParams(namespaceVariableValues);
        result.setGlobalParams(changeableGlobalVariableValues);
        result.setTaskInstanceId(stepInstance.getTaskInstanceId());
        result.setStepInstanceId(stepInstance.getId());
        result.setExecuteCount(stepInstance.getExecuteCount());
        result.setType(VariableValueTypeEnum.OUTPUT.getValue());
        return result;
    }


    private List<VariableValueDTO> buildChangeableGlobalVarValues() {
        if (changeableGlobalParamValues.isEmpty()) {
            return null;
        }
        List<VariableValueDTO> varValues = null;
        for (Map.Entry<ExecuteObjectGseKey, Map<String, String>> entry : changeableGlobalParamValues.entrySet()) {
            // 如果存在多个主机，那么随机选择其中一个主机上面的参数值
            varValues = toVariableValuesList(entry.getValue());
        }
        return varValues;
    }

    private List<HostVariableValuesDTO> buildNamespaceVariableValues() {
        if (namespaceParamValues.isEmpty()) {
            return null;
        }
        List<HostVariableValuesDTO> hostVariableValuesList = new ArrayList<>();
        for (Map.Entry<ExecuteObjectGseKey, Map<String, String>> entry : namespaceParamValues.entrySet()) {
            HostVariableValuesDTO hostVariableValues = new HostVariableValuesDTO();
            List<VariableValueDTO> paramValues = toVariableValuesList(entry.getValue());
            ExecuteObject executeObject = findExecuteObject((entry.getKey()));
            if (!executeObject.isHostExecuteObject()) {
                continue;
            }
            HostDTO host = executeObject.getHost();
            hostVariableValues.setHostId(host.getHostId());
            hostVariableValues.setCloudIpv4(host.toCloudIp());
            hostVariableValues.setCloudIpv6(host.toCloudIpv6());
            hostVariableValues.setValues(paramValues);
            hostVariableValuesList.add(hostVariableValues);
        }
        return hostVariableValuesList;
    }

    private List<VariableValueDTO> toVariableValuesList(Map<String, String> varNameValueMap) {
        List<VariableValueDTO> variableValues = new ArrayList<>();
        for (Map.Entry<String, String> name2ValEntry : varNameValueMap.entrySet()) {
            VariableValueDTO stepInstVarValue = new VariableValueDTO();
            stepInstVarValue.setName(name2ValEntry.getKey());
            stepInstVarValue.setValue(name2ValEntry.getValue());
            stepInstVarValue.setType(initialVariables.get(name2ValEntry.getKey()).getType());
            variableValues.add(stepInstVarValue);
        }
        return variableValues;
    }

    private void dealExecuteObjectFinish(ExecuteObjectGseKey executeObjectGseKey,
                                         ScriptExecuteObjectTaskResult executeObjectTaskResult,
                                         ExecuteObjectTask executeObjectTask) {
        dealTargetExecuteObjectFinish(executeObjectGseKey, executeObjectTaskResult.getStartTime(),
            executeObjectTaskResult.getEndTime(), executeObjectTask);
        executeObjectTask.setExitCode(getExitCode(executeObjectTaskResult.getExitCode()));
    }

    private int getExitCode(int exitCode) {
        if (exitCode >= 256) {
            return exitCode / 256;
        } else {
            return exitCode;
        }
    }

    /**
     * 刷新拉取日志进度
     *
     * @param context             日志内容
     * @param executeObjectGseKey executeObjectGseKey
     * @param mid                 gse任务的m_id
     */
    private void refreshPullLogProgress(String context, ExecuteObjectGseKey executeObjectGseKey, int mid) {
        int increase = 0;
        if (null != context) {
            increase = context.getBytes(StandardCharsets.UTF_8).length;
        }

        LogPullProgress progress = logPullProgressMap.get(executeObjectGseKey);
        if (null == progress) {
            progress = new LogPullProgress();
            logPullProgressMap.put(executeObjectGseKey, progress);
        }
        int prevMid = progress.getAtomicTaskId();
        if (prevMid != mid) {
            //重置offset
            progress.setByteOffset(0);
        } else {
            progress.setByteOffset(progress.getByteOffset() + increase);
        }
        progress.setExecuteObjectGseKey(executeObjectGseKey);
        progress.setAtomicTaskId(mid);
    }

    /**
     * 分析执行结果
     *
     * @return 任务执行结果
     */
    private GseTaskExecuteResult analyseExecuteResult() {
        GseTaskExecuteResult rst;
        if (isAllTargetExecuteObjectTasksDone()) {
            rst = analyseFinishedExecuteResult();
        } else {
            rst = GseTaskExecuteResult.RUNNING;
        }
        return rst;
    }

    @Override
    protected void saveFailInfoForUnfinishedExecuteObjectTask(ExecuteObjectTaskStatusEnum status, String errorMsg) {
        super.saveFailInfoForUnfinishedExecuteObjectTask(status, errorMsg);
        long endTime = System.currentTimeMillis();
        if (StringUtils.isNotEmpty(errorMsg)) {
            List<ServiceExecuteObjectScriptLogDTO> scriptLogs =
                notFinishedTargetExecuteObjectGseKeys.stream().map(executeObjectGseKey -> {
                    ExecuteObjectTask executeObjectTask = targetExecuteObjectTasks.get(executeObjectGseKey);
                    ExecuteObject executeObject = findExecuteObject(executeObjectGseKey);
                    return logService.buildSystemScriptLog(stepInstance, executeObject, errorMsg,
                        executeObjectTask.getScriptLogOffset(), endTime);
                }).collect(Collectors.toList());
            logService.batchWriteScriptLog(taskInstance.getCreateTime(), stepInstanceId, stepInstance.getExecuteCount(),
                stepInstance.getBatch(), scriptLogs);
        }
    }

    @Override
    public boolean isFinished() {
        return !getExecuteResult().getResultCode().equals(GseTaskExecuteResult.RESULT_CODE_RUNNING);
    }

    @Override
    public ScheduleStrategy getScheduleStrategy() {
        if (this.scheduleStrategy == null) {
            this.scheduleStrategy = new ScriptTaskResultHandleScheduleStrategy();
        }
        return this.scheduleStrategy;
    }

    @Override
    public String toString() {
        if (this.taskInfo == null) {
            this.taskInfo = "ScriptTaskResultHandle-" + stepInstance.getTaskInstanceId() + "-" + stepInstance.getId();
        }
        return this.taskInfo;
    }
}

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

import com.tencent.bk.job.common.gse.GseClient;
import com.tencent.bk.job.common.gse.constants.GSECode;
import com.tencent.bk.job.common.gse.v1.GseReadTimeoutException;
import com.tencent.bk.job.common.gse.v2.model.GetExecuteScriptResultRequest;
import com.tencent.bk.job.common.gse.v2.model.ScriptAgentTaskResult;
import com.tencent.bk.job.common.gse.v2.model.ScriptTaskResult;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.BatchUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.constants.VariableValueTypeEnum;
import com.tencent.bk.job.execute.engine.consts.AgentTaskStatusEnum;
import com.tencent.bk.job.execute.engine.evict.TaskEvictPolicyExecutor;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.model.GseLogBatchPullResult;
import com.tencent.bk.job.execute.engine.model.GseTaskExecuteResult;
import com.tencent.bk.job.execute.engine.model.GseTaskResult;
import com.tencent.bk.job.execute.engine.model.LogPullProgress;
import com.tencent.bk.job.execute.engine.model.ScriptGseTaskResult;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.engine.model.TaskVariablesAnalyzeResult;
import com.tencent.bk.job.execute.engine.result.ha.ResultHandleTaskKeepaliveManager;
import com.tencent.bk.job.execute.engine.util.GseUtils;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.HostVariableValuesDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepInstanceVariableValuesDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.VariableValueDTO;
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
    private static final int MAX_BATCH_SIZE = 1000;
    /**
     * GSE日志查询支持的每一批次的最小Agent数目
     */
    private static final int MIN_BATCH_SIZE = 10;
    /**
     * 每次批次的Agent数目 - 优先选择列表
     */
    private static final int[] BATCH_SIZE_PRIORITY_ARRAY = new int[]{MAX_BATCH_SIZE, 100, MIN_BATCH_SIZE};
    /**
     * 脚本任务执行日志进度, Map<AgentId, LogPullProgress>
     */
    private final Map<String, LogPullProgress> logPullProgressMap = new HashMap<>();
    /**
     * 保存命名空间参数，Map<agentId,Map<paramName,paramValue>>
     */
    private final Map<String, Map<String, String>> namespaceParamValues = new HashMap<>();
    /**
     * 保存可变全局参数，Map<agentId,Map<paramName,paramValue>>
     */
    private final Map<String, Map<String, String>> changeableGlobalParamValues = new HashMap<>();
    /**
     * 脚本任务结果处理调度策略
     */
    private volatile ScheduleStrategy scheduleStrategy;
    /**
     * 目标Agent分批
     */
    private List<List<String>> pullAgentIdBatches = new LinkedList<>();
    /**
     * 当前使用的批次大小
     */
    private volatile int currentBatchSize = MAX_BATCH_SIZE;
    /**
     * 每一轮拉取的批次序号
     */
    private AtomicInteger pullResultBatchesIndex = new AtomicInteger(1);


    /**
     * 任务基本信息，用于日志输出
     */
    private String taskInfo;

    /**
     * ScriptResultHandleTask Constructor
     *
     * @param taskInstance               任务实例
     * @param stepInstance               步骤实例
     * @param taskVariablesAnalyzeResult 任务变量以及分析结果
     * @param agentTaskMap               主机任务执行结果
     * @param gseTask                    gse任务执行结果
     * @param requestId                  请求ID
     */
    public ScriptResultHandleTask(TaskInstanceService taskInstanceService,
                                  GseTaskService gseTaskService,
                                  LogService logService,
                                  TaskInstanceVariableService taskInstanceVariableService,
                                  StepInstanceVariableValueService stepInstanceVariableValueService,
                                  TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
                                  ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager,
                                  TaskEvictPolicyExecutor taskEvictPolicyExecutor,
                                  ScriptAgentTaskService scriptAgentTaskService,
                                  StepInstanceService stepInstanceService,
                                  GseClient gseClient,
                                  TaskInstanceDTO taskInstance,
                                  StepInstanceDTO stepInstance,
                                  TaskVariablesAnalyzeResult taskVariablesAnalyzeResult,
                                  Map<String, AgentTaskDTO> agentTaskMap,
                                  GseTaskDTO gseTask,
                                  String requestId,
                                  List<AgentTaskDTO> agentTasks) {
        super(taskInstanceService,
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
            agentTaskMap,
            gseTask,
            requestId,
            agentTasks);
        initLogPullProcess(agentTaskMap.values());
    }

    private void initLogPullProcess(Collection<AgentTaskDTO> agentTasks) {
        agentTasks.forEach(agentTask -> {
            LogPullProgress process = new LogPullProgress();
            process.setAgentId(agentTask.getAgentId());
            process.setByteOffset(agentTask.getScriptLogOffset());
            process.setAtomicTaskId(0);
            logPullProgressMap.put(agentTask.getAgentId(), process);
        });
    }

    @Override
    GseLogBatchPullResult<ScriptTaskResult> pullGseTaskResultInBatches() {
        if (pullAgentIdBatches.isEmpty()) {
            List<String> queryAgentIdList = new ArrayList<>(notFinishedTargetAgentIds);
            pullAgentIdBatches = BatchUtil.buildBatchList(queryAgentIdList, currentBatchSize);
        }
        return tryPullGseResultWithRetry();
    }

    private GseLogBatchPullResult<ScriptTaskResult> tryPullGseResultWithRetry() {
        List<String> pullLogAgentIds = pullAgentIdBatches.get(pullResultBatchesIndex.get() - 1);
        try {
            ScriptTaskResult result = pullGseTaskResult(pullLogAgentIds);
            boolean isLastBatch = pullResultBatchesIndex.get() == pullAgentIdBatches.size();
            GseLogBatchPullResult<ScriptTaskResult> batchPullResult = new GseLogBatchPullResult<>(true,
                isLastBatch, new ScriptGseTaskResult(result), null);
            if (isLastBatch) {
                resetBatch();
            } else {
                pullResultBatchesIndex.incrementAndGet();
            }
            return batchPullResult;
        } catch (GseReadTimeoutException e) {
            boolean isSuccess = tryReduceBatchSizeAndRebuildBatchList();
            if (isSuccess) {
                log.info("Reduce batch size and rebuild batch list successfully, currentBatchSize: {}, batches: {}. " +
                        "Retry pull!",
                    this.currentBatchSize, this.pullAgentIdBatches);
                return tryPullGseResultWithRetry();
            } else {
                log.warn("Try pull gse log with min batch size, but fail!");
                return new GseLogBatchPullResult<>(false, true, null, "Pull gse task log timeout");
            }
        }
    }

    private void resetBatch() {
        pullResultBatchesIndex = new AtomicInteger(1);
        pullAgentIdBatches.clear();
    }

    private ScriptTaskResult pullGseTaskResult(List<String> agentIds) {
        GetExecuteScriptResultRequest request = new GetExecuteScriptResultRequest();
        request.setGseV2Task(gseV2Task);
        request.setTaskId(gseTask.getGseTaskId());
        agentIds.forEach(agentId -> {
            LogPullProgress progress = logPullProgressMap.get(agentId);
            if (progress == null) {
                request.addAgentTaskQuery(agentId, 0, 0);
            } else {
                request.addAgentTaskQuery(agentId, progress.getAtomicTaskId(), progress.getByteOffset());
            }
        });

        return gseClient.getExecuteScriptResult(request);
    }

    private boolean tryReduceBatchSizeAndRebuildBatchList() {
        log.warn("Caught ReadTimeoutException when pull gse log, try to reduce batch size, currentBatchSize: {}",
            currentBatchSize);
        if (currentBatchSize <= MIN_BATCH_SIZE) {
            return false;
        }
        currentBatchSize = getBatchSizeLessThanCurrentSize();
        List<List<String>> newBatchList = new ArrayList<>();
        List<String> leftAgentIds = new ArrayList<>();
        if (pullResultBatchesIndex.get() > 1) {
            List<List<String>> pullFinishedBatchList = pullAgentIdBatches.subList(0, pullResultBatchesIndex.get() - 1);
            newBatchList.addAll(pullFinishedBatchList);
        }
        pullAgentIdBatches.subList(pullResultBatchesIndex.get() - 1, pullAgentIdBatches.size())
            .forEach(leftAgentIds::addAll);
        newBatchList.addAll(BatchUtil.buildBatchList(leftAgentIds, currentBatchSize));
        pullAgentIdBatches = newBatchList;
        return true;
    }

    private int getBatchSizeLessThanCurrentSize() {
        for (int batchSize : BATCH_SIZE_PRIORITY_ARRAY) {
            if (batchSize < currentBatchSize) {
                return batchSize;
            }
        }
        return currentBatchSize;
    }

    @Override
    GseTaskExecuteResult analyseGseTaskResult(GseTaskResult<ScriptTaskResult> taskDetail) {
        if (taskDetail == null || taskDetail.getResult() == null) {
            log.info("[{}] Analyse gse task result, result is empty!", gseTask.getTaskUniqueName());
            return analyseExecuteResult();
        }
        long currentTime = DateUtils.currentTimeMillis(); // 当前时间
        List<ServiceScriptLogDTO> scriptLogs = new ArrayList<>();
        StopWatch watch = new StopWatch("analyse-gse-script-task");
        watch.start("analyse");
        for (ScriptAgentTaskResult agentTaskResult : taskDetail.getResult().getResult()) {
            log.info("[{}]: Analyse agent task result, result: {}",
                gseTask.getTaskUniqueName(), agentTaskResult);

            /*为了解决shell上下文传参的问题，在下发用户脚本的时候，实际上下下发两个脚本。第一个脚本是用户脚本，第二个脚本
             *是获取上下文参数的脚本。所以m_id=0的是用户脚本的执行日志，需要分析记录；m_id=1的，则是获取上下文参数
             *输出的日志内容，不需要记录，仅需要从日志分析提取上下文参数*/
            boolean isUserScriptResult = agentTaskResult.getAtomicTaskId() == 0;
            String agentId = agentTaskResult.getAgentId();

            // 该Agent已经日志分析结束，不要再分析
            if (this.analyseFinishedTargetAgentIds.contains(agentId)) {
                continue;
            }
            AgentTaskDTO agentTask = targetAgentTasks.get(agentId);
            if (agentTask == null) {
                continue;
            }

            if (isUserScriptResult) {
                addScriptLogsAndRefreshPullProgress(scriptLogs, agentTaskResult, agentId, agentTask, currentTime);
            }

            analyseAgentResult(agentTaskResult, agentTask, agentId, isUserScriptResult, currentTime);
        }
        watch.stop();

        watch.start("saveScriptContent");
        saveScriptLogContent(scriptLogs);
        watch.stop();

        watch.start("saveGseAgentTasks");
        batchSaveChangedGseAgentTasks(targetAgentTasks.values());
        watch.stop();

        log.info("[{}] Analyse gse task result -> notFinishedTargetAgentIds={}, analyseFinishedTargetAgentIds={}",
            this.gseTask.getTaskUniqueName(), this.notFinishedTargetAgentIds, this.analyseFinishedTargetAgentIds);

        GseTaskExecuteResult rst = analyseExecuteResult();
        if (!rst.getResultCode().equals(GseTaskExecuteResult.RESULT_CODE_RUNNING)) {
            watch.start("saveVariables");
            saveStepInstanceVariables();
            watch.stop();
        }

        if (watch.getTotalTimeMillis() > 1000L) {
            log.info("[{}] Analyse script gse task is slow, statistics: {}", gseTask.getTaskUniqueName(),
                watch.prettyPrint());
        }
        return rst;
    }

    private void addScriptLogsAndRefreshPullProgress(List<ServiceScriptLogDTO> logs,
                                                     ScriptAgentTaskResult agentTaskResult,
                                                     String agentId,
                                                     AgentTaskDTO agentTask,
                                                     long currentTime) {
        HostDTO host = agentIdHostMap.get(agentTask.getAgentId());
        if (GSECode.AtomicErrorCode.getErrorCode(agentTaskResult.getErrorCode()) == GSECode.AtomicErrorCode.ERROR) {
            logs.add(logService.buildSystemScriptLog(host,
                agentTaskResult.getErrorMsg(), agentTask.getScriptLogOffset(), currentTime));
        } else {
            String content = agentTaskResult.getScreen();
            if (StringUtils.isEmpty(content)) {
                return;
            }
            int offset = agentTask.getScriptLogOffset();
            if (StringUtils.isNotEmpty(content)) {
                int bytes = content.getBytes(StandardCharsets.UTF_8).length;
                offset += bytes;
                agentTask.setScriptLogOffset(offset);
            }
            logs.add(new ServiceScriptLogDTO(host, offset, agentTaskResult.getScreen()));
        }
        // 刷新日志拉取偏移量
        refreshPullLogProgress(agentTaskResult.getScreen(), agentId, agentTaskResult.getAtomicTaskId());
    }

    private void saveScriptLogContent(List<ServiceScriptLogDTO> logs) {
        logService.batchWriteScriptLog(taskInstance.getCreateTime(), stepInstanceId, stepInstance.getExecuteCount(),
            stepInstance.getBatch(), logs);
    }

    private void analyseAgentResult(ScriptAgentTaskResult agentResult, AgentTaskDTO agentTask, String agentId,
                                    boolean isUserScriptResult, long currentTime) {
        boolean isShellScript = (stepInstance.getScriptType().equals(ScriptTypeEnum.SHELL.getValue()));
        if (agentTask.getStartTime() == null) {
            agentTask.setStartTime(currentTime);
        }
        agentTask.setErrorCode(agentResult.getErrorCode());
        if (GSECode.AtomicErrorCode.getErrorCode(agentResult.getErrorCode()) == GSECode.AtomicErrorCode.ERROR) {
            // 脚本执行失败
            dealAgentFinish(agentId, agentResult, agentTask);
            agentTask.setStatus(GseUtils.getStatusByGseErrorCode(agentResult.getErrorCode()));
        } else if (GSECode.AtomicErrorCode.getErrorCode(agentResult.getErrorCode())
            == GSECode.AtomicErrorCode.TERMINATE) {
            dealAgentFinish(agentId, agentResult, agentTask);
            agentTask.setStatus(AgentTaskStatusEnum.GSE_TASK_TERMINATE_SUCCESS);
            this.isTerminatedSuccess = true;
        } else {
            // 分析GSE的返回状态
            GSECode.Status status = GSECode.Status.getStatus(agentResult.getStatus());
            switch (status) {
                case UNSTART:
                    // 0：原子任务已派发；
                case RUNNING:
                    // 1：原子任务执行中；
                    agentTask.setStatus(AgentTaskStatusEnum.RUNNING);
                    break;
                case SUCCESS:
                    if (isShellScript && isUserScriptResult) {
                        if (!taskVariablesAnalyzeResult.isExistOnlyConstVar()
                            && (taskVariablesAnalyzeResult.isExistChangeableGlobalVar()
                            || taskVariablesAnalyzeResult.isExistNamespaceVar())) {
                            //对于包含云参或者上下文参数的任务，下发任务的时候包含了2个任务；第一个是执行用户脚本；第二个获取参数的值
                            agentTask.setStatus(AgentTaskStatusEnum.RUNNING);
                            refreshPullLogProgress("", agentId, 1);
                        } else {
                            //普通任务，拉取日志，设置为成功
                            dealAgentFinish(agentId, agentResult, agentTask);
                            agentTask.setStatus(AgentTaskStatusEnum.SUCCESS);
                            if (this.targetAgentIds.contains(agentId)) {
                                successTargetAgentIds.add(agentId);
                            }
                        }
                    } else {
                        //获取输出参数的任务执行完成，需要分析日志
                        dealAgentFinish(agentId, agentResult, agentTask);
                        agentTask.setStatus(AgentTaskStatusEnum.SUCCESS);
                        if (this.targetAgentIds.contains(agentId)) {
                            successTargetAgentIds.add(agentId);
                        }
                        parseVariableValueFromResult(agentResult, agentId);
                    }
                    if (isUserScriptResult) {
                        agentTask.setTag(agentResult.getTag());
                    }
                    break;
                case TIMEOUT:
                    dealAgentFinish(agentId, agentResult, agentTask);
                    agentTask.setStatus(AgentTaskStatusEnum.SCRIPT_TIMEOUT);
                    break;
                case DISCARD:
                    dealAgentFinish(agentId, agentResult, agentTask);
                    agentTask.setStatus(AgentTaskStatusEnum.SCRIPT_TERMINATE);
                    break;
                default:
                    dealAgentFinish(agentId, agentResult, agentTask);
                    int errCode = agentResult.getErrorCode();
                    int exitCode = agentResult.getExitCode();
                    if (errCode == 0) {
                        if (exitCode != 0) {
                            agentTask.setStatus(AgentTaskStatusEnum.SCRIPT_NOT_ZERO_EXIT_CODE);
                        } else {
                            agentTask.setStatus(AgentTaskStatusEnum.SCRIPT_FAILED);
                        }
                        agentTask.setTag(agentResult.getTag());
                    }
                    break;
            }
        }
    }

    private void parseVariableValueFromResult(ScriptAgentTaskResult agentTaskResult, String agentId) {
        if (agentTaskResult.getAtomicTaskId() == 1
            && agentTaskResult.getStatus() == GSECode.Status.SUCCESS.getValue()) {
            String paramsContent = agentTaskResult.getScreen();
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
                        Map<String, String> param2Values = namespaceParamValues.computeIfAbsent(agentId,
                            k -> new HashMap<>());
                        param2Values.put(paramName, parseShellEscapeValue(paramValue));
                    } else if (taskVariablesAnalyzeResult.isChangeableGlobalVar(paramName)) {
                        Map<String, String> param2Values = changeableGlobalParamValues.computeIfAbsent(agentId,
                            k -> new HashMap<>());
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
        boolean mayBeReassigned = (stepInstance.getScriptType().equals(ScriptTypeEnum.SHELL.getValue()))
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
        for (Map.Entry<String, Map<String, String>> entry : changeableGlobalParamValues.entrySet()) {
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
        for (Map.Entry<String, Map<String, String>> entry : namespaceParamValues.entrySet()) {
            HostVariableValuesDTO hostVariableValues = new HostVariableValuesDTO();
            List<VariableValueDTO> paramValues = toVariableValuesList(entry.getValue());
            HostDTO host = agentIdHostMap.get(entry.getKey());
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

    private void dealAgentFinish(String agentId, ScriptAgentTaskResult agentTaskResult, AgentTaskDTO agentTask) {
        dealTargetAgentFinish(agentId, agentTaskResult.getStartTime(), agentTaskResult.getEndTime(), agentTask);
        agentTask.setExitCode(getExitCode(agentTaskResult.getExitCode()));
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
     * @param context 日志内容
     * @param agentId bk_agent_id
     * @param mid     gse任务的m_id
     */
    private void refreshPullLogProgress(String context, String agentId, int mid) {
        int increase = 0;
        if (null != context) {
            increase = context.getBytes(StandardCharsets.UTF_8).length;
        }

        LogPullProgress progress = logPullProgressMap.get(agentId);
        if (null == progress) {
            progress = new LogPullProgress();
            logPullProgressMap.put(agentId, progress);
        }
        int prevMid = progress.getAtomicTaskId();
        if (prevMid != mid) {
            //重置offset
            progress.setByteOffset(0);
        } else {
            progress.setByteOffset(progress.getByteOffset() + increase);
        }
        progress.setAgentId(agentId);
        progress.setAtomicTaskId(mid);
    }

    /**
     * 分析执行结果
     *
     * @return 任务执行结果
     */
    private GseTaskExecuteResult analyseExecuteResult() {
        GseTaskExecuteResult rst;
        if (isAllTargetAgentTasksDone()) {
            rst = analyseFinishedExecuteResult();
        } else {
            rst = GseTaskExecuteResult.RUNNING;
        }
        return rst;
    }

    @Override
    protected void saveFailInfoForUnfinishedAgentTask(AgentTaskStatusEnum status, String errorMsg) {
        super.saveFailInfoForUnfinishedAgentTask(status, errorMsg);
        long endTime = System.currentTimeMillis();
        if (StringUtils.isNotEmpty(errorMsg)) {
            List<ServiceScriptLogDTO> scriptLogs = notFinishedTargetAgentIds.stream().map(agentId -> {
                AgentTaskDTO agentTask = targetAgentTasks.get(agentId);
                HostDTO host = agentIdHostMap.get(agentId);
                return logService.buildSystemScriptLog(host, errorMsg, agentTask.getScriptLogOffset(), endTime);
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

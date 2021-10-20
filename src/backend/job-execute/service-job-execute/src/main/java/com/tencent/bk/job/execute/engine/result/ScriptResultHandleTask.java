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

import com.tencent.bk.gse.taskapi.api_agent_task_rst;
import com.tencent.bk.gse.taskapi.api_query_task_info_v2;
import com.tencent.bk.gse.taskapi.api_task_detail_result;
import com.tencent.bk.job.common.util.BatchUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.common.exception.ReadTimeoutException;
import com.tencent.bk.job.execute.constants.VariableValueTypeEnum;
import com.tencent.bk.job.execute.engine.consts.GSECode;
import com.tencent.bk.job.execute.engine.consts.IpStatus;
import com.tencent.bk.job.execute.engine.gse.GseRequestPrinter;
import com.tencent.bk.job.execute.engine.gse.GseRequestUtils;
import com.tencent.bk.job.execute.engine.model.GseLog;
import com.tencent.bk.job.execute.engine.model.GseLogBatchPullResult;
import com.tencent.bk.job.execute.engine.model.GseTaskExecuteResult;
import com.tencent.bk.job.execute.engine.model.LogPullProgress;
import com.tencent.bk.job.execute.engine.model.ScriptTaskLog;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.engine.model.TaskVariablesAnalyzeResult;
import com.tencent.bk.job.execute.engine.util.Utils;
import com.tencent.bk.job.execute.model.GseTaskIpLogDTO;
import com.tencent.bk.job.execute.model.GseTaskLogDTO;
import com.tencent.bk.job.execute.model.HostVariableValuesDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepInstanceVariableValuesDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.VariableValueDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceScriptLogDTO;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 脚本任务执行结果处理
 */
@Slf4j
public class ScriptResultHandleTask extends AbstractResultHandleTask<api_task_detail_result> {
    /**
     * GSE日志查询支持的每一批次的最大IP数目
     */
    private static final int MAX_BATCH_SIZE = 1000;
    /**
     * GSE日志查询支持的每一批次的最小IP数目
     */
    private static final int MIN_BATCH_SIZE = 10;
    /**
     * 每次批次的IP数目 - 优先选择列表
     */
    private static final int[] BATCH_SIZE_PRIORITY_ARRAY = new int[]{MAX_BATCH_SIZE, 100, MIN_BATCH_SIZE};
    /**
     * 脚本任务执行日志进度
     */
    private Map<String, LogPullProgress> logPullProgressMap = new HashMap<>();
    /**
     * 保存命名空间参数，Map<ip,Map<paramName,paramValue>>
     */
    private Map<String, Map<String, String>> namespaceParamValues = new HashMap<>();
    /**
     * 保存可变全局参数，Map<ip,Map<paramName,paramValue>>
     */
    private Map<String, Map<String, String>> changeableGlobalParamValues = new HashMap<>();
    /**
     * 脚本任务结果处理调度策略
     */
    private volatile ScheduleStrategy scheduleStrategy;
    /**
     * 目标IP分批
     */
    private List<List<String>> pullIpBatches = new LinkedList<>();
    /**
     * 当前使用的批次大小
     */
    private volatile int currentBatchSize = MAX_BATCH_SIZE;
    /**
     * 每一轮拉取的批次序号
     */
    private AtomicInteger pullIpBatchesIndex = new AtomicInteger(1);


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
     * @param ipLogMap                   主机任务执行结果
     * @param gseTaskLog                 gse任务执行结果
     * @param targetIps                  目标主机ip
     * @param requestId                  请求ID
     */
    public ScriptResultHandleTask(TaskInstanceDTO taskInstance,
                                  StepInstanceDTO stepInstance,
                                  TaskVariablesAnalyzeResult taskVariablesAnalyzeResult,
                                  Map<String, GseTaskIpLogDTO> ipLogMap,
                                  GseTaskLogDTO gseTaskLog,
                                  Set<String> targetIps,
                                  String requestId) {
        super(taskInstance, stepInstance, taskVariablesAnalyzeResult, ipLogMap, gseTaskLog, targetIps, requestId);
        initLogPullProcess(ipLogMap.values());
    }

    private void initLogPullProcess(Collection<GseTaskIpLogDTO> ipLogs) {
        ipLogs.forEach(ipLog -> {
            LogPullProgress process = new LogPullProgress();
            process.setIp(ipLog.getCloudAreaAndIp());
            process.setByteOffset(ipLog.getOffset());
            process.setMid(0);
            logPullProgressMap.put(ipLog.getCloudAreaAndIp(), process);
        });
    }

    @Override
    GseLogBatchPullResult<api_task_detail_result> pullGseTaskLogInBatches() {
        if (pullIpBatches.isEmpty()) {
            Set<String> queryIpSet = new HashSet<>();
            queryIpSet.addAll(notStartedIpSet);
            queryIpSet.addAll(runningIpSet);
            List<String> queryIpList = new ArrayList<>(queryIpSet);
            pullIpBatches = BatchUtil.buildBatchList(queryIpList, currentBatchSize);
        }
        return tryPullGseLogWithRetry();
    }

    private GseLogBatchPullResult<api_task_detail_result> tryPullGseLogWithRetry() {
        List<String> pullLogIps = pullIpBatches.get(pullIpBatchesIndex.get() - 1);
        try {
            api_task_detail_result detailRst = pullGseLog(pullLogIps);
            boolean isLastBatch = pullIpBatchesIndex.get() == pullIpBatches.size();
            GseLogBatchPullResult<api_task_detail_result> batchPullResult = new GseLogBatchPullResult<>(true,
                isLastBatch, new ScriptTaskLog(detailRst), null);
            if (isLastBatch) {
                resetBatch();
            } else {
                pullIpBatchesIndex.incrementAndGet();
            }
            return batchPullResult;
        } catch (ReadTimeoutException e) {
            boolean isSuccess = tryReduceBatchSizeAndRebuildBatchList();
            if (isSuccess) {
                log.info("Reduce batch size and rebuild batch list successfully, currentBatchSize: {}, batches: {}. " +
                        "Retry pull!",
                    this.currentBatchSize, this.pullIpBatches);
                return tryPullGseLogWithRetry();
            } else {
                log.warn("Try pull gse log with min batch size, but fail!");
                return new GseLogBatchPullResult<>(false, true, null, "Pull gse task log timeout");
            }
        }
    }

    private void resetBatch() {
        pullIpBatchesIndex = new AtomicInteger(1);
        pullIpBatches.clear();
    }

    private api_task_detail_result pullGseLog(List<String> ips) {
        api_query_task_info_v2 requestV2 = GseRequestUtils.buildScriptLogRequestV2(gseTaskLog.getGseTaskId(), ips,
            logPullProgressMap);
        return GseRequestUtils.getScriptTaskDetailRst(stepInstanceId, requestV2);
    }

    private boolean tryReduceBatchSizeAndRebuildBatchList() {
        log.warn("Caught ReadTimeoutException when pull gse log, try to reduce batch size, currentBatchSize: {}",
            currentBatchSize);
        if (currentBatchSize <= MIN_BATCH_SIZE) {
            return false;
        }
        currentBatchSize = getBatchSizeLessThanCurrentSize();
        List<List<String>> newBatchList = new ArrayList<>();
        List<String> leftIps = new ArrayList<>();
        if (pullIpBatchesIndex.get() > 1) {
            List<List<String>> pullFinishedBatchList = pullIpBatches.subList(0, pullIpBatchesIndex.get() - 1);
            newBatchList.addAll(pullFinishedBatchList);
        }
        pullIpBatches.subList(pullIpBatchesIndex.get() - 1, pullIpBatches.size()).forEach(leftIps::addAll);
        newBatchList.addAll(BatchUtil.buildBatchList(leftIps, currentBatchSize));
        pullIpBatches = newBatchList;
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
    GseTaskExecuteResult analyseGseTaskLog(GseLog<api_task_detail_result> taskDetail) {

        long currentTime = DateUtils.currentTimeMillis(); // 当前时间
        Set<String> analysedIpSet = new HashSet<>();
        List<ServiceScriptLogDTO> scriptLogs = new ArrayList<>();
        StopWatch watch = new StopWatch("analyse-gse-script-task");
        watch.start("analyse");
        for (api_agent_task_rst ipResult : taskDetail.getGseLog().getResult()) {
            log.info("[{}]: ipResult={}", stepInstanceId, GseRequestPrinter.printAgentTaskResult(ipResult));

            /*为了解决shell上下文传参的问题，在下发用户脚本的时候，实际上下下发两个脚本。第一个脚本是用户脚本，第二个脚本
             *是获取上下文参数的脚本。所以m_id=0的是用户脚本的执行日志，需要分析记录；m_id=1的，则是获取上下文参数
             *输出的日志内容，不需要记录，仅需要从日志分析提取上下文参数*/
            boolean isUserScriptResult = ipResult.getAtomic_task_id() == 0;
            String cloudIp = ipResult.getGse_composite_id() + ":" + ipResult.getIp();

            // 该ip已经日志分析结束，不要再分析
            if (this.analyseFinishedIpSet.contains(cloudIp)) {
                continue;
            }
            GseTaskIpLogDTO ipLog = ipLogMap.get(cloudIp);
            if (ipLog == null) {
                continue;
            }
            analysedIpSet.add(cloudIp);

            if (isUserScriptResult) {
                addScriptLogsAndRefreshPullProgress(scriptLogs, ipResult, cloudIp, ipLog, currentTime);
            }

            analyseIpResult(ipResult, ipLog, cloudIp, isUserScriptResult, currentTime);
        }
        watch.stop();

        watch.start("saveScriptContent");
        saveScriptLogContent(scriptLogs);
        watch.stop();

        watch.start("saveIpLogs");
        batchSaveChangedIpLogs();
        watch.stop();

        GseTaskExecuteResult rst = analyseExecuteResult();
        if (!rst.getResultCode().equals(GseTaskExecuteResult.RESULT_CODE_RUNNING)) {
            watch.start("saveVariables");
            saveStepInstanceVariables();
            watch.stop();
        }

        if (watch.getTotalTimeMillis() > 1000L) {
            log.info("Analyse script gse task is slow, statistics: {}", watch.prettyPrint());
        }
        return rst;
    }

    private void addScriptLogsAndRefreshPullProgress(List<ServiceScriptLogDTO> logs, api_agent_task_rst ipResult,
                                                     String cloudIp, GseTaskIpLogDTO ipLog, long currentTime) {
        if (GSECode.AtomicErrorCode.getErrorCode(ipResult.getBk_error_code()) == GSECode.AtomicErrorCode.ERROR) {
            logs.add(logService.buildSystemScriptLog(cloudIp, ipResult.getBk_error_msg(), ipLog.getOffset(),
                currentTime));
        } else {
            String content = ipResult.getScreen();
            if (StringUtils.isEmpty(content)) {
                return;
            }
            int offset = ipLog.getOffset();
            if (StringUtils.isNotEmpty(content)) {
                int bytes = content.getBytes(StandardCharsets.UTF_8).length;
                offset += bytes;
                ipLog.setOffset(offset);
            }
            logs.add(new ServiceScriptLogDTO(cloudIp, offset, ipResult.getScreen()));
        }
        // 刷新日志拉取偏移量
        refreshPullLogProgress(ipResult.getScreen(), cloudIp, ipResult.getAtomic_task_id());
    }

    private void saveScriptLogContent(List<ServiceScriptLogDTO> logs) {
        logService.batchWriteScriptLog(DateUtils.formatUnixTimestamp(taskInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC")), stepInstanceId, stepInstance.getExecuteCount(),
            logs);
    }

    private void analyseIpResult(api_agent_task_rst ipResult, GseTaskIpLogDTO ipLog, String cloudIp,
                                 boolean isUserScriptResult, long currentTime) {
        boolean isShellScript = (stepInstance.getScriptType().equals(ScriptTypeEnum.SHELL.getValue()));
        if (ipLog.getStartTime() == null) {
            ipLog.setStartTime(currentTime);
        }
        ipLog.setErrCode(ipResult.getBk_error_code());
        if (GSECode.AtomicErrorCode.getErrorCode(ipResult.getBk_error_code()) == GSECode.AtomicErrorCode.ERROR) {
            // 脚本执行失败
            dealIPFinish(cloudIp, ipResult, ipLog);
            int ipStatus = Utils.getStatusByGseErrorCode(ipResult.getBk_error_code());
            if (ipStatus < 0) {
                ipStatus = IpStatus.FAILED.getValue();
            }
            ipLog.setStatus(ipStatus);
        } else if (GSECode.AtomicErrorCode.getErrorCode(ipResult.getBk_error_code())
            == GSECode.AtomicErrorCode.TERMINATE) {
            dealIPFinish(cloudIp, ipResult, ipLog);
            ipLog.setStatus(IpStatus.GSE_TASK_TERMINATE_SUCCESS.getValue());
            this.isTerminatedSuccess = true;
        } else {
            // 分析GSE的返回状态
            GSECode.Status status = GSECode.Status.getStatus(ipResult.getStatus());
            switch (status) {
                case UNSTART:
                    // 0：原子任务已派发；
                case RUNNING:
                    // 1：原子任务执行中；
                    notStartedIpSet.remove(cloudIp);
                    runningIpSet.add(cloudIp);
                    ipLog.setStatus(IpStatus.RUNNING.getValue());
                    break;
                case SUCCESS:
                    if (isShellScript && isUserScriptResult) {
                        if (!taskVariablesAnalyzeResult.isExistOnlyConstVar()
                            && (taskVariablesAnalyzeResult.isExistChangeableGlobalVar()
                            || taskVariablesAnalyzeResult.isExistNamespaceVar())) {
                            //对于包含云参或者上下文参数的任务，下发任务的时候包含了2个任务；第一个是执行用户脚本；第二个获取参数的值
                            ipLog.setStatus(IpStatus.RUNNING.getValue());
                            notStartedIpSet.remove(cloudIp);
                            runningIpSet.add(cloudIp);
                            refreshPullLogProgress("", cloudIp, 1);
                        } else {
                            //普通任务，拉取日志，设置为成功
                            dealIPFinish(cloudIp, ipResult, ipLog);
                            ipLog.setStatus(IpStatus.SUCCESS.getValue());
                            if (this.targetIpSet.contains(cloudIp)) {
                                successIpSet.add(cloudIp);
                            }
                        }
                    } else {
                        //获取输出参数的任务执行完成，需要分析日志
                        dealIPFinish(cloudIp, ipResult, ipLog);
                        ipLog.setStatus(IpStatus.SUCCESS.getValue());
                        if (this.targetIpSet.contains(cloudIp)) {
                            successIpSet.add(cloudIp);
                        }
                        parseVariableValueFromResult(ipResult, cloudIp);
                    }
                    if (isUserScriptResult) {
                        ipLog.setTag(ipResult.getTag());
                    }
                    break;
                case TIMEOUT:
                    dealIPFinish(cloudIp, ipResult, ipLog);
                    ipLog.setStatus(IpStatus.SCRIPT_TIMEOUT.getValue());
                    break;
                case DISCARD:
                    dealIPFinish(cloudIp, ipResult, ipLog);
                    ipLog.setStatus(IpStatus.SCRIPT_TERMINATE.getValue());
                    break;
                default:
                    dealIPFinish(cloudIp, ipResult, ipLog);
                    int errCode = ipResult.getBk_error_code();
                    int exitCode = getExitCode(ipResult.getExitcode());
                    if (errCode == 0) {
                        if (exitCode != 0) {
                            ipLog.setStatus(IpStatus.SCRIPT_NOT_ZERO_EXIT_CODE.getValue());
                        } else {
                            ipLog.setStatus(IpStatus.SCRIPT_FAILED.getValue());
                        }
                        ipLog.setTag(ipResult.getTag());
                    }
                    break;
            }
        }
    }

    private void parseVariableValueFromResult(api_agent_task_rst ipResult, String ip) {
        if (ipResult.getAtomic_task_id() == 1 && ipResult.getStatus() == GSECode.Status.SUCCESS.getValue()) {
            String paramsContent = ipResult.getScreen();
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
                        Map<String, String> param2Values = namespaceParamValues.computeIfAbsent(ip,
                            k -> new HashMap<>());
                        param2Values.put(paramName, parseShellEscapeValue(paramValue));
                    } else if (taskVariablesAnalyzeResult.isChangeableGlobalVar(paramName)) {
                        Map<String, String> param2Values = changeableGlobalParamValues.computeIfAbsent(ip,
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
            String ip = entry.getKey();
            hostVariableValues.setIp(ip);
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

    private void dealIPFinish(String ip, api_agent_task_rst ipResult, GseTaskIpLogDTO ipLog) {
        dealIPFinish(ip, ipResult.getStart_time(), ipResult.getEnd_time(), ipLog);
        ipLog.setExitCode(getExitCode(ipResult.getExitcode()));
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
     * @param ip      agent ip
     * @param mid     gse任务的m_id
     */
    private void refreshPullLogProgress(String context, String ip, int mid) {
        int increase = 0;
        if (null != context) {
            increase = context.getBytes(StandardCharsets.UTF_8).length;
        }

        LogPullProgress progress = logPullProgressMap.get(ip);
        if (null == progress) {
            progress = new LogPullProgress();
            logPullProgressMap.put(ip, progress);
        }
        int prevMid = progress.getMid();
        if (prevMid != mid) {
            //重置offset
            progress.setByteOffset(0);
        } else {
            progress.setByteOffset(progress.getByteOffset() + increase);
        }
        progress.setIp(ip);
        progress.setMid(mid);
    }

    /*
     * 分析执行结果
     * @return 任务执行结果
     */
    private GseTaskExecuteResult analyseExecuteResult() {
        GseTaskExecuteResult rst;
        if (this.notStartedIpSet.isEmpty() && this.runningIpSet.isEmpty()) {
            int targetIPNum = this.targetIpSet.size();
            int successTargetIpNum = this.successIpSet.size();
            boolean isSuccess = this.invalidIpSet.isEmpty() && successTargetIpNum == targetIPNum;
            if (isSuccess) {
                rst = GseTaskExecuteResult.SUCCESS;
            } else {
                if (this.isTerminatedSuccess) {
                    rst = GseTaskExecuteResult.STOP_SUCCESS;
                } else {
                    rst = GseTaskExecuteResult.FAILED;
                }
            }
        } else {
            rst = GseTaskExecuteResult.RUNNING;
        }
        return rst;
    }

    @Override
    protected void saveFailInfoForUnfinishedIpTask(int errorType, String errorMsg) {
        super.saveFailInfoForUnfinishedIpTask(errorType, errorMsg);
        long endTime = System.currentTimeMillis();
        Set<String> unfinishedIPSet = new HashSet<>();
        unfinishedIPSet.addAll(notStartedIpSet);
        unfinishedIPSet.addAll(this.runningIpSet);
        if (StringUtils.isNotEmpty(errorMsg)) {
            logService.batchWriteJobSystemScriptLog(taskInstance.getCreateTime(), stepInstanceId,
                stepInstance.getExecuteCount(), buildIpAndLogOffsetMap(unfinishedIPSet), errorMsg, endTime);
        }
    }

    private Map<String, Integer> buildIpAndLogOffsetMap(Collection<String> ips) {
        Map<String, Integer> ipAndLogOffsetMap = new HashMap<>();
        ips.forEach(ip -> {
            GseTaskIpLogDTO ipLog = ipLogMap.get(ip);
            if (ipLog != null) {
                ipAndLogOffsetMap.put(ip, ipLog.getOffset());
            } else {
                ipAndLogOffsetMap.put(ip, 0);
            }
        });
        return ipAndLogOffsetMap;
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

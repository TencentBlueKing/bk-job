package com.tencent.bk.job.common.gse.v1;

import com.tencent.bk.gse.cacheapi.AgentStatusRequestInfo;
import com.tencent.bk.gse.cacheapi.AgentStatusResponse;
import com.tencent.bk.gse.cacheapi.CacheIpInfo;
import com.tencent.bk.gse.cacheapi.CacheUser;
import com.tencent.bk.gse.taskapi.api_agent;
import com.tencent.bk.gse.taskapi.api_auth;
import com.tencent.bk.gse.taskapi.api_auto_task;
import com.tencent.bk.gse.taskapi.api_base_file_info;
import com.tencent.bk.gse.taskapi.api_comm_rsp;
import com.tencent.bk.gse.taskapi.api_copy_fileinfoV2;
import com.tencent.bk.gse.taskapi.api_host;
import com.tencent.bk.gse.taskapi.api_map_rsp;
import com.tencent.bk.gse.taskapi.api_query_agent_info_v2;
import com.tencent.bk.gse.taskapi.api_query_atom_task_info;
import com.tencent.bk.gse.taskapi.api_query_task_info_v2;
import com.tencent.bk.gse.taskapi.api_script_file;
import com.tencent.bk.gse.taskapi.api_script_request;
import com.tencent.bk.gse.taskapi.api_stop_task_request;
import com.tencent.bk.gse.taskapi.api_task_detail_result;
import com.tencent.bk.gse.taskapi.api_task_relation;
import com.tencent.bk.gse.taskapi.api_task_request;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.gse.IGseClient;
import com.tencent.bk.job.common.gse.constants.AgentStateStatusEnum;
import com.tencent.bk.job.common.gse.constants.FileDistModeEnum;
import com.tencent.bk.job.common.gse.constants.GseConstants;
import com.tencent.bk.job.common.gse.constants.GseTaskTypeEnum;
import com.tencent.bk.job.common.gse.util.FilePathUtils;
import com.tencent.bk.job.common.gse.util.WindowsHelper;
import com.tencent.bk.job.common.gse.v1.model.AgentStatusDTO;
import com.tencent.bk.job.common.gse.v1.model.CopyFileRsp;
import com.tencent.bk.job.common.gse.v1.model.GSEFileTaskResult;
import com.tencent.bk.job.common.gse.v2.model.Agent;
import com.tencent.bk.job.common.gse.v2.model.AtomicFileTaskResult;
import com.tencent.bk.job.common.gse.v2.model.AtomicFileTaskResultContent;
import com.tencent.bk.job.common.gse.v2.model.ExecuteScriptRequest;
import com.tencent.bk.job.common.gse.v2.model.FileTaskResult;
import com.tencent.bk.job.common.gse.v2.model.FileTransferTask;
import com.tencent.bk.job.common.gse.v2.model.GetExecuteScriptResultRequest;
import com.tencent.bk.job.common.gse.v2.model.GetTransferFileResultRequest;
import com.tencent.bk.job.common.gse.v2.model.GseTaskResponse;
import com.tencent.bk.job.common.gse.v2.model.ScriptAgentTaskResult;
import com.tencent.bk.job.common.gse.v2.model.ScriptTaskResult;
import com.tencent.bk.job.common.gse.v2.model.SourceFile;
import com.tencent.bk.job.common.gse.v2.model.TargetFile;
import com.tencent.bk.job.common.gse.v2.model.TerminateGseTaskRequest;
import com.tencent.bk.job.common.gse.v2.model.TransferFileRequest;
import com.tencent.bk.job.common.gse.v2.model.req.ListAgentStateReq;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * GSE V1 api client
 */
@Slf4j
public class GseV1ApiClient implements IGseClient {

    private final MeterRegistry meterRegistry;
    private final GseCacheClientFactory gseCacheClientFactory;

    public GseV1ApiClient(MeterRegistry meterRegistry,
                          GseCacheClientFactory gseCacheClientFactory) {
        this.meterRegistry = meterRegistry;
        this.gseCacheClientFactory = gseCacheClientFactory;
    }


    @Override
    public GseTaskResponse asyncExecuteScript(ExecuteScriptRequest request) {
        api_script_request requestV1 = toV1ScriptRequest(request);
        return sendScriptTaskRequest(requestV1);
    }

    /**
     * 创建GSE执行脚本请求
     *
     * @param request 执行脚本任务通用请求
     * @return 执行脚本任务请求 - V1
     */
    private api_script_request toV1ScriptRequest(ExecuteScriptRequest request) {
        api_script_request scriptReq = new api_script_request();

        // 脚本文件
        List<api_script_file> scriptFiles = new ArrayList<>();
        request.getScripts().forEach(script -> {
            api_script_file scriptFile = new api_script_file();
            scriptFile.setDownload_path(script.getStoreDir());
            scriptFile.setName(script.getName());
            scriptFile.setContent(script.getContent());
            // 必须设置为空字符，否则GSE报错
            scriptFile.setMd5("");
            scriptFiles.add(scriptFile);
        });
        scriptReq.setScripts(scriptFiles);

        // 脚本任务
        api_task_request taskReq = new api_task_request();
        taskReq.setAtomic_task_num(request.getAtomicTaskNum());
        taskReq.setVersion("1.0");
        // 必须设置为空字符，否则GSE报错
        taskReq.setCrond("");

        // 脚本任务 - 目标 agent
        List<api_agent> agents = buildAgents(request.getAgents());
        taskReq.setAgent_list(agents);

        // 脚本任务 - 原子任务
        List<api_auto_task> taskList = new ArrayList<>();
        request.getAtomicTasks().forEach(atomicScriptTask -> {
            api_auto_task autoTask = new api_auto_task();
            autoTask.setAtomic_task_id(atomicScriptTask.getTaskId().byteValue());
            autoTask.setCmd(atomicScriptTask.getCommand());
            autoTask.setTimeout(atomicScriptTask.getTimeout());
            taskList.add(autoTask);
        });
        taskReq.setAtomic_tasks(taskList);

        // 任务关系
        if (CollectionUtils.isNotEmpty(request.getAtomicScriptTaskRelations())) {
            List<api_task_relation> relations =
                request.getAtomicScriptTaskRelations().stream()
                    .map(relation -> new api_task_relation(relation.getTaskId().byteValue(),
                        relation.getIndex().stream().map(Integer::byteValue).collect(Collectors.toList())))
                    .collect(Collectors.toList());

            taskReq.setRel_list(relations);
        } else {
            // 必须设置为空的集合，否则GSE会报错
            taskReq.setRel_list(Collections.emptyList());
        }

        scriptReq.setTasks(taskReq);

        return scriptReq;
    }

    /**
     * 下发脚本任务
     *
     * @param scriptRequest 请求内容
     * @return GSE Server响应
     */
    private GseTaskResponse sendScriptTaskRequest(api_script_request scriptRequest) {
        return sendCmd(new GseTaskResponseCaller() {
            @Override
            public GseTaskResponse callback(GseTaskClient gseTaskClient) throws TException {
                GseTaskResponse gseResponse = new GseTaskResponse();
                if (log.isDebugEnabled()) {
                    log.debug("RunScriptRequest: {}", scriptRequest);
                } else if (log.isInfoEnabled()) {
                    log.info("RunScriptRequest: {}",
                        GseRequestPrinter.simplifyScriptRequest(scriptRequest));
                }
                api_comm_rsp commRsp = gseTaskClient.getGseAgentClient().run_bash(scriptRequest);
                log.info("RunScriptResponse: {}", commRsp.toString());

                gseResponse.setErrorCode(commRsp.getBk_error_code());
                gseResponse.setErrorMessage(commRsp.getBk_error_msg());
                gseResponse.setGseTaskId(commRsp.getTask_id());
                return gseResponse;
            }

            @Override
            public String getApiName() {
                return "run_bash";
            }
        });
    }

    private List<api_agent> buildAgents(Collection<Agent> agents) {
        return agents.stream().map(this::buildAgent).collect(Collectors.toList());
    }

    private api_agent buildAgent(Agent agent) {
        api_auth auth = new api_auth();
        auth.setUser(agent.getUser());
        if (agent.getPwd() != null) {
            auth.setPassword(agent.getPwd());
        } else {
            auth.setPassword("");
        }

        return buildAgent(agent.getAgentId(), auth);
    }

    private api_agent buildAgent(String cloudIp, api_auth auth) {
        // 在gse api v1, agentId=云区域:IP
        int bkCloudId = GseConstants.DEFAULT_CLOUD_ID;
        String ip;
        String[] ipArray = cloudIp.split(":");
        if (ipArray.length > 1) {
            bkCloudId = Integer.parseInt(ipArray[0]);
            ip = ipArray[1];
        } else {
            ip = ipArray[0];
        }
        api_agent apiAgent = new api_agent();
        apiAgent.setIp(ip);
        if (bkCloudId > 1) {
            apiAgent.setGse_composite_id(bkCloudId);
        }

        apiAgent.setAuth(auth);
        return apiAgent;
    }

    @Override
    public ScriptTaskResult getExecuteScriptResult(GetExecuteScriptResultRequest request) {
        api_query_task_info_v2 requestV1 = toV1QueryScriptTaskResult(request);
        api_task_detail_result resultV1 = getScriptTaskDetailRst(requestV1);
        return toScriptTaskResult(resultV1);
    }

    private api_query_task_info_v2 toV1QueryScriptTaskResult(GetExecuteScriptResultRequest request) {
        List<api_query_agent_info_v2> queryAgentList = new ArrayList<>();
        for (GetExecuteScriptResultRequest.AgentTask agentTask : request.getAgentTasks()) {
            String cloudIp = agentTask.getAgentId();
            api_query_agent_info_v2 queryAgentInfo = new api_query_agent_info_v2();

            api_host apiHost = convertToApiHost(cloudIp);
            queryAgentInfo.setHost(apiHost);
            queryAgentInfo.setAtomic_tasks(
                agentTask.getAtomicTasks().stream()
                    .map(atomicTask -> buildQueryAtomTaskInfo(atomicTask.getOffset(), atomicTask.getAtomicTaskId()))
                    .collect(Collectors.toList()));

            queryAgentList.add(queryAgentInfo);
        }

        api_query_task_info_v2 queryTaskInfo = new api_query_task_info_v2();
        queryTaskInfo.setAgents(queryAgentList);
        queryTaskInfo.setTask_id(request.getTaskId());
        return queryTaskInfo;
    }

    private static api_host convertToApiHost(String cloudIp) {
        String[] ipArray = cloudIp.split(":");

        int cloudAreaId = 0;
        String ip;
        if (ipArray.length > 1) {
            cloudAreaId = Integer.parseInt(ipArray[0]);
            ip = ipArray[1];
        } else {
            ip = ipArray[0];
        }

        api_host apiHost = new api_host();
        apiHost.setIp(ip);
        if (cloudAreaId > 1) {
            apiHost.setGse_composite_id(cloudAreaId);
        } else {
            apiHost.setGse_composite_id(0);
        }
        return apiHost;
    }

    private ScriptTaskResult toScriptTaskResult(api_task_detail_result resultV1) {
        ScriptTaskResult result = new ScriptTaskResult();
        if (CollectionUtils.isNotEmpty(resultV1.getResult())) {
            List<ScriptAgentTaskResult> agentTaskResults =
                resultV1.getResult().stream()
                    .map(agentTaskResultV1 -> {
                        ScriptAgentTaskResult agentTaskResult = new ScriptAgentTaskResult();
                        agentTaskResult.setAgentId(agentTaskResultV1.getGse_composite_id() + ":"
                            + agentTaskResultV1.getIp());
                        agentTaskResult.setAtomicTaskId(agentTaskResultV1.getAtomic_task_id());
                        agentTaskResult.setErrorCode(agentTaskResultV1.getBk_error_code());
                        agentTaskResult.setErrorMsg(agentTaskResultV1.getBk_error_msg());
                        agentTaskResult.setStartTime(agentTaskResultV1.getStart_time());
                        agentTaskResult.setEndTime(agentTaskResultV1.getEnd_time());
                        agentTaskResult.setExitCode(agentTaskResultV1.getExitcode());
                        agentTaskResult.setScreen(agentTaskResultV1.getScreen());
                        agentTaskResult.setStatus(agentTaskResultV1.getStatus());
                        agentTaskResult.setTag(agentTaskResultV1.getTag());
                        return agentTaskResult;
                    })
                    .collect(Collectors.toList());
            result.setResult(agentTaskResults);
        }

        return result;
    }

    @Override
    public List<AgentState> listAgentState(ListAgentStateReq req) {
        List<String> cloudIps = req.getAgentIdList();
        AgentStatusResponse agentStatusResponse = queryAgentStatusFromCacheApi(cloudIps);

        List<AgentState> agentStates = new ArrayList<>();
        if (agentStatusResponse.getResult() != null && !agentStatusResponse.getResult().isEmpty()) {
            agentStatusResponse.getResult().forEach((agentId, stateStr) -> {

                AgentState agentState = new AgentState();
                agentState.setStatusCode(parseCacheAgentStatus(stateStr).getValue());
                agentState.setAgentId(agentId);
                agentStates.add(agentState);
            });

        }
        return agentStates;
    }

    /**
     * 解析agent状态 预期解析的文本: {"businessid":"","exist":1,"ip":"1.1.1.1","region":"1","version":"NULL"}
     */
    private AgentStateStatusEnum parseCacheAgentStatus(String statusStr) {
        AgentStatusDTO agentStatus = JsonUtils.fromJson(statusStr, AgentStatusDTO.class);
        if (agentStatus.getExist() != null && agentStatus.getExist().equals(1)) {
            return AgentStateStatusEnum.RUNNING;
        } else {
            return AgentStateStatusEnum.NOT_FOUND;
        }
    }

    private AgentStatusResponse queryAgentStatusFromCacheApi(Collection<String> agentIds) {
        GseCacheClient gseClient = gseCacheClientFactory.getClient();
        if (null == gseClient) {
            log.error("Get GSE cache client connection failed");
            throw new InternalException(ErrorCode.GSE_API_DATA_ERROR,
                new String[]{"Get GSE cache client connection failed"});
        }

        long start = System.currentTimeMillis();
        String status = "ok";
        try {

            AgentStatusRequestInfo request = buildQueryAgentStatusRequest(agentIds);

            log.debug("QueryAgentStatus request: {}", request);
            AgentStatusResponse response = gseClient.getCacheClient().quireAgentStatus(request);
            log.debug("QueryAgentStatus response: {}", response);
            return response;
        } catch (Throwable e) {
            log.error("QueryAgentStatus error", e);
            status = "error";
            throw new InternalException(e, ErrorCode.GSE_API_DATA_ERROR);
        } finally {
            long end = System.currentTimeMillis();
            log.info("BatchGetAgentStatus {} agentIds, cost: {}ms", agentIds.size(), (end - start));
            if (this.meterRegistry != null) {
                meterRegistry.timer(GseConstants.GSE_API_METRICS_NAME_PREFIX, "api_name", "quireAgentStatus",
                    "status", status).record(end - start, TimeUnit.MICROSECONDS);
            }
            gseClient.tearDown();
        }
    }

    private AgentStatusRequestInfo buildQueryAgentStatusRequest(Collection<String> agentIds) {
        AgentStatusRequestInfo request = new AgentStatusRequestInfo();

        List<CacheIpInfo> ipInfoList = new ArrayList<>();
        for (String agentId : agentIds) {
            String[] ipInfo = agentId.split(":");
            if (ipInfo.length != 2) {
                log.warn("Request agentId format error! IP: {}", agentId);
                continue;
            }

            CacheIpInfo cacheIpInfo = new CacheIpInfo();
            cacheIpInfo.setPlatId(ipInfo[0]);
            cacheIpInfo.setIp(ipInfo[1]);
            ipInfoList.add(cacheIpInfo);
        }

        CacheUser user = new CacheUser();
        user.setUser("bitmap");
        user.setPassword("bitmap");

        request.setUser(user);
        request.setIpinfos(ipInfoList);

        return request;
    }

    @Override
    public GseTaskResponse asyncTransferFile(TransferFileRequest request) {
        List<api_copy_fileinfoV2> copyFileV1Request = toV1CopyFileInfoRequest(request);
        return sendCopyFileTaskRequest(copyFileV1Request);
    }

    private List<api_copy_fileinfoV2> toV1CopyFileInfoRequest(TransferFileRequest request) {
        return request.getTasks().stream()
            .map(task -> buildCopyFileInfo(task, request.getUploadSpeed(),
                request.getDownloadSpeed(), request.getTimeout()))
            .collect(Collectors.toList());
    }

    @Override
    public FileTaskResult getTransferFileResult(GetTransferFileResultRequest request) {
        api_map_rsp rsp;
        if (CollectionUtils.isNotEmpty(request.getAgentIds())) {
            rsp = pullCopyFileResult(request.getTaskId(), request.getAgentIds());
        } else if (StringUtils.isNotBlank(request.getTaskId())) {
            rsp = pullCopyFileResult(request.getTaskId());
        } else {
            log.error("GetTransferFileResult, invalid request!");
            return null;
        }
        if (rsp == null) {
            log.error("GetTransferFileResult, response is null!");
            return null;
        }
        return toFileTaskResult(rsp);
    }

    private FileTaskResult toFileTaskResult(api_map_rsp rsp) {
        FileTaskResult result = new FileTaskResult();
        if (rsp.getResult().isEmpty()) {
            return null;
        }

        List<AtomicFileTaskResult> atomicFileTaskResults = new ArrayList<>();
        Set<Map.Entry<String, String>> ipResults = rsp.getResult().entrySet();
        for (Map.Entry<String, String> ipResult : ipResults) {
            CopyFileRsp copyFileRsp = parseCopyFileRsp(ipResult);
            if (copyFileRsp == null) {
                continue;
            }
            AtomicFileTaskResult atomicFileTaskResult = new AtomicFileTaskResult();
            atomicFileTaskResult.setErrorCode(copyFileRsp.getFinalErrorCode());
            atomicFileTaskResult.setErrorMsg(copyFileRsp.getFinalErrorMsg());

            AtomicFileTaskResultContent content = new AtomicFileTaskResultContent();
            GSEFileTaskResult fileTaskResult = copyFileRsp.getGseFileTaskResult();
            content.setSourceAgentId(fileTaskResult.getSourceAgentId());
            content.setSourceFileDir(fileTaskResult.getSrcDirPath());
            content.setSourceFileName(fileTaskResult.getSrcFileName());
            content.setStandardSourceFilePath(fileTaskResult.getStandardSourceFilePath());
            content.setDestAgentId(fileTaskResult.getDestAgentId());
            content.setDestFileDir(fileTaskResult.getDestDirPath());
            content.setDestFileName(fileTaskResult.getDestFileName());
            content.setStandardDestFilePath(fileTaskResult.getStandardDestFilePath());
            content.setStartTime(fileTaskResult.getStartTime());
            content.setEndTime(fileTaskResult.getEndTime());
            content.setMode(fileTaskResult.getMode());
            content.setProgress(fileTaskResult.getProcess());
            content.setSize(fileTaskResult.getSize());
            content.setSpeed(fileTaskResult.getSpeed());
            content.setStatus(fileTaskResult.getStatus());
            content.setStatusInfo(fileTaskResult.getStatusDesc());
            content.setTaskId(fileTaskResult.getTaskId());
            content.setTaskType(fileTaskResult.getTaskType());
            atomicFileTaskResult.setContent(content);
            atomicFileTaskResults.add(atomicFileTaskResult);
        }
        result.setAtomicFileTaskResults(atomicFileTaskResults);
        return result;
    }

    private CopyFileRsp parseCopyFileRsp(Map.Entry<String, String> ipResult) {
        log.info("ParseCopyFileRsp: {}", ipResult);
        String taskInfo = ipResult.getValue();
        CopyFileRsp copyFileRsp;
        try {
            copyFileRsp = JsonUtils.fromJson(taskInfo, CopyFileRsp.class);
            if (copyFileRsp == null) {
                return null;
            }
        } catch (Throwable e) {
            log.error("Parse CopyFileRsp error", e);
            return null;
        }

        boolean isStandardGSEProtocol = isStandardGSEProtocol(copyFileRsp.getGseFileTaskResult());
        if (!isStandardGSEProtocol) {
            log.warn("Not suggested agent file task result protocol version, please upgrade agent to 1.7.21+)");
            parseCopyFileRspFromResultKey(copyFileRsp, ipResult.getKey());
        }

        return copyFileRsp;
    }

    /**
     * 是否是标准的GSE 文件协议 (协议版本号 > 1)
     *
     * @param fileTaskResult 文件任务返回协议
     */
    private boolean isStandardGSEProtocol(GSEFileTaskResult fileTaskResult) {
        return fileTaskResult != null && fileTaskResult.getProtocolVersion() != null
            && fileTaskResult.getProtocolVersion() > 1;
    }

    /**
     * tmp: 临时兼容，等GSE Agent 全面升级到1.7.6+版本之后，不再支持
     *
     * @param copyFileRsp
     * @param resultKey
     */
    private void parseCopyFileRspFromResultKey(CopyFileRsp copyFileRsp, String resultKey) {
        if (!(resultKey.startsWith(FileDistModeEnum.UPLOAD.getName())
            || resultKey.startsWith(FileDistModeEnum.DOWNLOAD.getName()))) {
            log.warn("Invalid resultKey: {}, ignore", resultKey);
            return;
        }

        // 从key中提取任务信息
        String[] taskProps = resultKey.split(":");
        GSEFileTaskResult fileTaskResult = copyFileRsp.getGseFileTaskResult();
        if (fileTaskResult == null) {
            fileTaskResult = new GSEFileTaskResult();
            copyFileRsp.setGseFileTaskResult(fileTaskResult);
        }
        if (fileTaskResult.getMode() == null) {
            fileTaskResult.setMode(parseFileTaskModeFromKey(taskProps).getValue());
        }
        HostDTO cloudIp = parseCloudIpFromKey(taskProps);
        if (FileDistModeEnum.DOWNLOAD.getValue().equals(fileTaskResult.getMode())) {
            // 格式: "download:srcIpInt:srcIpInt:destFilePath:destCloudId:destIp"
            fileTaskResult.setDestIp(cloudIp.getIp());
            fileTaskResult.setDestCloudId(cloudIp.getBkCloudId());
            // GSE BUG, srcIpInt可能为-1(download:-1:2130706433:/tmp/1.log:0:127.0.0.2)
            String srcIpInt = "-1".equals(taskProps[1]) ? taskProps[2] : taskProps[1];
            fileTaskResult.setSourceIp(IpUtils.revertIpFromNumericalStr(srcIpInt));
            // GSE BUG, 只有源主机IP，没有云区域ID
            fileTaskResult.setSourceCloudId(null);
            // GSE BUG, 只有目标文件信息，没有源文件信息
            String destFilePath = parseFilePathFromKey(taskProps);
            Pair<String, String> dirAndFileName = FilePathUtils.parseDirAndFileName(destFilePath);
            fileTaskResult.setDestDirPath(dirAndFileName.getLeft());
            fileTaskResult.setDestFileName(dirAndFileName.getRight());
        } else {
            // 格式: "upload:srcIpInt:srcFilePath:srcCloudId:srcIp"
            fileTaskResult.setSourceIp(cloudIp.getIp());
            fileTaskResult.setSourceCloudId(cloudIp.getBkCloudId());
            String sourceFilePath = parseFilePathFromKey(taskProps);
            Pair<String, String> dirAndFileName = FilePathUtils.parseDirAndFileName(sourceFilePath);
            fileTaskResult.setSrcDirPath(dirAndFileName.getLeft());
            fileTaskResult.setSrcFileName(dirAndFileName.getRight());
        }
    }

    private String parseFilePathFromKey(String[] taskProps) {
        String filePath = taskProps[taskProps.length - 3];
        if (taskProps.length > 4 && taskProps[taskProps.length - 4] != null) {
            // 如果是正则的文件， /tmp/REGEX:abc.*.txt 这种有:，在key中会被分开，要拼回去
            // GSE 的Redis Key问题 可能引入空格变=号，导致key被当成key=value, 所以要判断 taskProps.length > 4
            // Windows路径包含:
            if (taskProps[taskProps.length - 4].endsWith("REGEX")
                || WindowsHelper.isWindowsDiskPartition(taskProps[taskProps.length - 4])) {
                filePath = taskProps[taskProps.length - 4] + ":" + filePath;
            }
        }
        return filePath;
    }


    private HostDTO parseCloudIpFromKey(String[] taskProps) {
        String ip = taskProps[taskProps.length - 1];
        long cloudAreaId = Long.parseLong(taskProps[taskProps.length - 2].trim());
        return new HostDTO(cloudAreaId, ip);
    }

    private FileDistModeEnum parseFileTaskModeFromKey(String[] taskProps) {
        String mode = taskProps[0];
        if (FileDistModeEnum.UPLOAD.getName().equals(mode)) {
            return FileDistModeEnum.UPLOAD;
        } else if (FileDistModeEnum.DOWNLOAD.getName().equals(mode)) {
            return FileDistModeEnum.DOWNLOAD;
        } else {
            throw new IllegalArgumentException("Invalid file dist mode: " + mode);
        }
    }

    @Override
    public GseTaskResponse terminateGseFileTask(TerminateGseTaskRequest request) {
        return sendForceStopTaskRequest(toV1StopTaskRequest(request, GseTaskTypeEnum.FILE));
    }

    private api_stop_task_request toV1StopTaskRequest(TerminateGseTaskRequest request,
                                                      GseTaskTypeEnum taskType) {
        api_stop_task_request stopRequest = new api_stop_task_request();
        stopRequest.setStop_task_id(request.getTaskId());
        if (CollectionUtils.isNotEmpty(request.getAgentIds())) {
            stopRequest.setAgents(request.getAgentIds().stream()
                .map(agentId ->
                    // 终止任务并不不需要账号密码,此处传入为了绕过thrift协议的校验
                    buildAgent(agentId, buildEmptyApiAuth()))
                .collect(Collectors.toList()));
        }
        stopRequest.setType(taskType.getValue());
        return stopRequest;
    }

    private api_auth buildEmptyApiAuth() {
        api_auth auth = new api_auth();
        auth.setUser("job");
        auth.setPassword("");
        return auth;
    }

    @Override
    public GseTaskResponse terminateGseScriptTask(TerminateGseTaskRequest request) {
        return sendForceStopTaskRequest(toV1StopTaskRequest(request, GseTaskTypeEnum.SCRIPT));
    }


    /**
     * 构建 GSE 拷贝文件请求
     *
     * @param task 文件拷贝任务
     * @return 拷贝文件请求
     */
    public api_copy_fileinfoV2 buildCopyFileInfo(FileTransferTask task,
                                                 Integer uploadSpeedLimit,
                                                 Integer downloadSpeedLimit,
                                                 Integer timeout) {
        api_copy_fileinfoV2 copyFileInfo = new api_copy_fileinfoV2();

        api_base_file_info baseFileInfo = new api_base_file_info();
        SourceFile sourceFile = task.getSource();
        TargetFile targetFile = task.getTarget();
        baseFileInfo.setName(sourceFile.getFileName());
        baseFileInfo.setPath(sourceFile.getStoreDir());
        baseFileInfo.setDest_path(targetFile.getStoreDir());
        baseFileInfo.setDest_name(targetFile.getFileName());
        baseFileInfo.setBackup_name(StringUtils.EMPTY);
        baseFileInfo.setBackup_path(StringUtils.EMPTY);
        baseFileInfo.setOwner(StringUtils.EMPTY);
        baseFileInfo.setMd5(StringUtils.EMPTY);

        copyFileInfo.setFile(baseFileInfo);
        copyFileInfo.setSrc_agent(buildAgent(sourceFile.getAgent()));
        copyFileInfo.setDest_agents(buildAgents(targetFile.getAgents()));
        if (uploadSpeedLimit != null) {
            copyFileInfo.setUpload_speed(uploadSpeedLimit);
        }
        if (downloadSpeedLimit != null) {
            copyFileInfo.setDownload_speed(downloadSpeedLimit);
        }
        if (timeout != null) {
            copyFileInfo.setTimeout(timeout);
        }
        return copyFileInfo;
    }

    /**
     * 下发文件分发任务
     *
     * @param copyFileInfoList 请求内容
     * @return GSE Server响应
     */
    public GseTaskResponse sendCopyFileTaskRequest(List<api_copy_fileinfoV2> copyFileInfoList) {
        return sendCmd(new GseTaskResponseCaller() {
            @Override
            public GseTaskResponse callback(GseTaskClient gseTaskClient) throws TException {
                GseTaskResponse gseResponse = new GseTaskResponse();
                log.info("FileCopyTaskRequest: {}", copyFileInfoList);
                api_comm_rsp commRsp = gseTaskClient.getGseAgentClient().copy_file_v2(copyFileInfoList);
                log.info("FileCopyTaskResponse: {}", commRsp);
                gseResponse.setErrorCode(commRsp.getBk_error_code());
                gseResponse.setErrorMessage(commRsp.getBk_error_msg());
                gseResponse.setGseTaskId(commRsp.getTask_id());
                return gseResponse;
            }

            @Override
            public String getApiName() {
                return "copy_file_v2";
            }
        });
    }

    /**
     * 强制终止任务
     *
     * @param stopTaskRequest 终止任务请求
     * @return GSE SERVER 响应
     */
    public GseTaskResponse sendForceStopTaskRequest(api_stop_task_request stopTaskRequest) {
        return sendCmd(new GseTaskResponseCaller() {
            @Override
            public GseTaskResponse callback(GseTaskClient gseTaskClient) throws TException {
                GseTaskResponse gseResponse = new GseTaskResponse();
                log.info("ForceStopTaskRequest: {}", stopTaskRequest);
                api_comm_rsp commRsp = gseTaskClient.getGseAgentClient().stop_task(stopTaskRequest);
                log.info("ForceStopTaskResponse: {}", commRsp);
                gseResponse.setErrorCode(commRsp.getBk_error_code());
                gseResponse.setErrorMessage(commRsp.getBk_error_msg());
                gseResponse.setGseTaskId(commRsp.getTask_id());
                return gseResponse;
            }

            @Override
            public String getApiName() {
                return "stop_task";
            }
        });
    }

    private api_query_atom_task_info buildQueryAtomTaskInfo(int offset, int mid) {
        api_query_atom_task_info queryAtomTaskInfo = new api_query_atom_task_info();
        queryAtomTaskInfo.setId((byte) mid);
        queryAtomTaskInfo.setOffset(offset);
        return queryAtomTaskInfo;
    }


    /**
     * 获取文件任务执行结果
     *
     * @param gseTaskId gse任务ID
     * @return 结果
     */
    private api_map_rsp pullCopyFileResult(String gseTaskId) {
        return sendCmd(new GseApiCallback<api_map_rsp>() {
            @Override
            public api_map_rsp callback(GseTaskClient gseTaskClient) throws TException {
                log.info("GetCopyFileResult|request|gseTaskId: {}", gseTaskId);
                api_map_rsp copyFileTaskLog = gseTaskClient.getGseAgentClient().get_copy_file_result(gseTaskId);
                log.info("GetCopyFileResult|response: {}", copyFileTaskLog);
                return copyFileTaskLog;
            }

            @Override
            public String getApiName() {
                return "get_copy_file_result";
            }
        });
    }

    /**
     * 拉取文件任务执行结果-根据IP
     */
    private api_map_rsp pullCopyFileResult(String gseTaskId, Collection<String> cloudIps) {
        return sendCmd(new GseApiCallback<api_map_rsp>() {
            @Override
            public api_map_rsp callback(GseTaskClient gseTaskClient) throws TException {
                List<api_host> hosts = cloudIps.stream()
                    .map(GseV1ApiClient::convertToApiHost).collect(Collectors.toList());
                log.info("GetCopyFileResultByAgent|gseTaskId:{}|hosts:{}", gseTaskId, hosts);
                api_map_rsp copyFileTaskLog = gseTaskClient.getGseAgentClient()
                    .get_copy_file_result_by_ip_v2(gseTaskId, hosts);
                log.info("GetCopyFileResultByAgent|response:{}", copyFileTaskLog);
                return copyFileTaskLog;
            }

            @Override
            public String getApiName() {
                return "get_copy_file_result_by_ip_v2";
            }
        });
    }

    private api_task_detail_result getScriptTaskDetailRst(api_query_task_info_v2 taskQuery) {
        return sendCmd(new GseApiCallback<api_task_detail_result>() {
            @Override
            public api_task_detail_result callback(GseTaskClient gseTaskClient) throws TException {
                log.info("GetScriptTaskDetailRequest: {}", taskQuery);
                api_task_detail_result taskDetailRst =
                    gseTaskClient.getGseAgentClient().get_task_detail_result(taskQuery);
                log.info("GetScriptTaskDetailResponse: {}", GseRequestPrinter.printScriptTaskResult(taskDetailRst));
                return taskDetailRst;
            }

            @Override
            public String getApiName() {
                return "get_task_detail_result";
            }
        });
    }

    private <T> T sendCmd(GseApiCallback<T> caller) {
        int retry = 1;
        boolean connect = true;
        do {
            String status = "ok";
            long start = System.nanoTime();
            try (GseTaskClient gseTaskClient = GseTaskClient.getClient()) {
                connect = true;
                if (gseTaskClient == null) {
                    connect = false;
                    // 如果拿不到连接 ，则等待3s重试
                    log.info("Get GseTaskClient, retry after 3000ms");
                    ThreadUtils.sleep(3000L);
                    continue;
                }
                return caller.callback(gseTaskClient);
            } catch (TTransportException e) {
                // 由于无法捕获到底层的java.net.SocketTimeoutException，所以只能对errorMessage进行判断。读超时无需重试
                status = "error";
                if ("java.net.SocketTimeoutException: Read timed out".equalsIgnoreCase(e.getMessage())) {
                    log.error("Invoke gse api fail", e);
                    throw new GseReadTimeoutException(e.getMessage());
                }
            } catch (Throwable e) {
                log.error("Invoke gse api fail", e);
                status = "error";
            } finally {
                long end = System.nanoTime();
                meterRegistry.timer(GseConstants.GSE_API_METRICS_NAME_PREFIX, "api_name", caller.getApiName(),
                    "status", status).record(end - start, TimeUnit.NANOSECONDS);
            }
        } while (retry-- > 0); //重试1次
        return caller.fail(connect);
    }

    public interface GseApiCallback<T> {

        T callback(GseTaskClient gseTaskClient) throws TException;

        String getApiName();

        default T fail(boolean connect) {
            log.warn("Call gse api fail, connect: {}", connect);
            return null;
        }
    }

    public interface GseTaskResponseCaller extends GseApiCallback<GseTaskResponse> {

        @Override
        default GseTaskResponse fail(boolean connect) {
            GseTaskResponse gseResponse = new GseTaskResponse();
            gseResponse.setErrorCode(-1);
            if (connect) {
                gseResponse.setErrorMessage("Send GSE Job failed!");
            } else {
                gseResponse.setErrorMessage("Connect GSE Server failed!");
            }
            return gseResponse;
        }
    }
}

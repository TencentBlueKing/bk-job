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

package com.tencent.bk.job.execute.engine.gse;

import com.tencent.bk.gse.taskapi.api_agent;
import com.tencent.bk.gse.taskapi.api_auth;
import com.tencent.bk.gse.taskapi.api_auto_task;
import com.tencent.bk.gse.taskapi.api_base_file_info;
import com.tencent.bk.gse.taskapi.api_comm_rsp;
import com.tencent.bk.gse.taskapi.api_copy_fileinfoV2;
import com.tencent.bk.gse.taskapi.api_host;
import com.tencent.bk.gse.taskapi.api_map_rsp;
import com.tencent.bk.gse.taskapi.api_process_base_info;
import com.tencent.bk.gse.taskapi.api_process_extra_info;
import com.tencent.bk.gse.taskapi.api_process_info;
import com.tencent.bk.gse.taskapi.api_process_req;
import com.tencent.bk.gse.taskapi.api_query_agent_info_v2;
import com.tencent.bk.gse.taskapi.api_query_atom_task_info;
import com.tencent.bk.gse.taskapi.api_query_task_info_v2;
import com.tencent.bk.gse.taskapi.api_script_file;
import com.tencent.bk.gse.taskapi.api_script_request;
import com.tencent.bk.gse.taskapi.api_stop_task_request;
import com.tencent.bk.gse.taskapi.api_task_detail_result;
import com.tencent.bk.gse.taskapi.api_task_request;
import com.tencent.bk.job.common.gse.constants.GseConstants;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.execute.common.exception.ReadTimeoutException;
import com.tencent.bk.job.execute.engine.model.GseTaskResponse;
import com.tencent.bk.job.execute.engine.model.LogPullProgress;
import com.tencent.bk.job.execute.engine.model.RunSQLScriptFile;
import com.tencent.bk.job.execute.gse.model.ProcessOperateTypeEnum;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * GSE请求构建工具类
 */
@Slf4j
public class GseRequestUtils {
    private static final MeterRegistry meterRegistry;

    static {
        meterRegistry = ApplicationContextRegister.getBean(MeterRegistry.class);
    }

    /**
     * 构建 GSE 拷贝文件信息
     *
     * @param src                源agent
     * @param srcPath            源文件路径
     * @param srcName            源文件名称
     * @param dests              目标agent
     * @param destPath           目标路径
     * @param destName           目标目录
     * @param downloadSpeedLimit 下载限速，KB
     * @param uploadSpeedLimit   上传限速，KB
     * @param timeout            超时时间，单位s
     * @return 拷贝文件信息
     */
    public static api_copy_fileinfoV2 buildCopyFileInfo(api_agent src, String srcPath, String srcName,
                                                        List<api_agent> dests, String destPath,
                                                        String destName, Integer downloadSpeedLimit,
                                                        Integer uploadSpeedLimit, Integer timeout) {
        api_copy_fileinfoV2 copyFileInfo = new api_copy_fileinfoV2();

        api_base_file_info baseFileInfo = new api_base_file_info();
        baseFileInfo.setName(srcName);
        baseFileInfo.setPath(srcPath);
        baseFileInfo.setDest_path(destPath);
        baseFileInfo.setDest_name(destName);
        baseFileInfo.setBackup_name(StringUtils.EMPTY);
        baseFileInfo.setBackup_path(StringUtils.EMPTY);
        baseFileInfo.setOwner(StringUtils.EMPTY);
        baseFileInfo.setMd5(StringUtils.EMPTY);

        copyFileInfo.setFile(baseFileInfo);
        copyFileInfo.setSrc_agent(src);
        copyFileInfo.setDest_agents(dests);
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
     * 创建 Agent 列表
     *
     * @param userName 用户名
     * @param passwd   密码
     * @return Agent列表
     */
    public static List<api_agent> buildAgentList(Set<String> jobIpSet, String userName, String passwd) {
        List<api_agent> agentList = new ArrayList<>();
        for (String ip : jobIpSet) {
            api_agent agent = buildAgent(ip, userName, passwd);
            agentList.add(agent);
        }
        return agentList;
    }

    public static List<api_agent> buildAgentList(List<IpDTO> ipList, String userName, String passwd) {
        List<api_agent> agentList = new ArrayList<>();
        for (IpDTO ipDTO : ipList) {
            String fullIp = ipDTO.getCloudAreaId() + ":" + ipDTO.getIp();
            api_agent agent = buildAgent(fullIp, userName, passwd);
            agentList.add(agent);
        }
        return agentList;
    }

    /**
     * @param userName 用户名
     * @param cloudIp  云区域:IP
     * @return Agent
     */
    public static api_agent buildAgent(String cloudIp, String userName, String passwd) {
        int source = 0;
        String ip;
        String[] ipArray = cloudIp.split(":");
        if (ipArray.length > 1) {
            source = Integer.parseInt(ipArray[0]);
            ip = ipArray[1];
        } else {
            ip = ipArray[0];
        }

        api_auth auth = new api_auth();
        auth.setUser(userName);
        if (passwd != null) {
            auth.setPassword(passwd);
        } else {
            auth.setPassword("");
        }

        api_agent agent = new api_agent();
        agent.setIp(ip);
        agent.setAuth(auth);
        if (source > 1) {
            agent.setGse_composite_id(source);
        }

        return agent;
    }

    /**
     * 创建GSE执行脚本请求(以文件做参数）
     */
    public static api_script_request buildScriptRequestWithSQL(List<api_agent> agentList,
                                                               RunSQLScriptFile runSQLScriptFile) {
        api_script_request scriptReq = new api_script_request();

        api_script_file scriptFile = new api_script_file();
        scriptFile.setDownload_path(runSQLScriptFile.getDownloadPath());
        scriptFile.setMd5("");
        scriptFile.setName(runSQLScriptFile.getPublicScriptName());
        scriptFile.setContent(runSQLScriptFile.getPublicScriptContent());
        api_script_file sqlScriptFile = new api_script_file();
        sqlScriptFile.setDownload_path(runSQLScriptFile.getDownloadPath());
        sqlScriptFile.setMd5("");
        sqlScriptFile.setName(runSQLScriptFile.getSqlScriptFileName());
        sqlScriptFile.setContent(runSQLScriptFile.getSqlScriptContent());
        List<api_script_file> scriptFileList = new ArrayList<>();
        scriptFileList.add(scriptFile);
        scriptFileList.add(sqlScriptFile);
        scriptReq.setScripts(scriptFileList);

        api_task_request req = new api_task_request();
        req.setAtomic_task_num(1);
        req.setCrond("");

        List<api_auto_task> taskList = new ArrayList<>();
        api_auto_task autoTask = new api_auto_task();
        autoTask.setAtomic_task_id((byte) 0);

        String exeCmd = runSQLScriptFile.getDownloadPath() + "/" + runSQLScriptFile.getPublicScriptName()
            + " " + runSQLScriptFile.getDownloadPath() + "/" + runSQLScriptFile.getSqlScriptFileName()
            + " " + runSQLScriptFile.getParamForDBInfo();
        autoTask.setCmd(exeCmd);
        autoTask.setTimeout(runSQLScriptFile.getTimeout());
        taskList.add(autoTask);

        req.setAtomic_tasks(taskList);
        req.setAgent_list(agentList);
        req.setRel_list(new ArrayList<>());
        req.setVersion("1.0");
        scriptReq.setTasks(req);

        return scriptReq;
    }

    /**
     * 创建GSE执行脚本请求
     *
     * @param agentList      Agent列表
     * @param scriptContent  脚本内容
     * @param scriptFileName 脚本名称
     * @param downloadPath   脚本存放路径
     * @param scriptParam    脚本参数
     * @param timeout        脚本任务超时时间，单位秒
     * @return 执行脚本任务请求
     */
    public static api_script_request buildScriptRequest(List<api_agent> agentList, String scriptContent,
                                                        String scriptFileName, String downloadPath, String scriptParam,
                                                        int timeout) {
        api_script_request scriptReq = new api_script_request();

        // 脚本文件
        api_script_file scriptFile = new api_script_file();
        scriptFile.setDownload_path(downloadPath);
        scriptFile.setMd5("");
        scriptFile.setName(scriptFileName);
        scriptFile.setContent(scriptContent);
        List<api_script_file> scriptFileList = new ArrayList<>();
        scriptFileList.add(scriptFile);
        scriptReq.setScripts(scriptFileList);

        api_task_request req = new api_task_request();
        req.setAtomic_task_num(1);
        req.setCrond("");

        List<api_auto_task> taskList = new ArrayList<>();
        api_auto_task autoTask = new api_auto_task();
        autoTask.setAtomic_task_id((byte) 0);

        String exeCmd = downloadPath + "/" + scriptFileName;
        if (StringUtils.isNotBlank(scriptParam)) {
            exeCmd += " " + scriptParam;
        }

        autoTask.setCmd(exeCmd);
        autoTask.setTimeout(timeout);
        taskList.add(autoTask);

        req.setAtomic_tasks(taskList);
        req.setAgent_list(agentList);
        req.setRel_list(new ArrayList<>());
        req.setVersion("1.0");
        scriptReq.setTasks(req);

        return scriptReq;
    }

    /**
     * 下发脚本任务
     *
     * @param id            任务ID
     * @param scriptRequest 请求内容
     * @return GSE Server响应
     */
    public static GseTaskResponse sendScriptTaskRequest(long id, api_script_request scriptRequest) {
        return sendCmd("" + id, new GseTaskResponseCaller() {
            @Override
            public GseTaskResponse callback(GseClient gseClient) throws TException {
                GseTaskResponse gseResponse = new GseTaskResponse();
                if (log.isDebugEnabled()) {
                    log.debug("[{}]: runScriptTaskRequest={}", id, scriptRequest);
                } else if (log.isInfoEnabled()) {
                    log.info("[{}]: runScriptTaskRequest={}", id,
                        GseRequestPrinter.simplifyScriptRequest(scriptRequest));
                }
                api_comm_rsp commRsp = gseClient.getGseAgentClient().run_bash(scriptRequest);
                log.info("[{}]: runScriptTaskResponse={}", id, commRsp.toString());

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


    public static api_process_req buildProcessRequest(List<api_agent> agentList, api_process_base_info processBaseInfo,
                                                      api_process_extra_info processExtraInfo, int reqType) {
        api_process_req processReq = new api_process_req();

        api_process_info processInfo = new api_process_info();
        processInfo.setBase_info(processBaseInfo);
        processInfo.setExtra_info(processExtraInfo);
        processReq.setAgents(agentList);
        processReq.setProc_info(processInfo);
        processReq.setOperate_type(reqType);

        return processReq;
    }

    /**
     * 下发文件分发任务
     *
     * @param id               任务ID
     * @param copyFileInfoList 请求内容
     * @return GSE Server响应
     */
    public static GseTaskResponse sendCopyFileTaskRequest(long id, List<api_copy_fileinfoV2> copyFileInfoList) {
        return sendCmd("" + id, new GseTaskResponseCaller() {
            @Override
            public GseTaskResponse callback(GseClient gseClient) throws TException {
                GseTaskResponse gseResponse = new GseTaskResponse();
                log.info("[{}]: fileCopyTaskRequest={}", id, copyFileInfoList);
                api_comm_rsp commRsp = gseClient.getGseAgentClient().copy_file_v2(copyFileInfoList);
                log.info("[{}]: fileCopyTaskResponse={}", id, commRsp);
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
     * @param id              任务ID
     * @param stopTaskRequest 终止任务请求
     * @return GSE SERVER 响应
     */
    public static GseTaskResponse sendForceStopTaskRequest(long id, api_stop_task_request stopTaskRequest) {
        return sendCmd("" + id, new GseTaskResponseCaller() {
            @Override
            public GseTaskResponse callback(GseClient gseClient) throws TException {
                GseTaskResponse gseResponse = new GseTaskResponse();
                log.info("[{}]: sendForceStopTaskRequest={}", id, stopTaskRequest);
                api_comm_rsp commRsp = gseClient.getGseAgentClient().stop_task(stopTaskRequest);
                log.info("[{}]: sendForceStopTaskResponse={}", id, commRsp);
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

    private static List<api_query_atom_task_info> buildQueryAtomTaskInfoList(int offset, int mid) {
        api_query_atom_task_info queryAtomTaskInfo = new api_query_atom_task_info();
        queryAtomTaskInfo.setId((byte) mid);
        queryAtomTaskInfo.setOffset(offset);

        List<api_query_atom_task_info> queryAtomTaskList = new ArrayList<>();
        queryAtomTaskList.add(queryAtomTaskInfo);
        return queryAtomTaskList;
    }

    /**
     * 拉取脚本执行任务日志 V2
     *
     * @param gseTaskId        gse任务ID
     * @param cloudIps         目标服务器列表
     * @param runBushOffsetMap 日志偏移量
     * @return 响应
     */
    public static api_query_task_info_v2 buildScriptLogRequestV2(String gseTaskId, Collection<String> cloudIps,
                                                                 Map<String, LogPullProgress> runBushOffsetMap) {
        List<api_query_agent_info_v2> queryAgentList = new ArrayList<>();
        for (String cloudIp : cloudIps) {
            api_query_agent_info_v2 queryAgentInfo = new api_query_agent_info_v2();

            api_host apiHost = convertToApiHost(cloudIp);
            queryAgentInfo.setHost(apiHost);

            LogPullProgress process = runBushOffsetMap.get(cloudIp);
            if (null == process) {
                queryAgentInfo.setAtomic_tasks(buildQueryAtomTaskInfoList(0, 0));
            } else {
                queryAgentInfo.setAtomic_tasks(buildQueryAtomTaskInfoList(process.getByteOffset(), process.getMid()));
            }

            queryAgentList.add(queryAgentInfo);
        }

        // 请求消息
        api_query_task_info_v2 queryTaskInfo = new api_query_task_info_v2();
        queryTaskInfo.setAgents(queryAgentList);
        queryTaskInfo.setTask_id(gseTaskId);
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

    /**
     * 获取文件任务执行结果
     *
     * @param id        job任务ID
     * @param gseTaskId gse任务ID
     * @return 结果
     */
    public static api_map_rsp pullCopyFileTaskLog(long id, String gseTaskId) {
        return sendCmd("" + id, new GseApiCallback<api_map_rsp>() {
            @Override
            public api_map_rsp callback(GseClient gseClient) throws TException {
                log.info("[{}]: pullCopyFileTaskLog|gseTaskId:{}", id, gseTaskId);
                api_map_rsp copyFileTaskLog = gseClient.getGseAgentClient().get_copy_file_result(gseTaskId);
                log.info("[{}]: pullCopyFileTaskLog|response:{}", id, copyFileTaskLog);
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
    public static api_map_rsp pullCopyFileTaskLog(long id, String gseTaskId, Collection<String> cloudIps) {
        return sendCmd("" + id, new GseApiCallback<api_map_rsp>() {
            @Override
            public api_map_rsp callback(GseClient gseClient) throws TException {
                List<api_host> hosts = cloudIps.stream()
                    .map(GseRequestUtils::convertToApiHost).collect(Collectors.toList());
                log.info("[{}]: pullCopyFileTaskLogByIp|gseTaskId:{}|hosts:{}", id, gseTaskId, hosts);
                api_map_rsp copyFileTaskLog = gseClient.getGseAgentClient()
                    .get_copy_file_result_by_ip_v2(gseTaskId, hosts);
                log.info("[{}]: pullCopyFileTaskLogByIp|response:{}", id, copyFileTaskLog);
                return copyFileTaskLog;
            }

            @Override
            public String getApiName() {
                return "get_copy_file_result_by_ip_v2";
            }
        });
    }

    /**
     * 脚本任务概览信息
     */
    public static api_task_detail_result getScriptTaskDetailRst(long id, api_query_task_info_v2 taskQuery) {
        return sendCmd("" + id, new GseApiCallback<api_task_detail_result>() {
            @Override
            public api_task_detail_result callback(GseClient gseClient) throws TException {
                log.info("[{}]: getScriptTaskDetailRequest={}", id, taskQuery);
                api_task_detail_result taskDetailRst = gseClient.getGseAgentClient().get_task_detail_result(taskQuery);
                log.info("[{}]: getScriptTaskDetailResponse={}", id,
                    GseRequestPrinter.printScriptTaskResult(taskDetailRst));
                return taskDetailRst;
            }

            @Override
            public String getApiName() {
                return "get_task_detail_result";
            }
        });
    }

    public static GseTaskResponse sendProcessRequest(String id, List<api_process_req> processRequestList, int reqType) {
        return sendCmd(id, new GseTaskResponseCaller() {
            @Override
            public GseTaskResponse callback(GseClient gseClient) throws TException {
                GseTaskResponse gseResponse = new GseTaskResponse();
                log.info("[{}]: sendProcessRequest: {}", id, processRequestList.toString());
                api_comm_rsp commRsp;
                if (reqType == ProcessOperateTypeEnum.REGISTER_PROC.getValue()) {
                    commRsp = gseClient.getGseAgentClient().register_hosting_process(processRequestList);
                } else if (reqType == ProcessOperateTypeEnum.UNREGISTER_PROC.getValue()) {
                    commRsp = gseClient.getGseAgentClient().unregister_hosting_process(processRequestList);
                } else {
                    commRsp = gseClient.getGseAgentClient().process_operate(processRequestList);
                    log.info("[{}]: sendProcessOperResponse: {}", id, commRsp.toString());
                }
                gseResponse.setErrorCode(commRsp.getBk_error_code());
                gseResponse.setErrorMessage(commRsp.getBk_error_msg());
                gseResponse.setGseTaskId(commRsp.getTask_id());
                return gseResponse;
            }

            @Override
            public String getApiName() {
                return "process_operate";
            }
        });
    }

    public static api_map_rsp getGetProcRst(String id, String gseTaskId) {
        return sendCmd(id, new GseApiCallback<api_map_rsp>() {
            @Override
            public api_map_rsp callback(GseClient gseClient) throws TException {
                log.info("[{}]: getGetProcRst: gseTaskId={}", id, gseTaskId);
                api_map_rsp getGetProcRst = gseClient.getGseAgentClient().get_proc_result(gseTaskId);
                log.info("[{}]: getGetProcRst: {}", id, getGetProcRst);
                return getGetProcRst;
            }

            @Override
            public String getApiName() {
                return "get_proc_result";
            }
        });
    }

    private static <T> T sendCmd(String id, GseApiCallback<T> caller) {
        int retry = 1;
        boolean connect = true;
        do {
            String status = "ok";
            long start = System.nanoTime();
            try (GseClient gseClient = GseClient.getClient()) {
                connect = true;
                if (gseClient == null) {
                    connect = false;
                    continue; //如果拿不到连接 ，则重试
                }
                return caller.callback(gseClient);
            } catch (TTransportException e) {
                // 由于无法捕获到底层的java.net.SocketTimeoutException，所以只能对errorMessage进行判断。读超时无需重试
                status = "error";
                if ("java.net.SocketTimeoutException: Read timed out".equalsIgnoreCase(e.getMessage())) {
                    log.error("[" + id + "]: Invoke gse api fail", e);
                    throw new ReadTimeoutException(e.getMessage());
                }
            } catch (Exception e) {
                log.error("[" + id + "]: Invoke gse api fail", e);
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

        T callback(GseClient gseClient) throws TException;

        String getApiName();

        default T fail(boolean connect) {
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

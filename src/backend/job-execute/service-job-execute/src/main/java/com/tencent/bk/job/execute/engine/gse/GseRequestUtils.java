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
import com.tencent.bk.gse.taskapi.api_comm_rsp;
import com.tencent.bk.gse.taskapi.api_map_rsp;
import com.tencent.bk.gse.taskapi.api_process_base_info;
import com.tencent.bk.gse.taskapi.api_process_extra_info;
import com.tencent.bk.gse.taskapi.api_process_info;
import com.tencent.bk.gse.taskapi.api_process_req;
import com.tencent.bk.gse.taskapi.api_stop_task_request;
import com.tencent.bk.job.common.gse.constants.GseConstants;
import com.tencent.bk.job.common.gse.model.GseTaskResponse;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.execute.common.exception.ReadTimeoutException;
import com.tencent.bk.job.execute.gse.model.ProcessOperateTypeEnum;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * GSE请求构建工具类
 */
@Slf4j
public class GseRequestUtils {
    private static final MeterRegistry meterRegistry;

    static {
        meterRegistry = ApplicationContextRegister.getBean(MeterRegistry.class);
    }


    public static List<api_agent> buildAgentList(Collection<String> agentIds, String userName, String password) {
        List<api_agent> agentList = new ArrayList<>();
        for (String agentId : agentIds) {
            api_agent agent = buildAgent(agentId, userName, password);
            agentList.add(agent);
        }
        return agentList;
    }

    public static api_agent buildAgent(String agentId, String userName, String password) {
        // 在gse api v1, agentId=云区域:IP
        int source = 0;
        String ip;
        String[] ipArray = agentId.split(":");
        if (ipArray.length > 1) {
            source = Integer.parseInt(ipArray[0]);
            ip = ipArray[1];
        } else {
            ip = ipArray[0];
        }

        api_auth auth = new api_auth();
        auth.setUser(userName);
        if (password != null) {
            auth.setPassword(password);
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
                    // 如果拿不到连接 ，则等待3s重试
                    ThreadUtils.sleep(3000L);
                    continue;
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

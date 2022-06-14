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

package com.tencent.bk.job.execute.gse;

import com.tencent.bk.gse.taskapi.api_agent;
import com.tencent.bk.gse.taskapi.api_map_rsp;
import com.tencent.bk.gse.taskapi.api_process_base_info;
import com.tencent.bk.gse.taskapi.api_process_extra_info;
import com.tencent.bk.gse.taskapi.api_process_req;
import com.tencent.bk.job.common.gse.model.GseTaskResponse;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.engine.gse.GseRequestUtils;
import com.tencent.bk.job.execute.gse.model.GseProcessInfoDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class GseApiExecutor {

    private String uid;

    public GseApiExecutor(String uid) {
        this.uid = uid;
    }

    public GseTaskResponse processOperateProcess(List<GseProcessInfoDTO> processInfoList, int reqType) {
        List<api_process_req> procReqList = new ArrayList<>();
        for (GseProcessInfoDTO processInfo : processInfoList) {
            List<api_agent> agentList = GseRequestUtils.buildAgentList(
                processInfo.getIpList().stream().map(HostDTO::toCloudIp).collect(Collectors.toList()),
                processInfo.getUserName(), null);

            api_process_base_info processBaseInfo = createProcessBaseInfo(processInfo);
            api_process_extra_info processExtraInfo = createProcessExtraInfo(processInfo);

            api_process_req procRequest = GseRequestUtils.buildProcessRequest(agentList, processBaseInfo,
                processExtraInfo, reqType);

            procReqList.add(procRequest);
        }

        return GseRequestUtils.sendProcessRequest(uid, procReqList, reqType);
    }

    private api_process_extra_info createProcessExtraInfo(GseProcessInfoDTO processInfo) {
        api_process_extra_info processExtraInfo = new api_process_extra_info();
        processExtraInfo.setType(processInfo.getType());
        processExtraInfo.setCpu_limit(processInfo.getCpuLmt());
        processExtraInfo.setMem_limit(processInfo.getMemLmt());
        processExtraInfo.setCycle_time(processInfo.getCycleTime());
        processExtraInfo.setInstance_num(processInfo.getInstanceNum());
        processExtraInfo.setStart_check_begin_time(processInfo.getStartCheckBeginTime());
        processExtraInfo.setStart_check_end_time(processInfo.getStartCheckEndTime());
        processExtraInfo.setOp_timeout(processInfo.getOpTimeOut());
        return processExtraInfo;
    }

    private api_process_base_info createProcessBaseInfo(GseProcessInfoDTO processInfo) {
        api_process_base_info processBaseInfo = new api_process_base_info();
        processBaseInfo.setProc_name(ifNull(processInfo.getProcName()));
        processBaseInfo.setSetup_path(ifNull(processInfo.getSetupPath()));
        processBaseInfo.setCfg_path(ifNull(processInfo.getCfgPath()));
        processBaseInfo.setLog_path(ifNull(processInfo.getLogPath()));
        processBaseInfo.setPid_path(ifNull(processInfo.getPidPath()));
        processBaseInfo.setContact(ifNull(processInfo.getContact()));
        processBaseInfo.setStart_cmd(ifNull(processInfo.getStartCmd()));
        processBaseInfo.setStop_cmd(ifNull(processInfo.getStopCmd()));
        processBaseInfo.setRestart_cmd(ifNull(processInfo.getRestartCmd()));
        processBaseInfo.setReload_cmd(ifNull(processInfo.getReloadCmd()));
        processBaseInfo.setKill_cmd(ifNull(processInfo.getKillCmd()));
        processBaseInfo.setFunc_id(ifNull(processInfo.getFuncID()));
        processBaseInfo.setInstance_id(ifNull(processInfo.getInstanceID()));
        processBaseInfo.setValue_key(ifNull(processInfo.getValueKey()));
        return processBaseInfo;
    }

    private String ifNull(String str) {
        if (str == null) {
            return "";
        }
        return str;
    }

    public api_map_rsp getGetProcRst(String gseTaskId) {
        return GseRequestUtils.getGetProcRst("", gseTaskId);
    }
}










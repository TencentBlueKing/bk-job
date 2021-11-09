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

package com.tencent.bk.job.execute.api.esb.gse.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.execute.api.esb.gse.GseManageProcessResource;
import com.tencent.bk.job.execute.engine.model.GseTaskResponse;
import com.tencent.bk.job.execute.gse.GseApiExecutor;
import com.tencent.bk.job.execute.gse.model.GseProcessInfoDTO;
import com.tencent.bk.job.execute.gse.model.ProcessManageTypeEnum;
import com.tencent.bk.job.execute.model.esb.gse.EsbGseTaskResultDTO;
import com.tencent.bk.job.execute.model.esb.gse.req.EsbGseManageProcRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class GseManageProcessResourceImpl implements GseManageProcessResource {

    private final MessageI18nService i18nService;

    @Autowired
    public GseManageProcessResourceImpl(MessageI18nService i18nService) {
        this.i18nService = i18nService;
    }

    @Override
    public EsbResp<EsbGseTaskResultDTO> gseManageProc(EsbGseManageProcRequest request) {
        log.info("Gse manage process, request={}", request);
        if (!checkRequest(request)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }

        List<GseProcessInfoDTO> gseProcessInfos = new ArrayList<>();
        for (EsbGseManageProcRequest.ManageProcessInfoDTO manageProcInfo : request.getProcessInfos()) {
            GseProcessInfoDTO gseProcessInfo = buildGseProcessInfoDTO(manageProcInfo);
            gseProcessInfos.add(gseProcessInfo);
        }
        GseTaskResponse resp = new GseApiExecutor("")
            .processOperateProcess(gseProcessInfos, request.getOpType());

        if (resp == null || resp.getErrorCode() != GseTaskResponse.ERROR_CODE_SUCCESS) {
            throw new InvalidParamException(ErrorCode.GSE_ERROR);
        }
        EsbGseTaskResultDTO result = new EsbGseTaskResultDTO();
        result.setGseTaskId(resp.getGseTaskId());
        return EsbResp.buildSuccessResp(result);
    }

    private GseProcessInfoDTO buildGseProcessInfoDTO(EsbGseManageProcRequest.ManageProcessInfoDTO manageProcInfo) {
        GseProcessInfoDTO gseProcessInfo = new GseProcessInfoDTO();
        String procName = manageProcInfo.getProcName();
        String contact = manageProcInfo.getProcOwner();
        gseProcessInfo.setIpList(manageProcInfo.getIpList().stream()
            .map(ipDTO -> new IpDTO(ipDTO.getCloudAreaId(), ipDTO.getIp())).collect(Collectors.toList()));
        gseProcessInfo.setProcName(procName);
        gseProcessInfo.setContact(contact);

        String account = manageProcInfo.getAccount();
        if (StringUtils.isBlank(account)) {
            account = "root";
        }
        gseProcessInfo.setUserName(account);
        gseProcessInfo.setSetupPath(getDefaultIfNull(manageProcInfo.getSetupPath(), ""));
        gseProcessInfo.setPidPath(getDefaultIfNull(manageProcInfo.getPidPath(), ""));
        gseProcessInfo.setCfgPath(getDefaultIfNull(manageProcInfo.getCfgPath(), ""));
        gseProcessInfo.setLogPath(getDefaultIfNull(manageProcInfo.getLogPath(), ""));
        gseProcessInfo.setStartCmd(getDefaultIfNull(manageProcInfo.getStartCmd(), ""));
        gseProcessInfo.setStopCmd(getDefaultIfNull(manageProcInfo.getStopCmd(), ""));
        gseProcessInfo.setRestartCmd(getDefaultIfNull(manageProcInfo.getRestartCmd(), ""));
        gseProcessInfo.setKillCmd(getDefaultIfNull(manageProcInfo.getKillCmd(), ""));
        gseProcessInfo.setReloadCmd(getDefaultIfNull(manageProcInfo.getReloadCmd(), ""));
        gseProcessInfo.setFuncID(getDefaultIfNull(manageProcInfo.getFuncId(), ""));
        gseProcessInfo.setInstanceID(getDefaultIfNull(manageProcInfo.getInstanceId(), ""));
        gseProcessInfo.setValueKey(getDefaultIfNull(manageProcInfo.getValueKey(), ""));
        gseProcessInfo.setType(getDefaultIfNull(manageProcInfo.getType(), 0));
        Integer cpuLmt = manageProcInfo.getCpuLmt();
        if (cpuLmt == null || cpuLmt < 0 || cpuLmt > 100) {
            cpuLmt = 0;
        }
        gseProcessInfo.setCpuLmt(cpuLmt);
        Integer memLmt = manageProcInfo.getMemLmt();
        if (memLmt == null || memLmt < 0 || memLmt > 100) {
            memLmt = 0;
        }
        gseProcessInfo.setMemLmt(memLmt);
        gseProcessInfo.setCycleTime(getDefaultIfNull(manageProcInfo.getCycleTime(), 0));
        gseProcessInfo.setInstanceNum(getDefaultIfNull(manageProcInfo.getInstanceNum(), 0));
        gseProcessInfo.setStartCheckBeginTime(getDefaultIfNull(manageProcInfo.getStartCheckBeginTime(), 0));
        gseProcessInfo.setStartCheckEndTime(getDefaultIfNull(manageProcInfo.getStartCheckEndTime(), 0));
        gseProcessInfo.setOpTimeOut(getDefaultIfNull(manageProcInfo.getOpTimeout(), 0));

        return gseProcessInfo;
    }

    private int getDefaultIfNull(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String getDefaultIfNull(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    private boolean checkRequest(EsbGseManageProcRequest request) {
        if (request.getAppId() == null || request.getAppId() <= 0) {
            log.warn("AppId is empty!");
            return false;
        }
        if (request.getOpType() == null) {
            log.warn("OpType is empty!");
            return false;
        }
        if (!isManageProcTypeValid(request.getOpType())) {
            log.warn("OpType:{} is invalid!", request.getOpType());
            return false;
        }
        if (request.getProcessInfos() == null || request.getProcessInfos().isEmpty()) {
            log.warn("ProcessInfos is empty!");
            return false;
        }
        for (EsbGseManageProcRequest.ManageProcessInfoDTO processInfo : request.getProcessInfos()) {
            if (StringUtils.isBlank(processInfo.getSetupPath()) || StringUtils.isBlank(processInfo.getProcName())
                || StringUtils.isBlank(processInfo.getPidPath())) {
                log.warn("ProcessInfo setup_path|proc_name|pid_path is empty!");
                return false;
            }
            if (processInfo.getIpList() == null || processInfo.getIpList().isEmpty()) {
                log.warn("ProcessInfo ip_list is empty!");
                return false;
            }

        }
        return true;
    }

    private boolean isManageProcTypeValid(Integer opType) {
        for (ProcessManageTypeEnum manageType : ProcessManageTypeEnum.values()) {
            if (opType.equals(manageType.getValue())) {
                return true;
            }
        }
        return false;
    }
}

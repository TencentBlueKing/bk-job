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
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.api.esb.gse.GseOperateProcessV2Resource;
import com.tencent.bk.job.execute.engine.model.GseTaskResponse;
import com.tencent.bk.job.execute.gse.GseApiExecutor;
import com.tencent.bk.job.execute.gse.model.GseProcessInfoDTO;
import com.tencent.bk.job.execute.gse.model.ProcessOperateTypeEnum;
import com.tencent.bk.job.execute.model.esb.gse.EsbGseTaskResultDTO;
import com.tencent.bk.job.execute.model.esb.gse.EsbProcessInfoDTO;
import com.tencent.bk.job.execute.model.esb.gse.req.EsbGseOperateProcessRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class GseOperateProcessV2ResourceImpl implements GseOperateProcessV2Resource {

    private final MessageI18nService i18nService;

    @Autowired
    public GseOperateProcessV2ResourceImpl(MessageI18nService i18nService) {
        this.i18nService = i18nService;
    }

    @Override
    public EsbResp<EsbGseTaskResultDTO> gseOperateProcessV2(EsbGseOperateProcessRequest request) {
        log.info("Gse operate process v2, request={}", request);
        if (!checkRequest(request)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }

        List<GseProcessInfoDTO> gseProcessInfos = new ArrayList<>();
        for (EsbProcessInfoDTO processInfo : request.getProcessInfos()) {
            GseProcessInfoDTO gseProcessInfo = new GseProcessInfoDTO();
            gseProcessInfo.setIpList(processInfo.getIpList().stream().map(ipDTO -> new HostDTO(ipDTO.getBkCloudId(),
                ipDTO.getIp())).collect(Collectors.toList()));
            String setupPath = processInfo.getSetupPath();
            String procName = processInfo.getProcName();

            String account = processInfo.getAccount();
            if (StringUtils.isBlank(account)) {
                account = "root";
            }
            gseProcessInfo.setUserName(account);
            gseProcessInfo.setProcName(procName);// bk_gse_unifyTlogc
            gseProcessInfo.setSetupPath(setupPath);// /usr/local/gse/gseagent/plugins/unifyTlogc/sbin
            gseProcessInfo.setCfgPath(processInfo.getCfgPath());// /usr/local/gse/gseagent/plugins/unifyTlogc/conf/
            gseProcessInfo.setLogPath(processInfo.getLogPath());// /usr/local/gse/gseagent/plugins/unifyTlogc/log/
            gseProcessInfo.setPidPath(processInfo.getPidPath());// /usr/local/gse/gseagent/plugins/unifyTlogc/log
            // /unifyTlogc.pid
            gseProcessInfo.setContact(processInfo.getContact());
            gseProcessInfo.setStartCmd(processInfo.getStartCmd());
            gseProcessInfo.setStopCmd(processInfo.getStopCmd());
            gseProcessInfo.setRestartCmd(processInfo.getRestartCmd());
            gseProcessInfo.setReloadCmd(processInfo.getReloadCmd());
            gseProcessInfo.setKillCmd(processInfo.getKillCmd());
            gseProcessInfo.setFuncID(processInfo.getFuncId());
            gseProcessInfo.setInstanceID(processInfo.getInstanceId());
            gseProcessInfo.setValueKey(processInfo.getValueKey());

            gseProcessInfo.setType(processInfo.getType());//常驻进程托管
            gseProcessInfo.setCpuLmt(processInfo.getCpuLmt());
            gseProcessInfo.setMemLmt(processInfo.getMemLmt());
            gseProcessInfo.setCycleTime(processInfo.getCycleTime());
            gseProcessInfo.setInstanceNum(processInfo.getInstanceNum());
            gseProcessInfo.setStartCheckBeginTime(processInfo.getStartCheckBeginTime());
            gseProcessInfo.setStartCheckEndTime(processInfo.getStartCheckBeginTime());
            gseProcessInfo.setOpTimeOut(processInfo.getOpTimeout());//5-10

            gseProcessInfos.add(gseProcessInfo);
        }
        GseTaskResponse resp = new GseApiExecutor("")
            .processOperateProcess(gseProcessInfos, request.getOpType());

        if (resp == null || resp.getErrorCode() != GseTaskResponse.ERROR_CODE_SUCCESS) {
            throw new InternalException(ErrorCode.GSE_ERROR);
        }
        EsbGseTaskResultDTO result = new EsbGseTaskResultDTO();
        result.setGseTaskId(resp.getGseTaskId());
        return EsbResp.buildSuccessResp(result);
    }

    private boolean checkRequest(EsbGseOperateProcessRequest request) {
        if (request.getAppId() == null || request.getAppId() <= 0) {
            log.warn("AppId is empty!");
            return false;
        }
        if (request.getOpType() == null) {
            log.warn("OpType is empty!");
            return false;
        }
        if (!isOperateTypeValid(request.getOpType())) {
            log.warn("OpType:{} is invalid!", request.getOpType());
            return false;
        }
        if (request.getProcessInfos() == null || request.getProcessInfos().isEmpty()) {
            log.warn("ProcessInfos is empty!");
            return false;
        }
        for (EsbProcessInfoDTO processInfo : request.getProcessInfos()) {
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

    private boolean isOperateTypeValid(Integer opType) {
        for (ProcessOperateTypeEnum operateType : ProcessOperateTypeEnum.values()) {
            if (opType.equals(operateType.getValue())) {
                return true;
            }
        }
        return false;
    }
}

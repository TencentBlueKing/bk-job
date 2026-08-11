/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.crontab.service;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.CmdbTopoNodeDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.service.CommonAppService;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.crontab.api.common.CronCheckUtil;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.model.dto.CronJobVariableDTO;
import com.tencent.bk.job.crontab.model.esb.v4.req.V4SaveCronRequest;
import com.tencent.bk.job.crontab.model.inner.ServerDTO;
import com.tencent.bk.job.crontab.util.CronExpressionUtil;
import com.tencent.bk.job.execute.model.esb.v4.req.OpenApiV4HostDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4GlobalVarDTO;
import com.tencent.bk.job.manage.api.inner.ServiceTaskPlanResource;
import com.tencent.bk.job.manage.model.inner.ServiceTaskVariableDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * v4 保存定时任务请求 -> 内部定时任务模型的转换。
 * <p>
 * 仓库里暂无 v4 直接保存定时任务接口，本转换器是从零新写的一份，参照 v3 的
 * EsbCronJobV3ResourceImpl.saveCron，并按 v4 协议做了字段调整（去 bk_biz_id、主机类全局变量
 * 取值改用 v4 执行目标结构）。
 * <p>
 * v3 里散落在请求体分组校验（EsbSaveCronV3RequestSequenceProvider）与 Resource 层的校验，
 * 在这里收敛为一处，供审批预检（dryRun）与放行执行共用，保证两次调用不产生行为漂移。
 * <p>
 * TODO 后续补齐 v4 直接保存定时任务接口后，v3 的转换逻辑应改为委托本转换器，合并为一份实现，
 * 避免 v3/v4 两份转换长期并存导致行为分叉。
 */
@Slf4j
@Service
public class V4SaveCronRequestConverter {

    private static final int MAX_CRON_NAME_LENGTH = 60;

    private final ServiceTaskPlanResource taskPlanResource;

    private final CommonAppService commonAppService;

    @Autowired
    public V4SaveCronRequestConverter(ServiceTaskPlanResource taskPlanResource,
                                      CommonAppService commonAppService) {
        this.taskPlanResource = taskPlanResource;
        this.commonAppService = commonAppService;
    }

    /**
     * 是否为更新已有定时任务
     */
    public static boolean isUpdate(V4SaveCronRequest request) {
        return request.getId() != null && request.getId() > 0;
    }

    /**
     * 校验并把 v4 保存定时任务请求转换为定时任务信息
     *
     * @param request  v4 请求
     * @param operator 操作人
     * @return 定时任务信息
     * @throws InvalidParamException 请求参数不合法
     */
    public CronJobInfoDTO convert(V4SaveCronRequest request, User operator) {
        validate(request);

        String username = operator == null ? null : operator.getUsername();
        CronJobInfoDTO cronJobInfo = new CronJobInfoDTO();
        cronJobInfo.setId(request.getId());
        cronJobInfo.setAppId(request.getAppId());
        cronJobInfo.setName(request.getName());
        cronJobInfo.setTaskPlanId(request.getPlanId());
        cronJobInfo.setCronExpression(CronExpressionUtil.fixExpressionForQuartz(request.getCronExpression()));
        cronJobInfo.setExecuteTime(request.getExecuteTime());
        cronJobInfo.setExecuteTimeZone(resolveExecuteTimeZone(request));
        if (request.getGlobalVarList() != null) {
            cronJobInfo.setVariableValue(convertGlobalVars(request.getPlanId(), request.getGlobalVarList()));
        }
        if (!isUpdate(request)) {
            cronJobInfo.setCreator(username);
            cronJobInfo.setDelete(false);
        }
        // 保存不改变启停状态，启停由单独的接口负责
        cronJobInfo.setEnable(false);
        cronJobInfo.setLastModifyUser(username);
        cronJobInfo.setLastModifyTime(DateUtils.currentTimeSeconds());
        return cronJobInfo;
    }

    public void validate(V4SaveCronRequest request) {
        if (isUpdate(request)) {
            validateForUpdate(request);
        } else {
            validateForCreate(request);
        }
        if (StringUtils.isNotBlank(request.getName()) && request.getName().length() > MAX_CRON_NAME_LENGTH) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"name", "name length must not exceed " + MAX_CRON_NAME_LENGTH});
        }
        if (StringUtils.isNotBlank(request.getCronExpression())) {
            CronCheckUtil.checkCronExpression(request.getCronExpression(), "expression");
        }
    }

    private void validateForCreate(V4SaveCronRequest request) {
        if (request.getPlanId() == null || request.getPlanId() <= 0) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"job_plan_id", "job_plan_id is required when creating cron"});
        }
        if (StringUtils.isBlank(request.getName())) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"name", "name is required when creating cron"});
        }
        if (StringUtils.isBlank(request.getCronExpression())
            && (request.getExecuteTime() == null || request.getExecuteTime() <= 0)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"expression/execute_time", "expression/execute_time cannot both be null or invalid"});
        }
    }

    private void validateForUpdate(V4SaveCronRequest request) {
        boolean hasChange = (request.getPlanId() != null && request.getPlanId() > 0)
            || StringUtils.isNotBlank(request.getName())
            || StringUtils.isNotBlank(request.getCronExpression())
            || (request.getExecuteTime() != null && request.getExecuteTime() > 0);
        if (!hasChange) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON, new String[]{
                "job_plan_id/name/expression/execute_time",
                "At least one of job_plan_id/name/expression/execute_time must be given to update cron "
                    + request.getId()});
        }
    }

    /**
     * 时区缺省时使用 业务时区 > 服务器时区
     */
    private String resolveExecuteTimeZone(V4SaveCronRequest request) {
        String executeTimeZone = request.getExecuteTimeZone();
        if (StringUtils.isNotBlank(executeTimeZone)) {
            return executeTimeZone;
        }
        executeTimeZone = commonAppService.getAppTimeZoneById(request.getAppId());
        if (StringUtils.isBlank(executeTimeZone)) {
            executeTimeZone = ZoneId.systemDefault().getId();
        }
        return executeTimeZone;
    }

    private List<CronJobVariableDTO> convertGlobalVars(Long planId, List<V4GlobalVarDTO> globalVars) {
        List<CronJobVariableDTO> variables = new ArrayList<>();
        for (V4GlobalVarDTO globalVar : globalVars) {
            ServiceTaskVariableDTO planVariable = resolvePlanVariable(planId, globalVar);
            CronJobVariableDTO variable = new CronJobVariableDTO();
            variable.setId(planVariable.getId());
            variable.setName(planVariable.getName());
            if (planVariable.getType() != null) {
                variable.setType(TaskVariableTypeEnum.valOf(planVariable.getType()));
            }
            variable.setValue(globalVar.getValue());
            variable.setServer(convertExecuteTarget(globalVar.getExecuteTarget()));
            variables.add(variable);
        }
        return variables;
    }

    /**
     * 用执行方案里的变量定义校正 id / name / type：只给 name 时按 name 解析，给了 id 则一律以 id 解析出的为准
     */
    private ServiceTaskVariableDTO resolvePlanVariable(Long planId, V4GlobalVarDTO globalVar) {
        Long id = globalVar.getId();
        String name = globalVar.getName();
        if (id == null && StringUtils.isBlank(name)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_REASON,
                new String[]{"id/name of globalVar cannot be null/blank at the same time"});
        }
        InternalResponse<ServiceTaskVariableDTO> resp = id == null
            ? taskPlanResource.getGlobalVarByName(planId, name)
            : taskPlanResource.getGlobalVarById(planId, id);
        if (!resp.isSuccess()) {
            throw new InternalException(resp.getCode());
        }
        if (id != null && StringUtils.isNotBlank(name) && !name.equals(resp.getData().getName())) {
            log.info("Ignore given name {}, use name {} parsed by id", name, resp.getData().getName());
        }
        return resp.getData();
    }

    /**
     * v4 执行目标 -> 定时任务的主机变量取值。
     * <p>
     * 定时任务的主机变量只能保存静态主机 / 动态分组 / 拓扑节点，无法承载容器过滤器（过滤器需在执行时
     * 才能解析成具体容器），因此传了容器过滤器一律判为非法参数，与 v3 只支持 server 结构的语义保持一致。
     */
    private ServerDTO convertExecuteTarget(V4ExecuteTargetDTO executeTarget) {
        if (executeTarget == null || executeTarget.isTargetEmpty()) {
            return null;
        }
        if (CollectionUtils.isNotEmpty(executeTarget.getKubeContainerFilters())) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON, new String[]{
                "global_var.execute_target.kube_container_filters",
                "Cron global var does not support kube container filters"});
        }
        ServerDTO server = new ServerDTO();
        if (CollectionUtils.isNotEmpty(executeTarget.getHostList())) {
            List<HostDTO> hosts = new ArrayList<>();
            for (OpenApiV4HostDTO host : executeTarget.getHostList()) {
                // 优先使用 hostId
                if (host.getBkHostId() != null) {
                    hosts.add(HostDTO.fromHostId(host.getBkHostId()));
                } else {
                    hosts.add(new HostDTO(host.getBkCloudId(), host.getIp()));
                }
            }
            server.setIps(hosts);
        }
        if (CollectionUtils.isNotEmpty(executeTarget.getDynamicGroups())) {
            List<String> dynamicGroupIds = new ArrayList<>();
            executeTarget.getDynamicGroups().forEach(group -> dynamicGroupIds.add(group.getId()));
            server.setDynamicGroupIds(dynamicGroupIds);
        }
        if (CollectionUtils.isNotEmpty(executeTarget.getTopoNodes())) {
            List<CmdbTopoNodeDTO> topoNodes = new ArrayList<>();
            executeTarget.getTopoNodes().forEach(
                topoNode -> topoNodes.add(new CmdbTopoNodeDTO(topoNode.getId(), topoNode.getNodeType())));
            server.setTopoNodes(topoNodes);
        }
        return server;
    }
}

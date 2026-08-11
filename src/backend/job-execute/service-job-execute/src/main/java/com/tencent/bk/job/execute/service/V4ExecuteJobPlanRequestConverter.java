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

package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.TaskExecuteParam;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteJobPlanRequest;
import com.tencent.bk.job.execute.model.esb.v4.req.V4GlobalVarDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * v4 启动执行方案请求 -> 内部执行模型的转换。
 * <p>
 * 仓库里暂无 v4 直接启动执行方案接口，本转换器是从零新写的一份，参照 v3 的
 * EsbExecuteJobPlanV3ResourceImpl，并按 v4 协议做了字段调整（去 bk_biz_id、主机类全局变量取值
 * 改用 v4 执行目标结构）。
 * <p>
 * 校验与转换放在一起，供审批预检（dryRun）与放行执行共用，保证两次调用不产生行为漂移。
 * <p>
 * TODO 后续补齐 v4 直接启动执行方案接口后，v3 的转换逻辑应改为委托本转换器，合并为一份实现，
 * 避免 v3/v4 两份转换长期并存导致行为分叉。
 */
@Slf4j
public class V4ExecuteJobPlanRequestConverter {

    private V4ExecuteJobPlanRequestConverter() {
    }

    /**
     * 校验并把 v4 启动执行方案请求转换为作业执行参数
     *
     * @param request  v4 请求
     * @param operator 操作人
     * @param appCode  调用方 appCode
     * @param dryRun   是否只做预检，不产生任何副作用
     * @return 作业执行参数，cronTaskId 固定为 null，因此 skipAuth 必然不生效
     * @throws InvalidParamException 请求参数不合法
     */
    public static TaskExecuteParam convert(V4ExecuteJobPlanRequest request,
                                           User operator,
                                           String appCode,
                                           boolean dryRun) {
        ValidateResult checkResult = validate(request);
        if (!checkResult.isPass()) {
            throw new InvalidParamException(checkResult);
        }

        TaskExecuteParam param = TaskExecuteParam.builder()
            .appId(request.getAppId())
            .planId(request.getPlanId())
            .operator(operator)
            .executeVariableValues(convertGlobalVars(request.getGlobalVars()))
            .startupMode(TaskStartupModeEnum.API)
            .callbackUrl(request.getCallbackUrl())
            .appCode(appCode)
            .startTask(request.getStartTask())
            .dryRun(dryRun)
            .build();
        param.assertDryRunNotSkipAuth();
        return param;
    }

    public static ValidateResult validate(V4ExecuteJobPlanRequest request) {
        if (request.getPlanId() == null || request.getPlanId() <= 0) {
            log.warn("Execute job plan, planId is empty!");
            return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "job_plan_id");
        }
        if (CollectionUtils.isNotEmpty(request.getGlobalVars())) {
            for (V4GlobalVarDTO globalVar : request.getGlobalVars()) {
                if ((globalVar.getId() == null || globalVar.getId() <= 0)
                    && StringUtils.isBlank(globalVar.getName())) {
                    log.warn("Execute job plan, both variable id and name are empty");
                    return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME,
                        "global_var.id|global_var.name");
                }
            }
        }
        return ValidateResult.pass();
    }

    private static List<TaskVariableDTO> convertGlobalVars(List<V4GlobalVarDTO> globalVars) {
        List<TaskVariableDTO> variables = new ArrayList<>();
        if (CollectionUtils.isEmpty(globalVars)) {
            return variables;
        }
        for (V4GlobalVarDTO globalVar : globalVars) {
            TaskVariableDTO variable = new TaskVariableDTO();
            variable.setId(globalVar.getId());
            variable.setName(globalVar.getName());
            ExecuteTargetDTO executeTarget =
                V4ExecuteTargetConverter.v4ToExecuteTargetDTO(globalVar.getExecuteTarget());
            // 主机类变量用 execute_target 传值，其余类型用 value 传值
            if (StringUtils.isEmpty(globalVar.getValue()) && executeTarget != null) {
                variable.setExecuteTarget(executeTarget);
            } else {
                variable.setValue(globalVar.getValue());
            }
            variables.add(variable);
        }
        return variables;
    }
}

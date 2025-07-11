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

package com.tencent.bk.job.common.service.feature.strategy;

import com.tencent.bk.job.common.util.feature.FeatureExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * 按照作业实例属性灰度策略
 */
@Slf4j
public class JobInstanceAttrToggleStrategy extends AbstractToggleStrategy {
    /**
     * 特性开关开启策略ID
     */
    public static final String STRATEGY_ID = "JobInstanceAttrToggleStrategy";

    /**
     * 策略初始化参数
     */
    public static final String INIT_PARAM_REQUIRE_ALL_GSE_V2_AGENT_AVAILABLE = "requireAllGseV2AgentAvailable";
    public static final String INIT_PARAM_REQUIRE_ANY_GSE_V2_AGENT_AVAILABLE = "requireAnyGseV2AgentAvailable";
    public static final String INIT_PARAM_STARTUP_MODES = "startupModes";
    public static final String INIT_PARAM_OPERATORS = "operators";

    /**
     * 灰度策略上下文参数
     */
    public static final String CTX_PARAM_IS_ALL_GSE_V2_AGENT_AVAILABLE = "isAllGseV2AgentAvailable";
    public static final String CTX_PARAM_IS_ANY_GSE_V2_AGENT_AVAILABLE = "isAnyGseV2AgentAvailable";
    public static final String CTX_PARAM_STARTUP_MODE = "startupMode";
    public static final String CTX_PARAM_OPERATOR = "operator";

    /**
     * 灰度策略生效条件 - 所有 GSE v2 agent 可用
     */
    private Boolean requireAllGseV2AgentAvailable;
    /**
     * 灰度策略生效条件 - 任一 GSE v2 agent 可用
     */
    private Boolean requireAnyGseV2AgentAvailable;
    /**
     * 灰度策略生效条件 - 任务启动方式，web: job 页面启动; api: 第三方 API 调用；cron: 定时触发
     */
    private Set<String> startupModes;
    /**
     * 灰度策略生效条件 - 操作人列表
     */
    private Set<String> operators;


    public JobInstanceAttrToggleStrategy(Map<String, String> initParams) {
        super(STRATEGY_ID, initParams);
        String requireAllGseV2AgentAvailableValue = initParams.get(INIT_PARAM_REQUIRE_ALL_GSE_V2_AGENT_AVAILABLE);
        if (StringUtils.isNotBlank(requireAllGseV2AgentAvailableValue)) {
            requireAllGseV2AgentAvailable = Boolean.valueOf(requireAllGseV2AgentAvailableValue);
        }

        String requireAnyGseV2AgentAvailableValue = initParams.get(INIT_PARAM_REQUIRE_ANY_GSE_V2_AGENT_AVAILABLE);
        if (StringUtils.isNotBlank(requireAnyGseV2AgentAvailableValue)) {
            requireAnyGseV2AgentAvailable = Boolean.valueOf(requireAnyGseV2AgentAvailableValue);
        }

        String startupModesValue = initParams.get(INIT_PARAM_STARTUP_MODES);
        if (StringUtils.isNotBlank(startupModesValue)) {
            startupModes = Arrays.stream(startupModesValue.split(",")).collect(Collectors.toSet());
        }

        String operatorValue = initParams.get(INIT_PARAM_OPERATORS);
        if (StringUtils.isNotBlank(operatorValue)) {
            operators = Arrays.stream(operatorValue.split(",")).collect(Collectors.toSet());
        }
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public boolean evaluate(String featureId, FeatureExecutionContext ctx) {
        if (requireAllGseV2AgentAvailable != null && requireAllGseV2AgentAvailable) {
            boolean checkResult = checkRequiredContextParam(ctx, CTX_PARAM_IS_ALL_GSE_V2_AGENT_AVAILABLE);
            if (!checkResult) {
                return false;
            }
            boolean isAllAgentV2Available = (boolean) ctx.getParam(CTX_PARAM_IS_ALL_GSE_V2_AGENT_AVAILABLE);
            if (!isAllAgentV2Available) {
                return false;
            }
        }
        if (requireAnyGseV2AgentAvailable != null && requireAnyGseV2AgentAvailable) {
            boolean checkResult = checkRequiredContextParam(ctx, CTX_PARAM_IS_ANY_GSE_V2_AGENT_AVAILABLE);
            if (!checkResult) {
                return false;
            }
            boolean isAnyAgentV2Available = (boolean) ctx.getParam(CTX_PARAM_IS_ANY_GSE_V2_AGENT_AVAILABLE);
            if (!isAnyAgentV2Available) {
                return false;
            }
        }
        if (CollectionUtils.isNotEmpty(startupModes)) {
            boolean checkResult = checkRequiredContextParam(ctx, CTX_PARAM_STARTUP_MODE);
            if (!checkResult) {
                return false;
            }
            String startupMode = (String) ctx.getParam(CTX_PARAM_STARTUP_MODE);
            if (!startupModes.contains(startupMode)) {
                return false;
            }
        }
        if (CollectionUtils.isNotEmpty(operators)) {
            boolean checkResult = checkRequiredContextParam(ctx, CTX_PARAM_OPERATOR);
            if (!checkResult) {
                return false;
            }
            String operator = (String) ctx.getParam(CTX_PARAM_OPERATOR);
            return operators.contains(operator);
        }
        return true;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", JobInstanceAttrToggleStrategy.class.getSimpleName()
            + "[", "]")
            .add("id='" + id + "'")
            .add("initParams=" + initParams)
            .toString();
    }
}

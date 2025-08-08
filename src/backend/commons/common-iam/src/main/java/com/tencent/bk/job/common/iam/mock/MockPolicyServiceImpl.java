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

package com.tencent.bk.job.common.iam.mock;

import com.tencent.bk.sdk.iam.constants.ExpressionOperationEnum;
import com.tencent.bk.sdk.iam.dto.ExpressionWithResourceDTO;
import com.tencent.bk.sdk.iam.dto.action.ActionDTO;
import com.tencent.bk.sdk.iam.dto.action.ActionPolicyDTO;
import com.tencent.bk.sdk.iam.dto.expression.ExpressionDTO;
import com.tencent.bk.sdk.iam.dto.resource.ResourceDTO;
import com.tencent.bk.sdk.iam.service.PolicyService;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mock工具类：始终返回 ANY 的策略服务实现
 */
public class MockPolicyServiceImpl implements PolicyService {
    @Override
    public ExpressionDTO getPolicyByAction(String s, String s1, ActionDTO actionDTO, List<ResourceDTO> list) {
        ExpressionDTO expressionDTO = new ExpressionDTO();
        expressionDTO.setOperator(ExpressionOperationEnum.ANY);
        return expressionDTO;
    }

    @Override
    public List<ActionPolicyDTO> batchGetPolicyByActionList(String s, String s1, List<ActionDTO> list,
                                                            List<ResourceDTO> list1) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream().map(actionDTO -> {
            ActionPolicyDTO actionPolicyDTO = new ActionPolicyDTO();
            actionPolicyDTO.setActionId(actionDTO.getId());
            actionPolicyDTO.setCondition(getPolicyByAction(s, s1, actionDTO, list1));
            return actionPolicyDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public ExpressionWithResourceDTO batchGetPolicyAndAttribute(String s, String s1, ActionDTO actionDTO,
                                                                ResourceDTO resourceDTO, List<ResourceDTO> list) {
        ExpressionWithResourceDTO expressionWithResourceDTO = new ExpressionWithResourceDTO();
        ExpressionDTO expressionDTO = new ExpressionDTO();
        expressionDTO.setOperator(ExpressionOperationEnum.ANY);
        expressionWithResourceDTO.setExpression(expressionDTO);
        expressionWithResourceDTO.setSystemInstanceMap(new HashMap<>());
        return expressionWithResourceDTO;
    }
}

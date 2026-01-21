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

package com.tencent.bk.job.execute.api.esb.v3;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.util.EsbDTOAppScopeMappingHelper;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.execute.model.GseTaskSimpleDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceRollingTaskDTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbTaskLinkV3DTO;
import com.tencent.bk.job.execute.service.GseTaskService;
import com.tencent.bk.job.execute.service.StepInstanceRollingTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class EsbQueryToolsV3ResourceImpl implements EsbQueryToolsV3Resource {
    private final StepInstanceService stepInstanceService;
    private final GseTaskService gseTaskService;
    private final AppAuthService appAuthService;
    private final AppScopeMappingService appScopeMappingService;
    private final StepInstanceRollingTaskService stepInstanceRollingTaskService;

    @Value("${job.web.url:}")
    private String jobWebUrl;

    /**
     * 快速执行链接地址
     */
    private static final String FAST_LINK = "%s/api_execute/%s?stepInstanceId=%s&retryCount=%s&batch=%s";

    /**
     * 作业执行链接地址
     */
    private static final String TASK_LINK = "%s/api_execute_step/%s/%s?retryCount=%s&batch=%s";

    @Autowired
    public EsbQueryToolsV3ResourceImpl(StepInstanceService stepInstanceService,
                                       GseTaskService gseTaskService,
                                       AppAuthService appAuthService,
                                       AppScopeMappingService appScopeMappingService,
                                       StepInstanceRollingTaskService stepInstanceRollingTaskService) {
        this.stepInstanceService = stepInstanceService;
        this.gseTaskService = gseTaskService;
        this.appAuthService = appAuthService;
        this.appScopeMappingService = appScopeMappingService;
        this.stepInstanceRollingTaskService = stepInstanceRollingTaskService;
    }

    @Override
    public EsbResp<List<EsbTaskLinkV3DTO>> queryGSETaskByStep(String username, String appCode, Long stepInstanceId) {
        StepInstanceBaseDTO stepInstance = stepInstanceService.getBaseStepInstanceById(stepInstanceId);
        if (stepInstance == null) {
            String reason = String.format("step not found by id: %s", stepInstanceId);
            return EsbResp.buildCommonFailResp(
                ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{String.valueOf(stepInstanceId), reason},
                null
            );
        }

        Long appId = stepInstance.getAppId();
        ResourceScope resourceScope = appScopeMappingService.getAppResourceScope(appId);
        AppResourceScope appResourceScope = new AppResourceScope(appId, resourceScope);
        // 鉴权
        auth(username, appResourceScope);

        List<GseTaskSimpleDTO> gseTaskResult = new ArrayList<>();
        List<StepInstanceRollingTaskDTO> stepInstanceRollingTaskDTOS =
            stepInstanceRollingTaskService.listRollingTasksByStep(stepInstance.getTaskInstanceId(), stepInstanceId);

        if (CollectionUtils.isNotEmpty(stepInstanceRollingTaskDTOS)) {
            // 步骤为滚动执行
            for (StepInstanceRollingTaskDTO stepInstanceRollingTaskDTO : stepInstanceRollingTaskDTOS) {
                List<GseTaskSimpleDTO> gseTaskSimpleDTOS = gseTaskService.listGseTaskSimpleInfo(
                    stepInstanceId,
                    stepInstanceRollingTaskDTO.getExecuteCount(),
                    stepInstanceRollingTaskDTO.getBatch()
                );
                gseTaskResult.addAll(gseTaskSimpleDTOS);
            }
        } else {
            // 步骤不是滚动执行
            gseTaskResult.addAll(gseTaskService.listGseTaskSimpleInfo(
                stepInstanceId,
                null,
                null
            ));
        }

        List<EsbTaskLinkV3DTO> taskLinks = new ArrayList<>();
        for (GseTaskSimpleDTO gseTaskSimpleDTO : gseTaskResult) {
            taskLinks.add(buildEsbTaskLinkV3DTO(
                appId,
                stepInstance,
                gseTaskSimpleDTO.getExecuteCount(),
                gseTaskSimpleDTO.getBatch(),
                gseTaskSimpleDTO.getGseTaskId()
            ));
        }
        return EsbResp.buildSuccessResp(taskLinks);
    }

    @Override
    public EsbResp<EsbTaskLinkV3DTO> queryJobInstanceByGseTask(String username, String appCode, String gseTaskId) {
        GseTaskSimpleDTO gseTask = gseTaskService.getGseTaskSimpleInfo(gseTaskId);
        if (gseTask == null) {
            String reason = String.format("gseTask not found by id: %s", gseTaskId);
            return EsbResp.buildCommonFailResp(
                ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{gseTaskId, reason},
                null
            );
        }

        StepInstanceBaseDTO stepInstanceBaseDTO = stepInstanceService.getBaseStepInstance(
            gseTask.getTaskInstanceId(),
            gseTask.getStepInstanceId()
        );
        Long appId = stepInstanceBaseDTO.getAppId();
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(appId);
        AppResourceScope appResourceScope = new AppResourceScope(appId, resourceScope);
        auth(username, appResourceScope);

        return EsbResp.buildSuccessResp(buildEsbTaskLinkV3DTO(
            appId,
            stepInstanceBaseDTO,
            gseTask.getExecuteCount(),
            gseTask.getBatch(),
            gseTaskId
        ));
    }

    private void auth(String username, AppResourceScope appResourceScope) {
        User user = JobContextUtil.getUser();
        AuthResult authResult = appAuthService.auth(
            user,
            ActionId.ACCESS_BUSINESS,
            appResourceScope
        );
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        authResult = appAuthService.auth(
            user,
            ActionId.VIEW_HISTORY,
            appResourceScope
        );
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
    }

    private EsbTaskLinkV3DTO buildEsbTaskLinkV3DTO(
        Long appId,
        StepInstanceBaseDTO stepInstanceBase,
        int retryCount,
        int batch,
        String gseTaskId
    ) {

        EsbTaskLinkV3DTO esbTaskLinkV3DTO = new EsbTaskLinkV3DTO();
        esbTaskLinkV3DTO.setJobInstanceId(stepInstanceBase.getTaskInstanceId());
        esbTaskLinkV3DTO.setStepInstanceId(stepInstanceBase.getId());
        EsbDTOAppScopeMappingHelper.fillEsbAppScopeDTOByAppId(appId, esbTaskLinkV3DTO);
        esbTaskLinkV3DTO.setRetryCount(retryCount);
        esbTaskLinkV3DTO.setBatch(batch);
        esbTaskLinkV3DTO.setGseTaskId(gseTaskId);
        esbTaskLinkV3DTO.setLink(buildLink(esbTaskLinkV3DTO, stepInstanceBase));
        return esbTaskLinkV3DTO;
    }

    private List<String> buildLink(EsbTaskLinkV3DTO esbTaskLinkV3DTO, StepInstanceBaseDTO stepInstanceBase) {
        List<String> links = new ArrayList<>();
        String linkTemplate = FAST_LINK;
        // 是作业执行
        if (stepInstanceBase.getStepId() != -1L) {
            linkTemplate = TASK_LINK;
        }

        String[] urls = jobWebUrl.split(",");
        for (String url : urls) {
            String link = String.format(
                linkTemplate,
                url,
                esbTaskLinkV3DTO.getJobInstanceId(),
                esbTaskLinkV3DTO.getStepInstanceId(),
                esbTaskLinkV3DTO.getRetryCount(),
                esbTaskLinkV3DTO.getBatch()
            );
            links.add(link);
        }
        return links;
    }
}

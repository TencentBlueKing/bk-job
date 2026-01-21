package com.tencent.bk.job.execute.api.web.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.execute.api.web.WebSearchToolsResource;
import com.tencent.bk.job.execute.model.GseTaskSimpleDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceRollingTaskDTO;
import com.tencent.bk.job.execute.model.web.vo.TaskLinkVO;
import com.tencent.bk.job.execute.service.GseTaskService;
import com.tencent.bk.job.execute.service.StepInstanceRollingTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class WebSearchToolsResourceImpl implements WebSearchToolsResource {
    private final StepInstanceService stepInstanceService;
    private final GseTaskService gseTaskService;
    private final AppAuthService appAuthService;
    private final AppScopeMappingService appScopeMappingService;
    private final StepInstanceRollingTaskService stepInstanceRollingTaskService;

    /**
     * 作业平台web访问地址，可配置多个，用","分隔
     */
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
    public WebSearchToolsResourceImpl(StepInstanceService stepInstanceService,
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
    public Response<TaskLinkVO> getTaskLink(String username,
                                            String gseTaskId) {
        GseTaskSimpleDTO gseTaskSimpleInfo = gseTaskService.getGseTaskSimpleInfo(gseTaskId);
        if (gseTaskSimpleInfo == null) {
            String errorMsg = "not found gseTask by " + gseTaskId;
            log.warn(errorMsg);
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{String.valueOf(gseTaskId), errorMsg});
        }
        StepInstanceBaseDTO stepInstanceBase = stepInstanceService.getBaseStepInstance(
            gseTaskSimpleInfo.getTaskInstanceId(), gseTaskSimpleInfo.getStepInstanceId());
        Long appId = stepInstanceBase.getAppId();
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(appId);
        AppResourceScope appResourceScope = new AppResourceScope(appId, resourceScope);
        // 鉴权
        auth(JobContextUtil.getUser(), appResourceScope);
        TaskLinkVO taskLinkVO = convertToTaskLinVO(resourceScope,
            stepInstanceBase,
            gseTaskSimpleInfo.getExecuteCount(),
            gseTaskSimpleInfo.getBatch(),
            gseTaskId);
        return Response.buildSuccessResp(taskLinkVO);
    }

    @Override
    public Response<List<TaskLinkVO>> getTaskLinkByStepId(String username,
                                                          Long stepInstanceId) {
        StepInstanceBaseDTO stepInstanceBase = stepInstanceService.getBaseStepInstanceById(stepInstanceId);
        if (stepInstanceBase == null) {
            String errorMsg = "not found StepInstance by " + stepInstanceId;
            log.warn(errorMsg);
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{String.valueOf(stepInstanceId), errorMsg});
        }
        Long appId = stepInstanceBase.getAppId();
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(appId);
        AppResourceScope appResourceScope = new AppResourceScope(appId, resourceScope);
        // 鉴权
        auth(JobContextUtil.getUser(), appResourceScope);

        List<StepInstanceRollingTaskDTO> stepInstanceRollingTaskDTOS =
            stepInstanceRollingTaskService.listRollingTasksByStep(stepInstanceBase.getTaskInstanceId(), stepInstanceId);
        List<GseTaskSimpleDTO> gseTaskDTOList = new ArrayList<>();
        // 是否滚动执行
        if (CollectionUtils.isNotEmpty(stepInstanceRollingTaskDTOS)) {
            for (StepInstanceRollingTaskDTO stepInstanceRollingTaskDTO : stepInstanceRollingTaskDTOS) {
                List<GseTaskSimpleDTO> gseTaskSimpleDTOList = gseTaskService.listGseTaskSimpleInfo(stepInstanceId,
                    stepInstanceRollingTaskDTO.getExecuteCount(),
                    stepInstanceRollingTaskDTO.getBatch());
                if (CollectionUtils.isNotEmpty(gseTaskSimpleDTOList)) {
                    gseTaskDTOList.addAll(gseTaskSimpleDTOList);
                }
            }
        } else {
            List<GseTaskSimpleDTO> gseTaskSimpleDTOList = gseTaskService.listGseTaskSimpleInfo(stepInstanceId,
                null,
                null);
            if (CollectionUtils.isNotEmpty(gseTaskSimpleDTOList)) {
                gseTaskDTOList.addAll(gseTaskSimpleDTOList);
            }
        }

        List<TaskLinkVO> taskLinkVOList = new ArrayList<>();
        for (GseTaskSimpleDTO gseTaskSimpleDTO : gseTaskDTOList) {
            taskLinkVOList.add(convertToTaskLinVO(resourceScope,
                stepInstanceBase,
                gseTaskSimpleDTO.getExecuteCount(),
                gseTaskSimpleDTO.getBatch(),
                gseTaskSimpleDTO.getGseTaskId()));
        }
        return Response.buildSuccessResp(taskLinkVOList);
    }

    private AuthResult auth(User user,
                            AppResourceScope appResourceScope) {
        AuthResult authResult = appAuthService.auth(user,
            ActionId.ACCESS_BUSINESS,
            appResourceScope);
        if (authResult.isPass()) {
            authResult = appAuthService.auth(user,
                ActionId.VIEW_HISTORY,
                appResourceScope);
        }
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        return authResult;
    }

    private TaskLinkVO convertToTaskLinVO(ResourceScope resourceScope,
                                          StepInstanceBaseDTO stepInstanceBase,
                                          int retryCount,
                                          int batch,
                                          String gseTaskId) {
        TaskLinkVO taskLinkVO = new TaskLinkVO();
        taskLinkVO.setScopeType(resourceScope.getType().getValue());
        taskLinkVO.setScopeId(resourceScope.getId());
        taskLinkVO.setAppId(stepInstanceBase.getAppId());
        taskLinkVO.setJobInstanceId(stepInstanceBase.getTaskInstanceId());
        taskLinkVO.setStepInstanceId(stepInstanceBase.getId());
        taskLinkVO.setRetryCount(retryCount);
        taskLinkVO.setBatch(batch);
        taskLinkVO.setGseTaskId(gseTaskId);
        taskLinkVO.setLink(buildLink(taskLinkVO, stepInstanceBase));
        return taskLinkVO;
    }

    /**
     * 拼接链接地址
     */
    private List<String> buildLink(TaskLinkVO taskLinkVO, StepInstanceBaseDTO stepInstanceBase) {
        List<String> links = new ArrayList();
        String linkTemplate = FAST_LINK;
        if (stepInstanceBase.getStepId() != -1L) {
            linkTemplate = TASK_LINK;
        }
        if (jobWebUrl.indexOf(",") != -1) {
            String[] jobWebUrls = jobWebUrl.split(",");
            for (String webUrl : jobWebUrls) {
                links.add(String.format(linkTemplate,
                    webUrl,
                    taskLinkVO.getJobInstanceId(),
                    taskLinkVO.getStepInstanceId(),
                    taskLinkVO.getRetryCount(),
                    taskLinkVO.getBatch()));
            }
        } else {
            links.add(String.format(linkTemplate,
                jobWebUrl,
                taskLinkVO.getJobInstanceId(),
                taskLinkVO.getStepInstanceId(),
                taskLinkVO.getRetryCount(),
                taskLinkVO.getBatch()));
        }
        return links;
    }
}

package com.tencent.bk.job.execute.api.web.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.execute.api.web.WebSearchToolsResource;
import com.tencent.bk.job.execute.model.GseTaskDTO;
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

    private final static Integer DEFAULT_EXECUTE_COUNT = 0;
    private final static Integer DEFAULT_BATCH = 0;

    /**
     * 作业平台web访问地址，可配置多个，用","分隔
     */
    @Value("${job.web.url:}")
    private String jobWebUrl;

    /**
     * 链接地址
     */
    private final static String linkTemplate = "%s/api_execute/%s?stepInstanceId=%s&executeCount=%s&batch=%s";

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
        Long stepInstanceId = gseTaskService.getStepInstanceId(gseTaskId);
        if (stepInstanceId == null) {
            log.warn("not found stepInstanceId by "+gseTaskId);
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM);
        }
        StepInstanceBaseDTO stepInstanceBase = stepInstanceService.getStepInstanceBase(stepInstanceId);
        Long appId = stepInstanceBase.getAppId();
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(appId);
        AppResourceScope appResourceScope = new AppResourceScope(appId, resourceScope);
        // 鉴权
        AuthResult authResult = auth(username, appResourceScope);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        TaskLinkVO taskLinkVO = convertToTaskLinVO(resourceScope,
            stepInstanceBase,
            stepInstanceBase.getExecuteCount(),
            stepInstanceBase.getBatch(),
            gseTaskId);
        return Response.buildSuccessResp(taskLinkVO);
    }

    @Override
    public Response<List<TaskLinkVO>> getTaskLinkByStepId(String username,
                                                          Long stepInstanceId) {
        StepInstanceBaseDTO stepInstanceBase = stepInstanceService.getStepInstanceBase(stepInstanceId);
        if (stepInstanceBase == null) {
            log.warn("not found StepInstance by "+stepInstanceId);
            return Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM);
        }
        Long appId = stepInstanceBase.getAppId();
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(appId);
        AppResourceScope appResourceScope = new AppResourceScope(appId, resourceScope);
        // 鉴权
        AuthResult authResult = auth(username, appResourceScope);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        List<StepInstanceRollingTaskDTO> stepInstanceRollingTaskDTOS =
            stepInstanceRollingTaskService.listRollingTasksByStep(stepInstanceId);
        List<GseTaskDTO> gseTaskDTOList = new ArrayList<>();
        // 是否滚动执行
        if (CollectionUtils.isNotEmpty(stepInstanceRollingTaskDTOS)) {
            for (StepInstanceRollingTaskDTO stepInstanceRollingTaskDTO : stepInstanceRollingTaskDTOS) {
                GseTaskDTO gseTaskDTO = gseTaskService.getGseTask(stepInstanceId,
                    stepInstanceRollingTaskDTO.getExecuteCount(),
                    stepInstanceRollingTaskDTO.getBatch());
                if (gseTaskDTO != null) {
                    gseTaskDTOList.add(gseTaskDTO);
                }
            }
        } else {
            GseTaskDTO gseTaskDTO = gseTaskService.getGseTask(stepInstanceId,
                DEFAULT_EXECUTE_COUNT,
                DEFAULT_BATCH);
            if (gseTaskDTO != null) {
                gseTaskDTOList.add(gseTaskDTO);
            }
        }

        List<TaskLinkVO> taskLinkVOList = new ArrayList<>();
        for (GseTaskDTO gseTaskDTO : gseTaskDTOList) {
            taskLinkVOList.add(convertToTaskLinVO(resourceScope,
                stepInstanceBase,
                gseTaskDTO.getExecuteCount(),
                gseTaskDTO.getBatch(),
                gseTaskDTO.getGseTaskId()));
        }
        return Response.buildSuccessResp(taskLinkVOList);
    }

    private AuthResult auth(String username,
                            AppResourceScope appResourceScope) {
        AuthResult authResult = appAuthService.auth(username,
            ActionId.ACCESS_BUSINESS,
            appResourceScope);
        if (authResult.isPass()) {
            authResult = appAuthService.auth(username,
                ActionId.VIEW_HISTORY,
                appResourceScope);
        }
        return authResult;
    }

    private TaskLinkVO convertToTaskLinVO(ResourceScope resourceScope,
                                          StepInstanceBaseDTO stepInstanceBase,
                                          int executeCount,
                                          int batch,
                                          String gseTaskId) {
        TaskLinkVO taskLinkVO = new TaskLinkVO();
        taskLinkVO.setScopeType(resourceScope.getType().getValue());
        taskLinkVO.setScopeId(resourceScope.getId());
        taskLinkVO.setAppId(stepInstanceBase.getAppId());
        taskLinkVO.setJobInstanceId(stepInstanceBase.getTaskInstanceId());
        taskLinkVO.setStepInstanceId(stepInstanceBase.getId());
        taskLinkVO.setExecuteCount(executeCount);
        taskLinkVO.setBatch(batch);
        taskLinkVO.setGseTaskId(gseTaskId);
        taskLinkVO.setLink(buildLink(taskLinkVO));
        return taskLinkVO;
    }

    /**
     * 拼接链接地址
     */
    private String buildLink(TaskLinkVO taskLinkVO) {
        if (jobWebUrl.indexOf(",") != -1) {
            String[] jobWebUrls = jobWebUrl.split(",");
            List<String> links = new ArrayList();
            for (String webUrl : jobWebUrls) {
                links.add(String.format(linkTemplate,
                    webUrl,
                    taskLinkVO.getJobInstanceId(),
                    taskLinkVO.getStepInstanceId(),
                    taskLinkVO.getExecuteCount(),
                    taskLinkVO.getBatch()));
            }
            return links.toString();
        } else {
            return String.format(linkTemplate,
                jobWebUrl,
                taskLinkVO.getJobInstanceId(),
                taskLinkVO.getStepInstanceId(),
                taskLinkVO.getExecuteCount(),
                taskLinkVO.getBatch());
        }
    }
}

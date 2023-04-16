package com.tencent.bk.job.execute.api.web.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.execute.api.web.WebSearchToolsResource;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.web.vo.TaskLinkVO;
import com.tencent.bk.job.execute.service.GseTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import lombok.extern.slf4j.Slf4j;
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
                                      AppScopeMappingService appScopeMappingService) {
        this.stepInstanceService = stepInstanceService;
        this.gseTaskService = gseTaskService;
        this.appAuthService = appAuthService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    public Response<TaskLinkVO> getTaskLink(String username, String gseTaskId) {
        Long stepInstanceId = gseTaskService.getStepInstanceId(gseTaskId);
        if (stepInstanceId == null) {
            throw new InternalException(gseTaskId + " does not exist", ErrorCode.ILLEGAL_PARAM);
        }
        StepInstanceBaseDTO stepInstanceBase = stepInstanceService.getStepInstanceBase(stepInstanceId);
        Long appId = stepInstanceBase.getAppId();
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(appId);
        AppResourceScope appResourceScope = new AppResourceScope(appId, resourceScope);
        // 鉴权
        AuthResult authResult = appAuthService.auth(username, ActionId.ACCESS_BUSINESS, appResourceScope);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        authResult = appAuthService.auth(username, ActionId.VIEW_HISTORY, appResourceScope);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        TaskLinkVO taskLinkVO = new TaskLinkVO();
        taskLinkVO.setScopeType(resourceScope.getType().getValue());
        taskLinkVO.setScopeId(resourceScope.getId());
        taskLinkVO.setAppId(stepInstanceBase.getAppId());
        taskLinkVO.setJobInstanceId(stepInstanceBase.getTaskInstanceId());
        taskLinkVO.setStepInstanceId(stepInstanceId);
        taskLinkVO.setExecuteCount(stepInstanceBase.getExecuteCount());
        taskLinkVO.setBatch(stepInstanceBase.getBatch());
        taskLinkVO.setGseTaskId(gseTaskId);
        taskLinkVO.setLink(buildLink(taskLinkVO));
        return Response.buildSuccessResp(taskLinkVO);
    }

    /**
     * 拼接链接地址
     */
    private String buildLink(TaskLinkVO taskLinkVO) {
        if (jobWebUrl.indexOf(",") != -1) {
            String[] jobWebUrls = jobWebUrl.split(",");
            List<String> links = new ArrayList();
            for (String webUrl : jobWebUrls) {
                links.add(String.format(linkTemplate, webUrl, taskLinkVO.getJobInstanceId(),
                    taskLinkVO.getStepInstanceId(), taskLinkVO.getExecuteCount(), taskLinkVO.getBatch()));
            }
            return links.toString();
        } else {
            return String.format(linkTemplate, jobWebUrl, taskLinkVO.getJobInstanceId(),
                taskLinkVO.getStepInstanceId(), taskLinkVO.getExecuteCount(), taskLinkVO.getBatch());
        }
    }
}

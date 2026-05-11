package com.tencent.bk.job.file_gateway.api.esb;

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.file_gateway.auth.FileSourceAuthService;
import com.tencent.bk.job.file_gateway.consts.WorkerSelectModeEnum;
import com.tencent.bk.job.file_gateway.consts.WorkerSelectScopeEnum;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceTypeDTO;
import com.tencent.bk.job.file_gateway.model.req.esb.v3.EsbCreateOrUpdateFileSourceV3Req;
import com.tencent.bk.job.file_gateway.model.req.esb.v3.EsbGetFileSourceDetailV3Req;
import com.tencent.bk.job.file_gateway.model.resp.esb.v3.EsbFileSourceSimpleInfoV3DTO;
import com.tencent.bk.job.file_gateway.model.resp.esb.v3.EsbFileSourceV3DTO;
import com.tencent.bk.job.file_gateway.service.FileSourceService;
import com.tencent.bk.job.file_gateway.service.validation.FileSourceValidateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@RestController
@Slf4j
public class EsbFileSourceV3ResourceImpl implements EsbFileSourceV3Resource {

    private final FileSourceService fileSourceService;
    private final AppScopeMappingService appScopeMappingService;
    private final FileSourceValidateService fileSourceValidateService;
    private final FileSourceAuthService fileSourceAuthService;

    @Autowired
    public EsbFileSourceV3ResourceImpl(FileSourceService fileSourceService,
                                       AppScopeMappingService appScopeMappingService,
                                       FileSourceValidateService fileSourceValidateService,
                                       FileSourceAuthService fileSourceAuthService) {
        this.fileSourceService = fileSourceService;
        this.appScopeMappingService = appScopeMappingService;
        this.fileSourceValidateService = fileSourceValidateService;
        this.fileSourceAuthService = fileSourceAuthService;
    }

    @Override
    @AuditEntry(actionId = ActionId.CREATE_FILE_SOURCE)
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "create_file_source"})
    public EsbResp<EsbFileSourceSimpleInfoV3DTO> createFileSource(
        String username,
        String appCode,
        @AuditRequestBody EsbCreateOrUpdateFileSourceV3Req req) {
        Long appId = req.getAppId();
        checkCreateParam(req);
        FileSourceDTO fileSourceDTO = buildFileSourceDTO(username, appId, null, req);
        FileSourceDTO createdFileSource = fileSourceService.saveFileSource(
            JobContextUtil.getUser(), appId, fileSourceDTO);
        return EsbResp.buildSuccessResp(new EsbFileSourceSimpleInfoV3DTO(createdFileSource.getId()));
    }

    @Override
    @AuditEntry(actionId = ActionId.MANAGE_FILE_SOURCE)
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "update_file_source"})
    public EsbResp<EsbFileSourceSimpleInfoV3DTO> updateFileSource(
        String username,
        String appCode,
        @AuditRequestBody EsbCreateOrUpdateFileSourceV3Req req) {
        Integer id = checkUpdateParamAndGetId(req);
        Long appId = req.getAppId();
        FileSourceDTO fileSourceDTO = buildFileSourceDTO(username, appId, id, req);
        FileSourceDTO updateFileSource = fileSourceService.updateFileSourceById(
            JobContextUtil.getUser(), appId, fileSourceDTO);
        return EsbResp.buildSuccessResp(new EsbFileSourceSimpleInfoV3DTO(updateFileSource.getId()));
    }

    @Override
    @AuditEntry(actionId = ActionId.VIEW_FILE_SOURCE)
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "get_file_source_detail"})
    public EsbResp<EsbFileSourceV3DTO> getFileSourceDetail(
        String username,
        String appCode,
        Long bizId,
        String scopeType,
        String scopeId,
        String code) {
        EsbGetFileSourceDetailV3Req req = new EsbGetFileSourceDetailV3Req();
        req.setBizId(bizId);
        req.setScopeType(scopeType);
        req.setScopeId(scopeId);
        req.setCode(code);
        req.fillAppResourceScope(appScopeMappingService);
        return getFileSourceDetailUsingPost(username, appCode, req);
    }

    @Override
    @AuditEntry(actionId = ActionId.VIEW_FILE_SOURCE)
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "get_file_source_detail"})
    public EsbResp<EsbFileSourceV3DTO> getFileSourceDetailUsingPost(
        String username,
        String appCode,
        @AuditRequestBody EsbGetFileSourceDetailV3Req req) {
        // POST 直接入口可能未补全 appId（GET 入口已先调用，重复调用幂等且安全）
        req.fillAppResourceScope(appScopeMappingService);
        Long appId = req.getAppId();

        // 先按 (appId, code) 拿到文件源，DAO 联合过滤天然保证业务隔离
        FileSourceDTO fileSourceDTO = fileSourceService.getFileSourceByCode(appId, req.getCode());
        if (fileSourceDTO == null) {
            throw new NotFoundException(ErrorCode.FAIL_TO_FIND_FILE_SOURCE_BY_CODE,
                new String[]{req.getCode()});
        }

        // IAM view_file_source 校验，与 Web 端 FileSourceServiceImpl#getFileSourceById(user,..) 对齐
        User user = JobContextUtil.getUser();
        fileSourceAuthService.authViewFileSource(
            user,
            new AppResourceScope(appId),
            fileSourceDTO.getId(),
            fileSourceDTO.getAlias()
        ).denyIfNoPermission();

        return EsbResp.buildSuccessResp(FileSourceDTO.toEsbFileSourceV3DTO(fileSourceDTO));
    }

    private void checkCommonParam(EsbCreateOrUpdateFileSourceV3Req req) {
        if (StringUtils.isBlank(req.getAlias())) {
            throw new InvalidParamException(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, new String[]{"alias"});
        }
        if (StringUtils.isBlank(req.getType())) {
            throw new InvalidParamException(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, new String[]{"type"});
        }
        if (StringUtils.isBlank(req.getCredentialId())) {
            throw new InvalidParamException(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, new String[]{"credential_id"});
        }
    }

    private void checkCreateParam(EsbCreateOrUpdateFileSourceV3Req req) {
        String code = req.getCode();
        FileSourceTypeDTO fileSourceTypeDTO = fileSourceService.getFileSourceTypeByCode(
            req.getType()
        );
        if (fileSourceTypeDTO == null) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, new String[]{"type"});
        }
        if (fileSourceService.existsCode(req.getAppId(), code)) {
            throw new FailedPreconditionException(ErrorCode.FILE_SOURCE_CODE_ALREADY_EXISTS, new String[]{code});
        }
        checkCommonParam(req);
        checkBkArtifactoryBaseUrlIfNeed(req);
    }

    private Integer checkUpdateParamAndGetId(EsbCreateOrUpdateFileSourceV3Req req) {
        Long appId = req.getAppId();
        String code = req.getCode();
        Integer id = fileSourceService.getFileSourceIdByCode(appId, code);
        if (id == null) {
            throw new FailedPreconditionException(ErrorCode.FAIL_TO_FIND_FILE_SOURCE_BY_CODE, new String[]{code});
        }
        if (!fileSourceService.existsFileSource(appId, id)) {
            throw new FailedPreconditionException(ErrorCode.FILE_SOURCE_ID_NOT_IN_BIZ, new String[]{id.toString()});
        }
        FileSourceTypeDTO fileSourceTypeDTO = fileSourceService.getFileSourceTypeByCode(req.getType());
        if (fileSourceTypeDTO == null) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME,
                new String[]{"type"});
        }
        checkBkArtifactoryBaseUrlIfNeed(req);
        return id;
    }

    private void checkBkArtifactoryBaseUrlIfNeed(EsbCreateOrUpdateFileSourceV3Req req) {
        if (req.isBlueKingArtifactoryType()) {
            // 制品库类型的文件源需要校验根地址
            fileSourceValidateService.checkBkArtifactoryBaseUrl(req.getBkArtifactoryBaseUrl());
        }
    }

    private FileSourceDTO buildFileSourceDTO(String username,
                                             Long appId,
                                             Integer id,
                                             EsbCreateOrUpdateFileSourceV3Req fileSourceCreateUpdateReq) {
        FileSourceDTO fileSourceDTO = new FileSourceDTO();
        fileSourceDTO.setAppId(appId);
        fileSourceDTO.setId(id);
        fileSourceDTO.setCode(fileSourceCreateUpdateReq.getCode());
        fileSourceDTO.setAlias(fileSourceCreateUpdateReq.getAlias());
        fileSourceDTO.setStatus(null);
        fileSourceDTO.setFileSourceType(
            fileSourceService.getFileSourceTypeByCode(
                fileSourceCreateUpdateReq.getType()
            )
        );
        fileSourceDTO.setFileSourceInfoMap(fileSourceCreateUpdateReq.getAccessParams());
        fileSourceDTO.setPublicFlag(false);
        fileSourceDTO.setSharedAppIdList(Collections.emptyList());
        fileSourceDTO.setShareToAllApp(false);
        fileSourceDTO.setCredentialId(fileSourceCreateUpdateReq.getCredentialId());
        fileSourceDTO.setFilePrefix(fileSourceCreateUpdateReq.getFilePrefix());
        fileSourceDTO.setWorkerSelectScope(WorkerSelectScopeEnum.PUBLIC.name());
        fileSourceDTO.setWorkerSelectMode(WorkerSelectModeEnum.AUTO.name());
        fileSourceDTO.setWorkerId(null);
        // 文件源默认开启状态
        fileSourceDTO.setEnable(true);
        fileSourceDTO.setCreator(username);
        fileSourceDTO.setCreateTime(System.currentTimeMillis());
        fileSourceDTO.setLastModifyUser(username);
        fileSourceDTO.setLastModifyTime(System.currentTimeMillis());
        return fileSourceDTO;
    }
}

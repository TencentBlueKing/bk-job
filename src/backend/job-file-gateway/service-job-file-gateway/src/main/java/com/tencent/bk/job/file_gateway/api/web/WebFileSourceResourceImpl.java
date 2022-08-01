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

package com.tencent.bk.job.file_gateway.api.web;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.PageUtil;
import com.tencent.bk.job.file_gateway.auth.FileSourceAuthService;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.model.req.common.FileSourceStaticParam;
import com.tencent.bk.job.file_gateway.model.req.web.FileSourceCreateUpdateReq;
import com.tencent.bk.job.file_gateway.model.resp.web.FileSourceVO;
import com.tencent.bk.job.file_gateway.service.FileSourceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.tools.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@Slf4j
public class WebFileSourceResourceImpl implements WebFileSourceResource {

    private final FileSourceService fileSourceService;
    private final FileSourceAuthService fileSourceAuthService;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public WebFileSourceResourceImpl(
        FileSourceService fileSourceService,
        FileSourceAuthService fileSourceAuthService,
        AppScopeMappingService appScopeMappingService) {
        this.fileSourceService = fileSourceService;
        this.fileSourceAuthService = fileSourceAuthService;
        this.appScopeMappingService = appScopeMappingService;
    }

    private void checkCodeBlank(String code) {
        if (StringUtils.isBlank(code)) {
            throw new InvalidParamException(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME, new String[]{"code"});
        }
    }

    private void confirmIdExists(Integer id) {
        if (id == null || id <= 0) {
            throw new InvalidParamException(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, new String[]{"id"});
        }
    }

    private void checkParam(Long appId, FileSourceCreateUpdateReq fileSourceCreateUpdateReq, boolean forCreate) {
        Integer id = fileSourceCreateUpdateReq.getId();
        String code = fileSourceCreateUpdateReq.getCode();
        checkCodeBlank(code);
        if (forCreate) {
            // 创建
            if (fileSourceService.existsCode(appId, code)) {
                throw new FailedPreconditionException(
                    ErrorCode.FILE_SOURCE_CODE_ALREADY_EXISTS,
                    new String[]{code}
                );
            }
        } else {
            // 更新
            confirmIdExists(id);
            if (fileSourceService.existsCodeExceptId(appId, code, id)) {
                throw new FailedPreconditionException(
                    ErrorCode.FILE_SOURCE_CODE_ALREADY_EXISTS,
                    new String[]{code}
                );
            }
        }
        if (StringUtils.isBlank(fileSourceCreateUpdateReq.getCredentialId())) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, new String[]{"credentialId"});
        }
    }

    @Override
    public Response<Boolean> checkAlias(String username,
                                        AppResourceScope appResourceScope,
                                        String scopeType,
                                        String scopeId,
                                        String alias,
                                        Integer fileSourceId) {
        return Response.buildSuccessResp(fileSourceService.checkFileSourceAlias(appResourceScope.getAppId(), alias,
            fileSourceId));
    }

    @Override
    public Response<Integer> saveFileSource(String username,
                                            AppResourceScope appResourceScope,
                                            String scopeType,
                                            String scopeId,
                                            FileSourceCreateUpdateReq fileSourceCreateUpdateReq) {
        try {
            Long appId = appResourceScope.getAppId();
            AuthResult authResult = checkCreateFileSourcePermission(username, appResourceScope);
            if (!authResult.isPass()) {
                throw new PermissionDeniedException(authResult);
            }

            checkParam(appId, fileSourceCreateUpdateReq, true);
            FileSourceDTO fileSourceDTO = buildFileSourceDTO(username, appId, fileSourceCreateUpdateReq);
            Integer fileSourceId = fileSourceService.saveFileSource(appId, fileSourceDTO);
            boolean registerResult = fileSourceAuthService.registerFileSource(
                username, fileSourceId, fileSourceDTO.getAlias());
            if (!registerResult) {
                log.warn("Fail to register file_source to iam:({},{})", fileSourceId, fileSourceDTO.getAlias());
            }
            return Response.buildSuccessResp(fileSourceId);
        } catch (ServiceException e) {
            return Response.buildCommonFailResp(e.getErrorCode(), e.getErrorParams());
        }
    }

    @Override
    public Response<Integer> updateFileSource(String username,
                                              AppResourceScope appResourceScope,
                                              String scopeType,
                                              String scopeId,
                                              FileSourceCreateUpdateReq fileSourceCreateUpdateReq) {
        Long appId = appResourceScope.getAppId();
        log.info("Input=({},{},{})", username, appId, fileSourceCreateUpdateReq);
        FileSourceDTO fileSourceDTO = buildFileSourceDTO(username, appId, fileSourceCreateUpdateReq);
        AuthResult authResult = checkManageFileSourcePermission(username, appResourceScope, fileSourceDTO.getId());
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        checkParam(appId, fileSourceCreateUpdateReq, false);
        return Response.buildSuccessResp(fileSourceService.updateFileSourceById(appId, fileSourceDTO));
    }

    @Override
    public Response<Integer> deleteFileSource(String username,
                                              AppResourceScope appResourceScope,
                                              String scopeType,
                                              String scopeId,
                                              Integer id) {
        Long appId = appResourceScope.getAppId();
        AuthResult authResult = checkManageFileSourcePermission(username, appResourceScope, id);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        return Response.buildSuccessResp(fileSourceService.deleteFileSourceById(appId, id));
    }

    @Override
    public Response<Boolean> enableFileSource(String username,
                                              AppResourceScope appResourceScope,
                                              String scopeType,
                                              String scopeId,
                                              Integer id,
                                              Boolean enableFlag) {
        Long appId = appResourceScope.getAppId();
        AuthResult authResult = checkManageFileSourcePermission(username, appResourceScope, id);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        return Response.buildSuccessResp(fileSourceService.enableFileSourceById(username, appId, id,
            enableFlag));
    }

    @Override
    public Response<FileSourceVO> getFileSourceDetail(String username,
                                                      AppResourceScope appResourceScope,
                                                      String scopeType,
                                                      String scopeId,
                                                      Integer id) {

        AuthResult authResult = checkViewFileSourcePermission(username, appResourceScope, id);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        return Response.buildSuccessResp(
            FileSourceDTO.toVO(fileSourceService.getFileSourceById(appResourceScope.getAppId(), id)));
    }

    @Override
    public Response<PageData<FileSourceVO>> listAvailableFileSource(String username,
                                                                    AppResourceScope appResourceScope,
                                                                    String scopeType,
                                                                    String scopeId,
                                                                    String credentialId,
                                                                    String alias,
                                                                    Integer start,
                                                                    Integer pageSize) {
        Long appId = appResourceScope.getAppId();
        log.info("Input=({},{},{},{},{},{})", username, appId, credentialId, alias, start, pageSize);
        Pair<Integer, Integer> pair = PageUtil.normalizePageParam(start, pageSize);
        start = pair.getLeft();
        pageSize = pair.getRight();
        PageData<FileSourceVO> pageData = new PageData<>();
        Integer count = fileSourceService.countAvailableFileSource(appId, credentialId, alias);
        List<FileSourceVO> resultList = fileSourceService.listAvailableFileSource(appId, credentialId, alias, start,
            pageSize).parallelStream().map(FileSourceDTO::toVO).collect(Collectors.toList());
        pageData.setTotal((long) count);
        pageData.setData(resultList);
        pageData.setStart(start);
        pageData.setPageSize(pageSize);
        addAvailableFileSourcePermissionData(username, appResourceScope, pageData);
        return Response.buildSuccessResp(pageData);
    }

    @Override
    public Response<PageData<FileSourceVO>> listWorkTableFileSource(String username,
                                                                    AppResourceScope appResourceScope,
                                                                    String scopeType,
                                                                    String scopeId,
                                                                    String credentialId,
                                                                    String alias,
                                                                    Integer start,
                                                                    Integer pageSize) {
        Long appId = appResourceScope.getAppId();
        log.info("Input=({},{},{},{},{},{})", username, appId, credentialId, alias, start, pageSize);
        Pair<Integer, Integer> pair = PageUtil.normalizePageParam(start, pageSize);
        start = pair.getLeft();
        pageSize = pair.getRight();
        PageData<FileSourceVO> pageData = new PageData<>();
        Integer count = fileSourceService.countWorkTableFileSource(appId, credentialId, alias);
        List<FileSourceVO> resultList = fileSourceService.listWorkTableFileSource(appId, credentialId, alias, start,
            pageSize).parallelStream().map(FileSourceDTO::toVO).collect(Collectors.toList());
        pageData.setTotal((long) count);
        pageData.setData(resultList);
        pageData.setStart(start);
        pageData.setPageSize(pageSize);
        addPermissionData(username, appResourceScope, pageData);
        return Response.buildSuccessResp(pageData);
    }

    @Override
    public Response<List<FileSourceStaticParam>> getFileSourceParams(String username,
                                                                     AppResourceScope appResourceScope,
                                                                     String scopeType,
                                                                     String scopeId,
                                                                     String fileSourceTypeCode) {
        Long appId = appResourceScope.getAppId();
        log.info("Input=({},{},{})", username, appId, fileSourceTypeCode);
        return Response.buildSuccessResp(fileSourceService.getFileSourceParams(appId, fileSourceTypeCode));
    }

    private FileSourceDTO buildFileSourceDTO(String username, Long appId,
                                             FileSourceCreateUpdateReq fileSourceCreateUpdateReq) {
        FileSourceDTO fileSourceDTO = new FileSourceDTO();
        fileSourceDTO.setAppId(appId);
        fileSourceDTO.setId(fileSourceCreateUpdateReq.getId());
        fileSourceDTO.setCode(fileSourceCreateUpdateReq.getCode());
        fileSourceDTO.setAlias(fileSourceCreateUpdateReq.getAlias());
        fileSourceDTO.setStatus(null);
        fileSourceDTO.setFileSourceType(
            fileSourceService.getFileSourceTypeByCode(
                fileSourceCreateUpdateReq.getFileSourceTypeCode()
            )
        );
        fileSourceDTO.setFileSourceInfoMap(fileSourceCreateUpdateReq.getFileSourceInfoMap());
        fileSourceDTO.setPublicFlag(fileSourceCreateUpdateReq.getPublicFlag());
        List<ResourceScope> sharedScopeList = fileSourceCreateUpdateReq.getSharedScopeList();
        Map<ResourceScope, Long> map = appScopeMappingService.getAppIdByScopeList(sharedScopeList);
        List<Long> sharedAppIdList = new ArrayList<>();
        for (ResourceScope resourceScope : sharedScopeList) {
            Long sharedAppId = map.get(resourceScope);
            if (sharedAppId == null) {
                throw new InvalidParamException(
                    ErrorCode.SCOPE_NOT_EXIST,
                    new String[]{resourceScope.getType().getValue() + "," + resourceScope.getId()}
                );
            }
            sharedAppIdList.add(sharedAppId);
        }
        fileSourceDTO.setSharedAppIdList(sharedAppIdList);
        fileSourceDTO.setShareToAllApp(fileSourceCreateUpdateReq.getShareToAllApp());
        fileSourceDTO.setCredentialId(fileSourceCreateUpdateReq.getCredentialId());
        fileSourceDTO.setFilePrefix(fileSourceCreateUpdateReq.getFilePrefix());
        fileSourceDTO.setWorkerSelectScope(fileSourceCreateUpdateReq.getWorkerSelectScope());
        fileSourceDTO.setWorkerSelectMode(fileSourceCreateUpdateReq.getWorkerSelectMode());
        fileSourceDTO.setWorkerId(fileSourceCreateUpdateReq.getWorkerId());
        // 文件源默认开启状态
        fileSourceDTO.setEnable(true);
        fileSourceDTO.setCreator(username);
        fileSourceDTO.setCreateTime(System.currentTimeMillis());
        fileSourceDTO.setLastModifyUser(username);
        fileSourceDTO.setLastModifyTime(System.currentTimeMillis());
        return fileSourceDTO;
    }

    private void addAvailableFileSourcePermissionData(String username, AppResourceScope appResourceScope,
                                                      PageData<FileSourceVO> fileSourceVOPageData) {
        List<FileSourceVO> fileSourceVOList = fileSourceVOPageData.getData();
        List<Integer> currentAppFileSourceIdList = new ArrayList<>();
        for (FileSourceVO fileSourceVO : fileSourceVOList) {
            if (appResourceScope.getType().getValue().equals(fileSourceVO.getScopeType())
                && appResourceScope.getId().equals(fileSourceVO.getScopeId())) {
                currentAppFileSourceIdList.add(fileSourceVO.getId());
            }
        }
        // 添加权限数据
        List<Integer> currentAppCanManageIdList =
            fileSourceAuthService.batchAuthManageFileSource(
                username, appResourceScope, currentAppFileSourceIdList);
        List<Integer> currentAppCanViewIdList =
            fileSourceAuthService.batchAuthViewFileSource(
                username, appResourceScope, currentAppFileSourceIdList);
        fileSourceVOList.forEach(it -> {
            if (currentAppFileSourceIdList.contains(it.getId())) {
                // 当前业务下的文件源走批量鉴权
                it.setCanManage(currentAppCanManageIdList.contains(it.getId()));
                it.setCanView(currentAppCanViewIdList.contains(it.getId()));
            } else {
                // 共享的文件源逐个鉴权
                it.setCanManage(checkManageFileSourcePermission(username,
                    new AppResourceScope(it.getScopeType(), it.getScopeId(), null), it.getId()).isPass());
                it.setCanView(checkViewFileSourcePermission(username, new AppResourceScope(it.getScopeType(),
                    it.getScopeId(), null), it.getId()).isPass());
            }
        });
        fileSourceVOPageData.setCanCreate(checkCreateFileSourcePermission(username, appResourceScope).isPass());
    }

    private void addPermissionData(String username, AppResourceScope appResourceScope,
                                   PageData<FileSourceVO> fileSourceVOPageData) {
        List<FileSourceVO> fileSourceVOList = fileSourceVOPageData.getData();
        List<Integer> currentAppFileSourceIdList = fileSourceVOList.parallelStream()
            .map(FileSourceVO::getId)
            .collect(Collectors.toList());
        // 添加权限数据
        List<Integer> canManageIdList =
            fileSourceAuthService.batchAuthManageFileSource(
                username, appResourceScope, currentAppFileSourceIdList);
        List<Integer> canViewIdList =
            fileSourceAuthService.batchAuthViewFileSource(
                username, appResourceScope, currentAppFileSourceIdList);
        fileSourceVOList.forEach(it -> {
            it.setCanManage(canManageIdList.contains(it.getId()));
            it.setCanView(canViewIdList.contains(it.getId()));
        });
        fileSourceVOPageData.setCanCreate(checkCreateFileSourcePermission(username, appResourceScope).isPass());
    }

    public AuthResult checkViewFileSourcePermission(String username, AppResourceScope appResourceScope,
                                                    Integer fileSourceId) {
        // 需要拥有在业务下查看某个具体文件源的权限
        return fileSourceAuthService.authViewFileSource(username, appResourceScope, fileSourceId, null);
    }

    public AuthResult checkCreateFileSourcePermission(String username, AppResourceScope appResourceScope) {
        // 需要拥有在业务下创建文件源的权限
        return fileSourceAuthService.authCreateFileSource(username, appResourceScope);
    }

    public AuthResult checkManageFileSourcePermission(String username, AppResourceScope appResourceScope,
                                                      Integer fileSourceId) {
        // 需要拥有在业务下管理某个具体文件源的权限
        return fileSourceAuthService.authManageFileSource(username, appResourceScope, fileSourceId, null);
    }
}

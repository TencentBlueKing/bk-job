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
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import com.tencent.bk.job.common.util.PageUtil;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.model.req.common.FileSourceStaticParam;
import com.tencent.bk.job.file_gateway.model.req.web.FileSourceCreateUpdateReq;
import com.tencent.bk.job.file_gateway.model.resp.web.FileSourceVO;
import com.tencent.bk.job.file_gateway.service.FileSourceService;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.tools.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@RestController
@Slf4j
public class WebFileSourceResourceImpl implements WebFileSourceResource {

    private final WebAuthService authService;
    private final FileSourceService fileSourceService;

    @Autowired
    public WebFileSourceResourceImpl(WebAuthService authService, FileSourceService fileSourceService) {
        this.authService = authService;
        this.fileSourceService = fileSourceService;
    }

    private void checkParam(FileSourceCreateUpdateReq fileSourceCreateUpdateReq) {
        if (StringUtils.isBlank(fileSourceCreateUpdateReq.getCredentialId())) {
            throw new ServiceException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, new String[]{"credentialId"});
        }
    }

    @Override
    public ServiceResponse<Boolean> checkAlias(String username, Long appId, String alias, Integer fileSourceId) {
        return ServiceResponse.buildSuccessResp(fileSourceService.checkFileSourceAlias(appId, alias, fileSourceId));
    }

    @Override
    public ServiceResponse<Integer> saveFileSource(String username, Long appId,
                                                   FileSourceCreateUpdateReq fileSourceCreateUpdateReq) {
        try {
            AuthResultVO authResultVO = checkCreateFileSourcePermission(username, appId);
            if (!authResultVO.isPass()) {
                return ServiceResponse.buildAuthFailResp(authResultVO);
            }
            checkParam(fileSourceCreateUpdateReq);
            FileSourceDTO fileSourceDTO = buildFileSourceDTO(username, appId, fileSourceCreateUpdateReq);
            Integer fileSourceId = fileSourceService.saveFileSource(appId, fileSourceDTO);
            boolean registerResult = authService.registerResource("" + fileSourceId, fileSourceDTO.getAlias(),
                ResourceId.FILE_SOURCE, username, null);
            if (!registerResult) {
                log.warn("Fail to register file_source to iam:({},{})", fileSourceId, fileSourceDTO.getAlias());
            }
            return ServiceResponse.buildSuccessResp(fileSourceId);
        } catch (ServiceException e) {
            return ServiceResponse.buildCommonFailResp(e.getErrorCode(), e.getErrorParams());
        }
    }

    @Override
    public ServiceResponse<Integer> updateFileSource(String username, Long appId,
                                                     FileSourceCreateUpdateReq fileSourceCreateUpdateReq) {
        log.info("Input=({},{},{})", username, appId, fileSourceCreateUpdateReq);
        FileSourceDTO fileSourceDTO = buildFileSourceDTO(username, appId, fileSourceCreateUpdateReq);
        AuthResultVO authResultVO = checkManageFileSourcePermission(username, appId, fileSourceDTO.getId());
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        return ServiceResponse.buildSuccessResp(fileSourceService.updateFileSourceById(appId, fileSourceDTO));
    }

    @Override
    public ServiceResponse<Integer> deleteFileSource(String username, Long appId, Integer id) {
        AuthResultVO authResultVO = checkManageFileSourcePermission(username, appId, id);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        return ServiceResponse.buildSuccessResp(fileSourceService.deleteFileSourceById(appId, id));
    }

    @Override
    public ServiceResponse<Boolean> enableFileSource(String username, Long appId, Integer id, Boolean enableFlag) {
        AuthResultVO authResultVO = checkManageFileSourcePermission(username, appId, id);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        return ServiceResponse.buildSuccessResp(fileSourceService.enableFileSourceById(username, appId, id,
            enableFlag));
    }

    @Override
    public ServiceResponse<FileSourceVO> getFileSourceDetail(String username, Long appId, Integer id) {
        AuthResultVO authResultVO = checkViewFileSourcePermission(username, appId, id);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        return ServiceResponse.buildSuccessResp(FileSourceDTO.toVO(fileSourceService.getFileSourceById(appId, id)));
    }

    @Override
    public ServiceResponse<PageData<FileSourceVO>> listAvailableFileSource(String username, Long appId,
                                                                           String credentialId, String alias,
                                                                           Integer start, Integer pageSize) {
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
        addAvailableFileSourcePermissionData(username, appId, pageData);
        return ServiceResponse.buildSuccessResp(pageData);
    }

    @Override
    public ServiceResponse<PageData<FileSourceVO>> listWorkTableFileSource(String username, Long appId,
                                                                           String credentialId, String alias,
                                                                           Integer start, Integer pageSize) {
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
        addPermissionData(username, appId, pageData);
        return ServiceResponse.buildSuccessResp(pageData);
    }

    @Override
    public ServiceResponse<List<FileSourceStaticParam>> getFileSourceParams(String username, Long appId,
                                                                            String fileSourceTypeCode) {
        log.info("Input=({},{},{})", username, appId, fileSourceTypeCode);
        return ServiceResponse.buildSuccessResp(fileSourceService.getFileSourceParams(appId, fileSourceTypeCode));
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
        fileSourceDTO.setSharedAppIdList(fileSourceCreateUpdateReq.getSharedAppIdList());
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

    private void addAvailableFileSourcePermissionData(String username, Long appId,
                                                      PageData<FileSourceVO> fileSourceVOPageData) {
        List<FileSourceVO> fileSourceVOList = fileSourceVOPageData.getData();
        List<String> currentAppFileSourceIdList = new ArrayList<>();
        for (FileSourceVO fileSourceVO : fileSourceVOList) {
            if (appId.equals(fileSourceVO.getAppId())) {
                currentAppFileSourceIdList.add(fileSourceVO.getId().toString());
            }
        }
        // 添加权限数据
        List<String> currentAppCanManageIdList =
            authService.batchAuth(username, ActionId.MANAGE_FILE_SOURCE, appId, ResourceTypeEnum.FILE_SOURCE,
                currentAppFileSourceIdList);
        List<String> currentAppCanViewIdList =
            authService.batchAuth(username, ActionId.VIEW_FILE_SOURCE, appId, ResourceTypeEnum.FILE_SOURCE,
                currentAppFileSourceIdList);
        fileSourceVOList.forEach(it -> {
            if (currentAppFileSourceIdList.contains(it.getId().toString())) {
                // 当前业务下的文件源走批量鉴权
                it.setCanManage(currentAppCanManageIdList.contains(it.getId().toString()));
                it.setCanView(currentAppCanViewIdList.contains(it.getId().toString()));
            } else {
                // 共享的文件源逐个鉴权
                it.setCanManage(checkManageFileSourcePermission(username, it.getAppId(), it.getId()).isPass());
                it.setCanView(checkViewFileSourcePermission(username, it.getAppId(), it.getId()).isPass());
            }
        });
        fileSourceVOPageData.setCanCreate(checkCreateFileSourcePermission(username, appId).isPass());
    }

    private void addPermissionData(String username, Long appId, PageData<FileSourceVO> fileSourceVOPageData) {
        List<FileSourceVO> fileSourceVOList = fileSourceVOPageData.getData();
        // 添加权限数据
        List<String> canManageIdList =
            authService.batchAuth(username, ActionId.MANAGE_FILE_SOURCE, appId, ResourceTypeEnum.FILE_SOURCE,
                fileSourceVOList.parallelStream()
                    .map(FileSourceVO::getId)
                    .map(Objects::toString)
                    .collect(Collectors.toList())
            );
        List<String> canViewIdList =
            authService.batchAuth(username, ActionId.VIEW_FILE_SOURCE, appId, ResourceTypeEnum.FILE_SOURCE,
                fileSourceVOList.parallelStream()
                    .map(FileSourceVO::getId)
                    .map(Objects::toString)
                    .collect(Collectors.toList())
            );
        fileSourceVOList.forEach(it -> {
            it.setCanManage(canManageIdList.contains(it.getId().toString()));
            it.setCanView(canViewIdList.contains(it.getId().toString()));
        });
        fileSourceVOPageData.setCanCreate(checkCreateFileSourcePermission(username, appId).isPass());
    }

    public AuthResultVO checkViewFileSourcePermission(String username, Long appId, Integer fileSourceId) {
        // 需要拥有在业务下查看某个具体文件源的权限
        return authService.auth(true, username, ActionId.VIEW_FILE_SOURCE, ResourceTypeEnum.FILE_SOURCE,
            fileSourceId.toString(),
            PathBuilder.newBuilder(ResourceTypeEnum.BUSINESS.getId(), appId.toString()).build());
    }

    public AuthResultVO checkCreateFileSourcePermission(String username, Long appId) {
        // 需要拥有在业务下创建文件源的权限
        return authService.auth(true, username, ActionId.CREATE_FILE_SOURCE, ResourceTypeEnum.BUSINESS,
            appId.toString(), null);
    }

    public AuthResultVO checkManageFileSourcePermission(String username, Long appId, Integer fileSourceId) {
        // 需要拥有在业务下管理某个具体文件源的权限
        return authService.auth(true, username, ActionId.MANAGE_FILE_SOURCE, ResourceTypeEnum.FILE_SOURCE,
            fileSourceId.toString(),
            PathBuilder.newBuilder(ResourceTypeEnum.BUSINESS.getId(), appId.toString()).build());
    }
}

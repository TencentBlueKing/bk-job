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

package com.tencent.bk.job.manage.api.web.impl;

import com.google.common.collect.Lists;
import com.tencent.bk.job.common.artifactory.model.dto.NodeDTO;
import com.tencent.bk.job.common.artifactory.model.dto.TempUrlInfo;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.util.Utils;
import com.tencent.bk.job.common.util.file.PathUtil;
import com.tencent.bk.job.manage.api.web.WebFileUploadResource;
import com.tencent.bk.job.manage.common.consts.globalsetting.RestrictModeEnum;
import com.tencent.bk.job.manage.config.ArtifactoryConfig;
import com.tencent.bk.job.manage.config.LocalFileConfigForManage;
import com.tencent.bk.job.manage.config.StorageSystemConfig;
import com.tencent.bk.job.manage.model.web.request.GenUploadTargetReq;
import com.tencent.bk.job.manage.model.web.vo.UploadLocalFileResultVO;
import com.tencent.bk.job.manage.model.web.vo.UploadTargetVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.FileUploadSettingVO;
import com.tencent.bk.job.manage.service.GlobalSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class WebFileUploadResourceImpl implements WebFileUploadResource {
    private final StorageSystemConfig storageSystemConfig;
    private final ArtifactoryConfig artifactoryConfig;
    private final LocalFileConfigForManage localFileConfigForManage;
    private final ArtifactoryClient artifactoryClient;
    private final GlobalSettingsService globalSettingsService;

    @Autowired
    public WebFileUploadResourceImpl(
        StorageSystemConfig storageSystemConfig,
        ArtifactoryConfig artifactoryConfig, LocalFileConfigForManage localFileConfigForManage,
        ArtifactoryClient artifactoryClient,
        GlobalSettingsService globalSettingsService) {
        this.storageSystemConfig = storageSystemConfig;
        this.artifactoryConfig = artifactoryConfig;
        this.localFileConfigForManage = localFileConfigForManage;
        this.artifactoryClient = artifactoryClient;
        this.globalSettingsService = globalSettingsService;
    }

    /**
     * 将上传的文件保存至本机挂载的NFS
     *
     * @param username
     * @param uploadFiles
     * @return
     */
    private List<UploadLocalFileResultVO> saveFileToLocal(String username, MultipartFile[] uploadFiles) {
        String uploadPath = storageSystemConfig.getJobStorageRootPath() + "/localupload/";
        List<UploadLocalFileResultVO> fileUploadResults = Lists.newArrayListWithCapacity(uploadFiles.length);

        for (MultipartFile file : uploadFiles) {
            UploadLocalFileResultVO result = new UploadLocalFileResultVO();
            try {
                String fileName = Utils.getUUID() +
                    File.separatorChar + username + File.separatorChar +
                    file.getOriginalFilename();

                String fullFileName = uploadPath.concat(fileName);
                File theFile = new File(fullFileName);

                //创建上传文件父目录，并设置父目录可写权限
                File parentDir = theFile.getParentFile();
                if (!parentDir.exists()) {
                    boolean isCreate = parentDir.mkdirs();
                    if (!isCreate) {
                        log.error("Fail to create parent dir:{}", parentDir.getCanonicalFile());
                        result.setFileName(file.getOriginalFilename());
                        result.setStatus(-1);
                        fileUploadResults.add(result);
                        continue;
                    }
                    if (!parentDir.setWritable(true, false)) {
                        log.error("Fail to set writable:{}", parentDir.getCanonicalFile());
                        result.setFileName(file.getOriginalFilename());
                        result.setStatus(-1);
                        fileUploadResults.add(result);
                        continue;
                    }
                }

                if (theFile.exists() && theFile.isFile()) {
                    if (!theFile.delete()) {
                        log.error("Fail to delete exist file:{}", theFile.getAbsolutePath());
                        result.setFileName(file.getOriginalFilename());
                        result.setStatus(-1);
                        fileUploadResults.add(result);
                        continue;
                    }
                }
                file.transferTo(theFile);

                result.setFileName(file.getOriginalFilename());
                result.setFilePath(fileName);
                result.setFileSize(file.getSize());
                result.setStatus(theFile.exists() ? 0 : 1);

                String md5 = getFileMd5(file);
                result.setMd5(md5);
                fileUploadResults.add(result);
            } catch (Exception e) {
                log.error("Upload file fail", e);
                result.setFileName(file.getOriginalFilename());
                result.setStatus(-1);
                fileUploadResults.add(result);
            }
        }
        return fileUploadResults;
    }

    /**
     * 将上传的文件保存至蓝鲸制品库
     *
     * @param username
     * @param uploadFiles
     * @return
     */
    private List<UploadLocalFileResultVO> saveFileToArtifactory(String username, MultipartFile[] uploadFiles) {
        List<UploadLocalFileResultVO> fileUploadResults = Lists.newArrayListWithCapacity(uploadFiles.length);
        for (MultipartFile file : uploadFiles) {
            String filePath = Utils.getUUID() +
                File.separatorChar + username + File.separatorChar +
                file.getOriginalFilename();
            String fileName = PathUtil.getFileNameByPath(filePath);
            log.debug("filePath={}", filePath);
            UploadLocalFileResultVO fileResultVO = new UploadLocalFileResultVO();
            fileResultVO.setFileName(fileName);
            fileResultVO.setFilePath(filePath);
            String project = artifactoryConfig.getArtifactoryJobProject();
            String repo = localFileConfigForManage.getLocalUploadRepo();
            try {
                NodeDTO nodeDTO = artifactoryClient.uploadGenericFileWithStream(
                    project,
                    repo,
                    filePath,
                    file.getInputStream(),
                    file.getSize()
                );
                fileResultVO.setFileSize(nodeDTO.getSize());
                fileResultVO.setMd5(nodeDTO.getMd5());
                fileResultVO.setStatus(0);
            } catch (IOException e) {
                String errMsg = String.format(
                    "Fail to upload file %s to artifactory project {} repo {}",
                    filePath,
                    project,
                    repo
                );
                fileResultVO.setStatus(-1);
                log.error(errMsg, e);
                throw new InternalException(errMsg, ErrorCode.ARTIFACTORY_API_DATA_ERROR);
            } finally {
                fileUploadResults.add(fileResultVO);
            }
        }
        return fileUploadResults;
    }

    @Override
    public Response<List<UploadLocalFileResultVO>> uploadLocalFile(String username,
                                                                   MultipartFile[] uploadFiles) {
        log.info("Handle upload file!");
        checkFileSuffixValid(uploadFiles);
        List<UploadLocalFileResultVO> fileUploadResults = null;
        if (JobConstants.FILE_STORAGE_BACKEND_ARTIFACTORY.equals(
            localFileConfigForManage.getStorageBackend()
        )) {
            fileUploadResults = saveFileToArtifactory(username, uploadFiles);

        } else {
            fileUploadResults = saveFileToLocal(username, uploadFiles);
        }
        return Response.buildSuccessResp(fileUploadResults);
    }

    private void checkFileSuffixValid(MultipartFile[] uploadFiles) {
        //检查是否合法后缀
        FileUploadSettingVO fileUploadSettingVO = globalSettingsService.getFileUploadSettings();
        Integer restrictMode = fileUploadSettingVO.getRestrictMode();
        List<String> suffixList = fileUploadSettingVO.getSuffixList();
        Boolean validateSuffix = true;
        //初始状态:允许模式：默认不允许，禁止模式：默认不禁止
        if (CollectionUtils.isNotEmpty(suffixList)) {
            if (restrictMode == RestrictModeEnum.ALLOW.getType()) {
                validateSuffix = false;
            } else if (restrictMode == RestrictModeEnum.FORBID.getType()) {
                validateSuffix = true;
            }
        }
        for (MultipartFile multipartFile : uploadFiles) {
            if (StringUtils.isBlank(multipartFile.getOriginalFilename())) {
                log.warn("upload file ,fileName are empty");
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
            }
            if (CollectionUtils.isNotEmpty(suffixList)) {
                for (String suffix : suffixList) {
                    if (multipartFile.getOriginalFilename().toLowerCase().endsWith(suffix.toLowerCase())) {
                        if (restrictMode == RestrictModeEnum.ALLOW.getType()) {
                            validateSuffix = true;
                        } else if (restrictMode == RestrictModeEnum.FORBID.getType()) {
                            validateSuffix = false;
                        }
                    }
                }
            }
        }
        if (restrictMode != -1 && !validateSuffix) {
            log.warn("upload file, file suffix not allow");
            throw new InvalidParamException(ErrorCode.UPLOAD_FILE_SUFFIX_NOT_ALLOW);
        }
    }

    @Override
    public Response<UploadTargetVO> genUploadTarget(String username, GenUploadTargetReq req) {
        List<String> fileNameList = req.getFileNameList();
        List<String> filePathList = new ArrayList<>();
        fileNameList.forEach(fileName -> {
            String filePath = Utils.getUUID() +
                File.separatorChar + username + File.separatorChar + fileName;
            filePathList.add(filePath);
        });
        List<TempUrlInfo> urlInfoList = artifactoryClient.createTempUrls(
            artifactoryConfig.getArtifactoryJobProject(),
            localFileConfigForManage.getLocalUploadRepo(),
            filePathList
        );
        return Response.buildSuccessResp(
            new UploadTargetVO(
                urlInfoList
                    .parallelStream()
                    .map(TempUrlInfo::getUrl)
                    .collect(Collectors.toList())
            )
        );
    }

    private String getFileMd5(MultipartFile file) {
        try {
            return DigestUtils.md5Hex(file.getInputStream());
        } catch (Exception e) {
            log.error("Md5 Digest file exception", e);
            return "";
        }
    }
}

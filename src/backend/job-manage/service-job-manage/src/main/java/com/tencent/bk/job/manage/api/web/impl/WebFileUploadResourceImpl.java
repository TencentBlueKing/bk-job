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
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.util.Utils;
import com.tencent.bk.job.manage.api.web.WebFileUploadResource;
import com.tencent.bk.job.manage.config.StorageSystemConfig;
import com.tencent.bk.job.manage.model.web.vo.UploadLocalFileResultVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@RestController
@Slf4j
public class WebFileUploadResourceImpl implements WebFileUploadResource {
    private final StorageSystemConfig storageSystemConfig;

    public WebFileUploadResourceImpl(StorageSystemConfig storageSystemConfig) {
        this.storageSystemConfig = storageSystemConfig;
    }

    @Override
    public ServiceResponse<List<UploadLocalFileResultVO>> uploadLocalFile(String username,
                                                                          MultipartFile[] uploadFiles) {
        log.info("Handle upload file!");
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
        return ServiceResponse.buildSuccessResp(fileUploadResults);
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

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

package com.tencent.bk.job.backup.service.impl;

import com.tencent.bk.job.backup.config.NfsStorageSystemConfig;
import com.tencent.bk.job.backup.service.StorageService;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * @since 29/7/2020 12:43
 */
@Slf4j
@Service
public class NfsStorageServiceImpl implements StorageService {

    private final NfsStorageSystemConfig nfsStorageSystemConfig;
    private final String baseStoragePath;
    private final String storagePath;
    private final String localUploadStoragePath;

    @Autowired
    public NfsStorageServiceImpl(NfsStorageSystemConfig nfsStorageSystemConfig) {
        this.nfsStorageSystemConfig = nfsStorageSystemConfig;
        this.baseStoragePath = this.nfsStorageSystemConfig.getJobStorageRootPath();
        this.storagePath = this.baseStoragePath + "/backup/";
        this.localUploadStoragePath = this.baseStoragePath + "/localupload/";
    }

    @Override
    public String getStoragePath() {
        return this.storagePath;
    }

    @Override
    public String store(String id, String prefix, MultipartFile file) {
        try {
            if (StringUtils.isNotBlank(prefix)) {
                prefix = File.separatorChar + prefix.trim() + File.separatorChar;
            } else {
                prefix = File.separator;
            }
            String fileName = prefix + id + File.separatorChar + file.getOriginalFilename();

            String fullFileName = storagePath.concat(fileName);
            File theFile = new File(fullFileName);

            // 创建上传文件父目录，并设置父目录可写权限
            File parentDir = theFile.getParentFile();
            if (!parentDir.exists()) {
                boolean isCreate = parentDir.mkdirs();
                if (!isCreate) {
                    log.error("Fail to create parent dir:{}", parentDir.getCanonicalFile());
                    throw new InternalException(ErrorCode.INTERNAL_ERROR, "Fail to create parent dir");
                }
                if (!parentDir.setWritable(true, false)) {
                    log.error("Fail to set writable:{}", parentDir.getCanonicalFile());
                    throw new InternalException(ErrorCode.INTERNAL_ERROR, "Fail to set writable");
                }
            }

            file.transferTo(theFile);
            return fileName;
        } catch (Exception e) {
            log.error("Upload file fail", e);
            return null;
        }
    }

    @Override
    public File getFile(String fileName) {
        String fullFileName = storagePath.concat(fileName);
        return new File(fullFileName);
    }

    @Override
    public String getLocalUploadPath() {
        return localUploadStoragePath;
    }

    @Override
    public File getLocalUploadFile(String fileName) {
        String fullFileName = localUploadStoragePath.concat(fileName);
        File file = new File((fullFileName));
        if (file.exists()) {
            return file;
        }
        return null;
    }
}

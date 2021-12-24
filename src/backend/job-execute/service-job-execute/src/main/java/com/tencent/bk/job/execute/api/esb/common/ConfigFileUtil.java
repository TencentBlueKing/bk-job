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

package com.tencent.bk.job.execute.api.esb.common;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.util.FileUtil;
import com.tencent.bk.job.common.util.JobUUID;
import com.tencent.bk.job.execute.engine.consts.FileDirTypeConf;
import com.tencent.bk.job.execute.engine.util.NFSUtils;

import java.io.File;

public class ConfigFileUtil {

    /**
     * 将base64编码的配置文件保存至本地
     *
     * @param jobStorageRootPath 本地存储根路径
     * @param userName           操作者
     * @param fileName           文件名称
     * @param base64Content      base64编码的文件内容
     * @return 保存路径
     */
    public static String saveConfigFileToLocal(String jobStorageRootPath,
                                               String userName,
                                               String fileName,
                                               String base64Content) {
        String uploadPath = NFSUtils.getFileDir(jobStorageRootPath, FileDirTypeConf.UPLOAD_FILE_DIR);
        String configFileRelativePath = JobUUID.getUUID() + File.separatorChar +
            userName + File.separatorChar + fileName;
        String fullFilePath = uploadPath.concat(configFileRelativePath);
        if (!FileUtil.saveBase64StrToFile(fullFilePath, base64Content)) {
            throw new InternalException(ErrorCode.FAIL_TO_SAVE_FILE_TO_LOCAL);
        }
        return configFileRelativePath;
    }
}

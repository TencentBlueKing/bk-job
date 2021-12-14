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

package com.tencent.bk.job.execute.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileDetailDTO implements Cloneable {
    /**
     * 文件路径
     */
    private String filePath;
    /**
     * 解析过的路径
     */
    private String resolvedFilePath;
    /**
     * 第三方文件源文件的原始路径
     */
    private String thirdFilePath;
    /**
     * 含文件源名称的第三方文件源文件的原始路径
     */
    private String thirdFilePathWithFileSourceName;
    /**
     * 文件名
     */
    private String fileName;
    /**
     * base64编码的文件内容
     */
    private String base64Content;
    /**
     * 文件hash，目前用md5
     */
    private String fileHash;

    /**
     * 文件大小，byte
     */
    private Long fileSize;

    public FileDetailDTO(String filePath) {
        this.filePath = filePath;
    }

    public FileDetailDTO(String fileName, String base64Content) {
        this.fileName = fileName;
        this.base64Content = base64Content;
    }

    public FileDetailDTO(boolean localUpload, String filePath, String fileHash, Long fileSize) {
        this.filePath = filePath;
        if (localUpload) {
            this.fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        }
        this.fileHash = fileHash;
        this.fileSize = fileSize;
    }

    public FileDetailDTO(String filePath, String fileName, String fileHash, Long fileSize) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.fileSize = fileSize;
    }

    public FileDetailDTO clone() {
        FileDetailDTO fileDetailDTO = new FileDetailDTO(filePath, fileName, fileHash, fileSize);
        fileDetailDTO.setResolvedFilePath(resolvedFilePath);
        return fileDetailDTO;
    }
}

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

package com.tencent.bk.job.backup.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * @since 29/7/2020 12:39
 */
public interface StorageService {
    /**
     * 获取存储路径
     *
     * @return 存储路径
     */
    String getStoragePath();

    /**
     * 保存文件
     *
     * @param id     文件 ID
     * @param suffix 文件前缀
     * @param file   待保存文件
     * @return 文件路径
     */
    String store(String id, String suffix, MultipartFile file);

    /**
     * 按文件名获取文件对象
     *
     * @param fileName 文件名
     * @return 文件对象
     */
    File getFile(String fileName);

    /**
     * 获取本地上传文件路径
     *
     * @return 本地上传文件路径
     */
    String getLocalUploadPath();

    /**
     * 按文件名获取本地上传文件对象
     *
     * @param fileName 文件名
     * @return 文件对象
     */
    File getLocalUploadFile(String fileName);
}

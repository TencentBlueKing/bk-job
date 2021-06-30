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

import com.tencent.bk.job.backup.model.dto.ImportJobInfoDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @since 28/7/2020 19:08
 */
public interface ImportJobService {
    /**
     * 新增导入任务
     *
     * @param username 用户名
     * @param appId    业务 ID
     * @param id       导入任务 ID
     * @param fileName 文件名
     * @return 任务 ID
     */
    String addImportJob(String username, Long appId, String id, String fileName);

    /**
     * 按 ID 查询导入任务信息
     *
     * @param appId 业务 ID
     * @param jobId 任务 ID
     * @return 导入任务信息
     */
    ImportJobInfoDTO getImportInfoById(Long appId, String jobId);

    /**
     * 按用户拉取正在导入的作业列表
     *
     * @param username 用户名
     * @param appId    业务 ID
     * @return 正在导入的任务信息列表
     */
    List<ImportJobInfoDTO> getCurrentJobByUser(String username, Long appId);

    /**
     * 开始导入
     *
     * @param importJobInfo 导入任务信息
     * @return 是否开始成功
     */
    Boolean startImport(ImportJobInfoDTO importJobInfo);

    /**
     * 解析导入文件
     *
     * @param username 用户名
     * @param appId    业务 ID
     * @param id       导入任务 ID
     * @return 解析是否成功
     */
    Boolean parseFile(String username, Long appId, String id);

    /**
     * 上传导入文件
     *
     * @param username 用户名
     * @param appId    业务 ID
     * @param id       导入任务 ID
     * @param file     待导入文件
     * @return 文件名
     */
    String saveFile(String username, Long appId, String id, MultipartFile file);

    /**
     * 校验密码
     *
     * @param username 用户名
     * @param appId    业务 ID
     * @param jobId    导入任务 ID
     * @param password 密码
     * @return 密码是否正确
     */
    Boolean checkPassword(String username, Long appId, String jobId, String password);

    /**
     * 更新导入任务信息
     *
     * @param importJob 导入任务信息
     * @return 更新是否成功
     */
    Boolean updateImportJob(ImportJobInfoDTO importJob);

    /**
     * 标记任务为失败
     *
     * @param importJob 导入任务信息
     * @param message   提示信息
     */
    void markJobFailed(ImportJobInfoDTO importJob, String message);

    /**
     * 清理导入文件
     */
    void cleanFile();
}

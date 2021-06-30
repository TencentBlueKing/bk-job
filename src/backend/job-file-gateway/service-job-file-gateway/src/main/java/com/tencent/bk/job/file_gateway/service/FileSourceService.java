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

package com.tencent.bk.job.file_gateway.service;

import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceTypeDTO;
import com.tencent.bk.job.file_gateway.model.req.common.FileSourceStaticParam;

import java.util.List;

/**
 * 文件源服务
 */
public interface FileSourceService {

    List<FileSourceTypeDTO> listUniqueFileSourceType(String storageType);

    Integer countAvailableFileSource(Long appId, String credentialId, String alias);

    Integer countWorkTableFileSource(Long appId, String credentialId, String alias);

    Integer countWorkTableFileSource(List<Long> appIdList, List<Integer> idList);

    List<FileSourceDTO> listAvailableFileSource(Long appId, String credentialId, String alias, Integer start,
                                                Integer pageSize);

    List<FileSourceDTO> listWorkTableFileSource(Long appId, String credentialId, String alias, Integer start,
                                                Integer pageSize);

    List<FileSourceDTO> listWorkTableFileSource(List<Long> appIdList, List<Integer> idList, Integer start,
                                                Integer pageSize);

    Integer saveFileSource(Long appId, FileSourceDTO fileSourceDTO);

    Integer updateFileSourceById(Long appId, FileSourceDTO fileSourceDTO);

    int updateFileSourceStatus(Integer fileSourceId, Integer status);

    FileSourceTypeDTO getFileSourceTypeById(Integer id);

    FileSourceTypeDTO getFileSourceTypeByCode(String code);

    Integer deleteFileSourceById(Long appId, Integer id);

    Boolean enableFileSourceById(String username, Long appId, Integer id, Boolean enableFlag);

    FileSourceDTO getFileSourceById(Long appId, Integer id);

    FileSourceDTO getFileSourceById(Integer id);

    FileSourceDTO getFileSourceByCode(String code);

    List<FileSourceStaticParam> getFileSourceParams(Long appId, String fileSourceTypeCode);

    Boolean checkFileSourceAlias(Long appId, String alias, Integer fileSourceId);
}

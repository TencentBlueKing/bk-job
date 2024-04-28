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

package com.tencent.bk.job.file_gateway.dao.filesource;

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.constant.CompatibleType;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceBasicInfoDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;

import java.util.Collection;
import java.util.List;

public interface FileSourceDAO {
    Integer insertFileSource(FileSourceDTO fileSourceDTO);

    int updateFileSource(FileSourceDTO fileSourceDTO);

    int updateFileSourceStatus(Integer fileSourceId, Integer status);

    int deleteFileSourceById(Integer id);

    int enableFileSourceById(String username, Long appId, Integer id, Boolean enableFlag);

    FileSourceDTO getFileSourceById(Integer id);

    List<FileSourceBasicInfoDTO> listFileSourceByIds(Collection<Integer> ids);

    @Deprecated
    @CompatibleImplementation(name = "fileSourceId", deprecatedVersion = "3.9.x", type = CompatibleType.DEPLOY,
        explain = "文件源标识仅在appId下唯一，发布完成后可删除")
    FileSourceDTO getFileSourceByCode(String code);

    FileSourceDTO getFileSourceByCode(Long appId, String code);

    Integer countAvailableLikeFileSource(Long appId, String credentialId, String alias);

    Integer countFileSource(Long appId, String credentialId, String alias);

    Integer countWorkTableFileSource(Long appId, String credentialId, String alias);

    Integer countWorkTableFileSource(List<Long> appIdList, List<Integer> idList);

    Boolean checkFileSourceExists(Long appId, String alias);

    List<FileSourceDTO> listAvailableFileSource(Long appId, String credentialId, String alias,
                                                Integer start, Integer pageSize);

    List<FileSourceDTO> listWorkTableFileSource(Long appId, String credentialId, String alias,
                                                Integer start, Integer pageSize);

    List<FileSourceDTO> listWorkTableFileSource(List<Long> appIdList, List<Integer> idList,
                                                Integer start, Integer pageSize);

    boolean existsCode(Long appId, String code);

    boolean existsCodeExceptId(Long appId, String code, Integer exceptId);

    boolean existsFileSource(Long appId, Integer id);

    boolean existsFileSourceUsingCredential(Long appId, String credentialId);

    Integer getFileSourceIdByCode(Long appId, String code);
}

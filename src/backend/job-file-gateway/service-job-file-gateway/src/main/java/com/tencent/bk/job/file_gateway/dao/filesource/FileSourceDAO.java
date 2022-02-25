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

import com.tencent.bk.job.file_gateway.model.dto.FileSourceBasicInfoDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import org.jooq.DSLContext;

import java.util.Collection;
import java.util.List;

public interface FileSourceDAO {
    Integer insertFileSource(DSLContext dslContext, FileSourceDTO fileSourceDTO);

    int updateFileSource(DSLContext dslContext, FileSourceDTO fileSourceDTO);

    int updateFileSourceStatus(DSLContext dslContext, Integer fileSourceId, Integer status);

    int deleteFileSourceById(DSLContext dslContext, Integer id);

    int enableFileSourceById(DSLContext dslContext, String username, Long appId, Integer id, Boolean enableFlag);

    FileSourceDTO getFileSourceById(DSLContext dslContext, Integer id);

    List<FileSourceBasicInfoDTO> listFileSourceByIds(DSLContext dslContext, Collection<Integer> ids);

    FileSourceDTO getFileSourceByCode(DSLContext dslContext, String code);

    Integer countAvailableLikeFileSource(DSLContext dslContext, Long appId, String credentialId, String alias);

    Integer countFileSource(DSLContext dslContext, Long appId, String credentialId, String alias);

    Integer countWorkTableFileSource(DSLContext dslContext, Long appId, String credentialId, String alias);

    Integer countWorkTableFileSource(DSLContext dslContext, List<Long> appIdList, List<Integer> idList);

    Boolean checkFileSourceExists(DSLContext dslContext, Long appId, String alias);

    List<FileSourceDTO> listAvailableFileSource(DSLContext dslContext, Long appId, String credentialId, String alias,
                                                Integer start, Integer pageSize);

    List<FileSourceDTO> listWorkTableFileSource(DSLContext dslContext, Long appId, String credentialId, String alias,
                                                Integer start, Integer pageSize);

    List<FileSourceDTO> listWorkTableFileSource(DSLContext dslContext, List<Long> appIdList, List<Integer> idList,
                                                Integer start, Integer pageSize);

    boolean existsCode(String code);

    boolean existsFileSource(Long appId, Integer id);

    Integer getFileSourceIdByCode(Long appId, String code);
}

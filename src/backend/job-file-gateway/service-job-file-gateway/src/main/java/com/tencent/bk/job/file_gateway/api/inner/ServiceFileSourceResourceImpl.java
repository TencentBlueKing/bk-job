/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.file_gateway.api.inner;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceBasicInfoDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.ServiceFileSourceAvailabilityDTO;
import com.tencent.bk.job.file_gateway.service.FileSourceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class ServiceFileSourceResourceImpl implements ServiceFileSourceResource {

    private final FileSourceService fileSourceService;
    private final FileSourceDAO fileSourceDAO;

    @Autowired
    public ServiceFileSourceResourceImpl(FileSourceService fileSourceService,
                                         FileSourceDAO fileSourceDAO) {
        this.fileSourceService = fileSourceService;
        this.fileSourceDAO = fileSourceDAO;
    }

    @Override
    public InternalResponse<Integer> getFileSourceIdByCode(Long appId, String code) {
        FileSourceDTO fileSourceDTO = fileSourceService.getFileSourceByCode(appId, code);
        if (null == fileSourceDTO) {
            throw new NotFoundException(ErrorCode.FAIL_TO_FIND_FILE_SOURCE_BY_CODE, new String[]{code});
        }
        return InternalResponse.buildSuccessResp(fileSourceDTO.getId());
    }

    @Override
    public InternalResponse<Integer> getFileSourceIdByCode(String code) {
        FileSourceDTO fileSourceDTO = fileSourceService.getFileSourceByCode(code);
        if (null == fileSourceDTO) {
            throw new NotFoundException(ErrorCode.FAIL_TO_FIND_FILE_SOURCE_BY_CODE, new String[]{code});
        }
        return InternalResponse.buildSuccessResp(fileSourceDTO.getId());
    }

    @Override
    public InternalResponse<Boolean> existsFileSourceUsingCredential(Long appId, String credentialId) {
        boolean result = fileSourceService.existsFileSourceUsingCredential(appId, credentialId);
        return InternalResponse.buildSuccessResp(result);
    }

    @Override
    public InternalResponse<List<ServiceFileSourceAvailabilityDTO>> checkFileSourceAvailability(
        Long appId,
        List<Integer> fileSourceIdList
    ) {
        if (CollectionUtils.isEmpty(fileSourceIdList)) {
            return InternalResponse.buildSuccessResp(Collections.emptyList());
        }
        Set<Integer> idSet = new HashSet<>(fileSourceIdList);
        // 两次查询：一次拿归属/别名/启用状态，一次拿业务可见范围，合起来让调用方能区分三种失败原因
        Map<Integer, FileSourceBasicInfoDTO> basicInfoMap =
            fileSourceDAO.listFileSourceByIds(idSet)
                .stream()
                .collect(Collectors.toMap(FileSourceBasicInfoDTO::getId, basicInfo -> basicInfo));
        Set<Integer> idsInAppScope = fileSourceDAO.listFileSourceIdsInAppScope(appId, idSet);
        List<ServiceFileSourceAvailabilityDTO> resultList = new ArrayList<>(idSet.size());
        for (Integer id : idSet) {
            FileSourceBasicInfoDTO basicInfo = basicInfoMap.get(id);
            resultList.add(new ServiceFileSourceAvailabilityDTO(
                id,
                idsInAppScope.contains(id),
                basicInfo == null ? null : basicInfo.getAppId(),
                basicInfo == null ? null : basicInfo.getAlias(),
                basicInfo == null ? null : basicInfo.getEnable()
            ));
        }
        return InternalResponse.buildSuccessResp(resultList);
    }
}

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

package com.tencent.bk.job.file_gateway.api.iam;

import com.tencent.bk.job.common.iam.constant.ResourceId;
import com.tencent.bk.job.common.iam.service.BaseIamCallbackService;
import com.tencent.bk.job.common.iam.util.IamRespUtil;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.service.FileSourceService;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.IamSearchCondition;
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.SearchInstanceResponseDTO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class IamFileSourceCallbackResourceImpl extends BaseIamCallbackService
    implements IamFileSourceCallbackResource {

    private final FileSourceService fileSourceService;

    @Autowired
    public IamFileSourceCallbackResourceImpl(FileSourceService fileSourceService) {
        this.fileSourceService = fileSourceService;
    }

    @Data
    static class FileSourceSearchCondition {
        List<Long> appIdList;
        List<String> idStrList;
        List<Integer> fileSourceIdList;
        int start;
        int length;
        String keyword;

        public FileSourceSearchCondition(
            List<Long> appIdList,
            List<String> idStrList,
            List<Integer> fileSourceIdList,
            int start,
            int length,
            String keyword
        ) {
            this.appIdList = appIdList;
            this.idStrList = idStrList;
            this.fileSourceIdList = fileSourceIdList;
            this.start = start;
            this.length = length;
            this.keyword = keyword;
        }
    }

    private FileSourceSearchCondition getSearchCondition(CallbackRequestDTO callbackRequest) {
        IamSearchCondition searchCondition = IamSearchCondition.fromReq(callbackRequest);
        // 文件源列表实现
        List<Long> appIdList = searchCondition.getAppIdList();
        List<String> idStrList = searchCondition.getIdList();
        List<Integer> fileSourceIdList = null;
        if (idStrList != null) {
            fileSourceIdList = idStrList.parallelStream().map(Integer::parseInt).collect(Collectors.toList());
        }

        int start = searchCondition.getStart().intValue();
        int length = searchCondition.getLength().intValue();

        String keyword = callbackRequest.getFilter().getKeyword();
        return new FileSourceSearchCondition(
            appIdList, idStrList, fileSourceIdList, start, length, keyword
        );
    }

    private InstanceInfoDTO convert(FileSourceDTO fileSourceDTO) {
        InstanceInfoDTO tmpInstanceInfo = new InstanceInfoDTO();
        tmpInstanceInfo.setId(fileSourceDTO.getId().toString());
        tmpInstanceInfo.setDisplayName(fileSourceDTO.getAlias());
        return tmpInstanceInfo;
    }

    @Override
    protected ListInstanceResponseDTO listInstanceResp(CallbackRequestDTO callbackRequest) {
        FileSourceSearchCondition searchCondition = getSearchCondition(callbackRequest);

        List<FileSourceDTO> fileSourceDTOList = fileSourceService.listWorkTableFileSource(
            searchCondition.getAppIdList(),
            searchCondition.getFileSourceIdList(),
            searchCondition.getStart(),
            searchCondition.getLength()
        );
        Long totalCount = fileSourceService.countWorkTableFileSource(
            searchCondition.getAppIdList(), searchCondition.getFileSourceIdList()
        ).longValue();
        PageData<FileSourceDTO> fileSourceDTOPageData = new PageData<>(
            searchCondition.getStart(),
            searchCondition.getLength(),
            totalCount,
            fileSourceDTOList
        );
        return IamRespUtil.getListInstanceRespFromPageData(fileSourceDTOPageData, this::convert);
    }

    @Override
    protected SearchInstanceResponseDTO searchInstanceResp(CallbackRequestDTO callbackRequest) {
        FileSourceSearchCondition searchCondition = getSearchCondition(callbackRequest);

        List<FileSourceDTO> fileSourceDTOList = fileSourceService.listWorkTableFileSource(
            searchCondition.getAppIdList().get(0),
            null,
            searchCondition.getKeyword(),
            searchCondition.getStart(),
            searchCondition.getLength()
        );

        Long totalCount = fileSourceService.countWorkTableFileSource(
            searchCondition.getAppIdList(), searchCondition.getFileSourceIdList()
        ).longValue();
        PageData<FileSourceDTO> fileSourceDTOPageData = new PageData<>(
            searchCondition.getStart(),
            searchCondition.getLength(),
            totalCount,
            fileSourceDTOList
        );
        return IamRespUtil.getSearchInstanceRespFromPageData(fileSourceDTOPageData, this::convert);
    }

    @Override
    protected CallbackBaseResponseDTO fetchInstanceResp(
        CallbackRequestDTO callbackRequest
    ) {
        IamSearchCondition searchCondition = IamSearchCondition.fromReq(callbackRequest);
        List<Object> instanceAttributeInfoList = new ArrayList<>();
        for (String instanceId : searchCondition.getIdList()) {
            try {
                // 文件源详情查询实现
                FileSourceDTO fileSourceDTO = fileSourceService.getFileSourceById(Integer.parseInt(instanceId));
                if (fileSourceDTO == null) {
                    return getNotFoundRespById(instanceId);
                }
                // 拓扑路径构建
                List<PathInfoDTO> path = new ArrayList<>();
                PathInfoDTO rootNode = new PathInfoDTO();
                rootNode.setType(ResourceId.APP);
                rootNode.setId(fileSourceDTO.getAppId().toString());
                PathInfoDTO fileSourceNode = new PathInfoDTO();
                fileSourceNode.setType(ResourceId.FILE_SOURCE);
                fileSourceNode.setId(fileSourceDTO.getId().toString());
                rootNode.setChild(fileSourceNode);
                path.add(rootNode);
                // 实例组装
                InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
                instanceInfo.setId(instanceId);
                instanceInfo.setDisplayName(fileSourceDTO.getAlias());
                instanceInfo.setPath(path);
                instanceAttributeInfoList.add(instanceInfo);
            } catch (NumberFormatException e) {
                log.error("Parse object id failed!|{}", instanceId, e);
            }
        }

        FetchInstanceInfoResponseDTO fetchInstanceInfoResponse = new FetchInstanceInfoResponseDTO();
        fetchInstanceInfoResponse.setCode(0L);
        fetchInstanceInfoResponse.setData(instanceAttributeInfoList);
        return fetchInstanceInfoResponse;
    }

    @Override
    public CallbackBaseResponseDTO callback(CallbackRequestDTO callbackRequest) {
        return baseCallback(callbackRequest);
    }
}

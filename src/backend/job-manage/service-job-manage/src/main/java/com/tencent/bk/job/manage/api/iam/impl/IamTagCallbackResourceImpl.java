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

package com.tencent.bk.job.manage.api.iam.impl;

import com.tencent.bk.job.common.iam.constant.ResourceId;
import com.tencent.bk.job.common.iam.service.BaseIamCallbackService;
import com.tencent.bk.job.common.iam.util.IamRespUtil;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.api.iam.IamTagCallbackResource;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.service.TagService;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.IamSearchCondition;
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.SearchInstanceResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class IamTagCallbackResourceImpl extends BaseIamCallbackService implements IamTagCallbackResource {
    private final TagService tagService;

    @Autowired
    public IamTagCallbackResourceImpl(TagService tagService) {
        this.tagService = tagService;
    }

    private Pair<TagDTO, BaseSearchCondition> getBasicQueryCondition(CallbackRequestDTO callbackRequest) {
        IamSearchCondition searchCondition = IamSearchCondition.fromReq(callbackRequest);
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(searchCondition.getStart().intValue());
        baseSearchCondition.setLength(searchCondition.getLength().intValue());

        TagDTO tagQuery = new TagDTO();
        tagQuery.setAppId(searchCondition.getAppIdList().get(0));
        return Pair.of(tagQuery, baseSearchCondition);
    }

    private InstanceInfoDTO convert(TagDTO tagDTO) {
        InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
        instanceInfo.setId(String.valueOf(tagDTO.getId()));
        instanceInfo.setDisplayName(tagDTO.getName());
        return instanceInfo;
    }

    @Override
    protected SearchInstanceResponseDTO searchInstanceResp(CallbackRequestDTO callbackRequest) {
        Pair<TagDTO, BaseSearchCondition> basicQueryCond = getBasicQueryCondition(callbackRequest);

        TagDTO tagQuery = basicQueryCond.getLeft();
        BaseSearchCondition baseSearchCondition = basicQueryCond.getRight();

        tagQuery.setName(callbackRequest.getFilter().getKeyword());
        PageData<TagDTO> tagDTOPageData = tagService.listPageTags(tagQuery, baseSearchCondition);

        return IamRespUtil.getSearchInstanceRespFromPageData(tagDTOPageData, this::convert);
    }

    @Override
    protected ListInstanceResponseDTO listInstanceResp(CallbackRequestDTO callbackRequest) {
        Pair<TagDTO, BaseSearchCondition> basicQueryCond = getBasicQueryCondition(callbackRequest);

        TagDTO tagQuery = basicQueryCond.getLeft();
        BaseSearchCondition baseSearchCondition = basicQueryCond.getRight();
        PageData<TagDTO> tagDTOPageData = tagService.listPageTags(tagQuery, baseSearchCondition);

        return IamRespUtil.getListInstanceRespFromPageData(tagDTOPageData, this::convert);
    }

    @Override
    protected CallbackBaseResponseDTO fetchInstanceResp(CallbackRequestDTO callbackRequest) {
        IamSearchCondition searchCondition = IamSearchCondition.fromReq(callbackRequest);
        List<Object> instanceAttributeInfoList = new ArrayList<>();
        for (String instanceId : searchCondition.getIdList()) {
            try {
                Long tagId = Long.parseLong(instanceId);
                TagDTO tagDTO = tagService.getTagInfoById(tagId);
                if (tagDTO == null) {
                    return getNotFoundRespById(instanceId);
                }
                // 拓扑路径构建
                List<PathInfoDTO> path = new ArrayList<>();
                PathInfoDTO rootNode = new PathInfoDTO();
                rootNode.setType(ResourceId.APP);
                rootNode.setId(tagDTO.getAppId().toString());
                PathInfoDTO tagNode = new PathInfoDTO();
                tagNode.setType(ResourceId.TAG);
                tagNode.setId(tagDTO.getId().toString());
                rootNode.setChild(tagNode);
                path.add(rootNode);
                // 实例组装
                InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
                instanceInfo.setId(instanceId);
                instanceInfo.setDisplayName(tagDTO.getName());
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

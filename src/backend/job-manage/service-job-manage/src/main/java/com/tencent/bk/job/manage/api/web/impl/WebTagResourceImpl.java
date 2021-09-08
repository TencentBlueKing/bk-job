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

package com.tencent.bk.job.manage.api.web.impl;

import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import com.tencent.bk.job.common.web.controller.AbstractJobController;
import com.tencent.bk.job.manage.api.web.WebTagResource;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.web.request.TagCreateReq;
import com.tencent.bk.job.manage.model.web.vo.TagVO;
import com.tencent.bk.job.manage.service.TagService;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class WebTagResourceImpl extends AbstractJobController implements WebTagResource {
    private final TagService tagService;
    private final WebAuthService authService;

    @Autowired
    public WebTagResourceImpl(TagService tagService, WebAuthService webAuthService) {
        super(webAuthService.getAuthService());
        this.tagService = tagService;
        this.authService = webAuthService;
    }

    @Override
    public ServiceResponse<List<TagVO>> listTags(String username, Long appId, String tagName) {
        List<TagDTO> tags = tagService.listTags(appId, tagName);
        assert tags != null;
        List<TagVO> tagVOS = new ArrayList<>(tags.size());
        for (TagDTO tag : tags) {
            TagVO tagVO = new TagVO();
            tagVO.setId(tag.getId());
            tagVO.setName(tag.getName());
            tagVOS.add(tagVO);
        }
        return ServiceResponse.buildSuccessResp(tagVOS);
    }

    @Override
    public ServiceResponse<Boolean> updateTagInfo(String username, Long appId, Long tagId, String tagName) {
        AuthResultVO authResultVO = checkManageTagPermission(username, appId, tagId.toString());
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        return ServiceResponse.buildSuccessResp(tagService.updateTagById(appId, tagId, tagName, username));
    }

    @Override
    public ServiceResponse<Long> saveTagInfo(String username, Long appId, TagCreateReq tagCreateReq) {
        AuthResultVO authResult = checkCreateTagPermission(username, appId);
        if (!authResult.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResult);
        }
        Long tagId = tagService.insertNewTag(appId, tagCreateReq.getTagName(), username);
        authService.registerResource(tagId.toString(), tagCreateReq.getTagName(), ResourceId.TAG, username, null);
        return ServiceResponse.buildSuccessResp(tagId);
    }

    private AuthResultVO checkManageTagPermission(String username, Long appId, String tagId) {
        return authService.auth(true, username, ActionId.MANAGE_TAG, ResourceTypeEnum.TAG, tagId,
            buildTagPathInfo(appId));
    }

    // 暂时未用到
    private AuthResultVO checkCreateTagPermission(String username, Long appId) {
        return authService.auth(true, username, ActionId.CREATE_TAG, ResourceTypeEnum.BUSINESS, appId.toString(), null);
    }

    private PathInfoDTO buildTagPathInfo(Long appId) {
        return PathBuilder.newBuilder(ResourceTypeEnum.BUSINESS.getId(), appId.toString()).build();
    }
}

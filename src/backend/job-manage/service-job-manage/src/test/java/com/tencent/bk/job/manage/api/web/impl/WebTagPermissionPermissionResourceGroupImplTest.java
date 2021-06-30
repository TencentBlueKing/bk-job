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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.web.vo.TagVO;
import com.tencent.bk.job.manage.service.TagService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("标签管理API测试")
public class WebTagPermissionPermissionResourceGroupImplTest {
    private TagService mockTagService;
    private WebAuthService authService;
    private WebTagResourceImpl webTagResource;

    @BeforeEach
    public void init() {
        mockTagService = mock(TagService.class);
        authService = mock(WebAuthService.class);
        webTagResource = new WebTagResourceImpl(mockTagService, authService);
    }

    @AfterEach
    public void destroy() {
        this.mockTagService = null;
        this.webTagResource = null;
    }

    @Test
    @DisplayName("查询业务下的所有标签，返回结果")
    public void whenListTagThenReturnSuccResp() {
        Long appId = 2L;
        String tagName = "tag";
        List<TagDTO> tagsReturnByTagService = new ArrayList<>();
        TagDTO tag1 = new TagDTO();
        tag1.setId(1L);
        tag1.setName("tag1");
        TagDTO tag2 = new TagDTO();
        tag2.setId(2L);
        tag2.setName("tag2");
        tagsReturnByTagService.add(tag1);
        tagsReturnByTagService.add(tag2);
        when(mockTagService.listTags(appId, tagName)).thenReturn(tagsReturnByTagService);

        ServiceResponse<List<TagVO>> resp = webTagResource.listTags("user1", appId, tagName);

        assertThat(resp.isSuccess()).isEqualTo(true);
        assertThat(resp.getCode()).isEqualTo(ErrorCode.RESULT_OK);
        assertThat(resp.getData()).hasSize(2).extracting("name").contains("tag1", "tag2");
    }
}

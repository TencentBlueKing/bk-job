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

package com.tencent.bk.job.manage.api.esb.impl.v4;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.api.esb.v4.OpenApiUserScopeV4Resource;
import com.tencent.bk.job.manage.model.dto.UserAppScopeDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4AuthorizedScopeDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4GetUserAuthorizedScopesResult;
import com.tencent.bk.job.manage.service.scope.UserAppScopeQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class OpenApiUserScopeV4ResourceImpl implements OpenApiUserScopeV4Resource {

    private static final int DEFAULT_OFFSET = 0;
    private static final int DEFAULT_LENGTH = 10;
    private static final int MAX_LENGTH = 200;

    private final UserAppScopeQueryService userAppScopeQueryService;

    @Autowired
    public OpenApiUserScopeV4ResourceImpl(UserAppScopeQueryService userAppScopeQueryService) {
        this.userAppScopeQueryService = userAppScopeQueryService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_get_user_authorized_scopes"})
    public EsbV4Response<V4GetUserAuthorizedScopesResult> getUserAuthorizedScopes(String username,
                                                                                  String appCode,
                                                                                  Integer offset,
                                                                                  Integer length) {
        int normalizedOffset = normalizeOffset(offset);
        int normalizedLength = normalizeLength(length);

        PageData<UserAppScopeDTO> pageData =
            userAppScopeQueryService.listAuthorizedScopesPaged(username, normalizedOffset, normalizedLength);

        V4GetUserAuthorizedScopesResult result = new V4GetUserAuthorizedScopesResult();
        result.setTotal(pageData.getTotal());
        result.setOffset(normalizedOffset);
        result.setLength(normalizedLength);
        result.setScopeList(toScopeList(pageData.getData()));
        return EsbV4Response.success(result);
    }

    private int normalizeOffset(Integer offset) {
        if (offset == null) {
            return DEFAULT_OFFSET;
        }
        if (offset < 0) {
            throw new InvalidParamException(
                ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new Object[]{"offset", "offset must be greater than or equal to 0"}
            );
        }
        return offset;
    }

    private int normalizeLength(Integer length) {
        if (length == null) {
            return DEFAULT_LENGTH;
        }
        if (length < 1 || length > MAX_LENGTH) {
            throw new InvalidParamException(
                ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new Object[]{"length", "length must be between 1 and " + MAX_LENGTH}
            );
        }
        return length;
    }

    private List<V4AuthorizedScopeDTO> toScopeList(List<UserAppScopeDTO> data) {
        if (data == null || data.isEmpty()) {
            return Collections.emptyList();
        }
        return data.stream().map(this::toAuthorizedScopeDTO).collect(Collectors.toList());
    }

    private V4AuthorizedScopeDTO toAuthorizedScopeDTO(UserAppScopeDTO scope) {
        V4AuthorizedScopeDTO dto = new V4AuthorizedScopeDTO();
        dto.setScopeType(scope.getScopeType());
        dto.setScopeId(scope.getScopeId());
        dto.setName(scope.getName());
        dto.setFavor(scope.getFavor());
        dto.setFavorTime(scope.getFavorTime());
        dto.setTimeZone(scope.getTimeZone());
        return dto;
    }
}

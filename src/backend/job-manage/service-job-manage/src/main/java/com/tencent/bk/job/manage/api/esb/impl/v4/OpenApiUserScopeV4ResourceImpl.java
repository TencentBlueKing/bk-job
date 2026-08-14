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

import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.api.esb.v4.OpenApiUserScopeV4Resource;
import com.tencent.bk.job.manage.model.dto.UserAppScopeDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4AuthorizedScopeDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4GetUserAuthorizedScopesResult;
import com.tencent.bk.job.manage.service.scope.UserAppScopeQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class OpenApiUserScopeV4ResourceImpl implements OpenApiUserScopeV4Resource {

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
        PageData<UserAppScopeDTO> pageData =
            userAppScopeQueryService.listAuthorizedScopesPaged(username, offset, length);

        V4GetUserAuthorizedScopesResult result = new V4GetUserAuthorizedScopesResult();
        result.setTotal(pageData.getTotal());
        result.setOffset(offset);
        result.setLength(length);
        result.setScopeList(toScopeList(pageData.getData()));
        return EsbV4Response.success(result);
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
        if (scope.getFavorTime() != null) {
            // 收藏是用户行为，按当前用户时区（由网关注入、拦截器写入上下文，缺失时兜底东八区）格式化收藏时间
            ZoneId userZone = JobContextUtil.getTimeZone();
            dto.setFavorTime(DateUtils.formatUnixTimestamp(
                scope.getFavorTime(), ChronoUnit.MILLIS, DateUtils.DATETIME_PATTERN_WITH_MILLIS, userZone));
        }
        dto.setTimeZone(scope.getTimeZone());
        return dto;
    }
}

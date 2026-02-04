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

package com.tencent.bk.job.manage.service.scope;

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.constant.TenantIdConstants;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.config.ScopePanelProperties;
import com.tencent.bk.job.manage.model.web.vo.AppVO;
import com.tencent.bk.job.manage.model.web.vo.ScopeGroup;
import com.tencent.bk.job.manage.model.web.vo.ScopeGroupPanel;
import com.tencent.bk.job.manage.model.web.vo.ScopeVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 资源范围（业务、业务集等）面板服务
 */
@Slf4j
@Service
public class ScopePanelService {

    private final ScopePanelProperties scopePanelProperties;
    private final AuthService authService;
    private final MessageI18nService messageI18nService;

    @Autowired
    public ScopePanelService(ScopePanelProperties scopePanelProperties,
                             AuthService authService,
                             MessageI18nService messageI18nService) {
        this.scopePanelProperties = scopePanelProperties;
        this.authService = authService;
        this.messageI18nService = messageI18nService;
    }

    /**
     * 通过用户名+Job业务列表数据，构建资源范围分组面板数据
     *
     * @param username 当前用户名
     * @param appList  Job业务列表数据
     * @return 资源范围分组面板数据
     */
    public ScopeGroupPanel buildScopeGroupPanel(String username, List<AppVO> appList) {
        ScopeGroupPanel scopeGroupPanel = new ScopeGroupPanel();
        List<ScopeGroup> scopeGroupList = buildScopeGroupList(appList);
        scopeGroupPanel.setScopeGroupList(scopeGroupList);
        scopeGroupPanel.setCanApply(decideIfUserCanApply(username));
        if (scopeGroupPanel.isCanApply()) {
            String applyUrl = tryToGetApplyUrl();
            scopeGroupPanel.setApplyUrl(applyUrl);
        }
        return scopeGroupPanel;
    }

    /**
     * 尝试获取权限申请链接，如果失败，返回null
     *
     * @return 权限申请链接
     */
    private String tryToGetApplyUrl() {
        try {
            return authService.getApplyUrl(ActionId.ACCESS_BUSINESS, ResourceTypeEnum.BUSINESS);
        } catch (Throwable t) {
            log.warn("Fail to getApplyUrl", t);
            return null;
        }
    }

    /**
     * 通过Job业务列表数据构建资源范围分组列表
     *
     * @param appList Job业务列表数据
     * @return 资源范围分组列表
     */
    private List<ScopeGroup> buildScopeGroupList(List<AppVO> appList) {
        if (CollectionUtils.isEmpty(appList)) {
            return Collections.emptyList();
        }
        List<ScopeGroup> scopeGroupList = new ArrayList<>();
        Map<String, List<ScopeVO>> id2ChildrenMap = new HashMap<>();
        User user = JobContextUtil.getUser();
        for (ResourceScopeTypeEnum scopeTypeEnum : ResourceScopeTypeEnum.values()) {
            if (!shouldDisplayScopeGroupForUser(user, scopeTypeEnum)) {
                continue;
            }
            ScopeGroup scopeGroup = new ScopeGroup();
            scopeGroup.setId(scopeTypeEnum.getValue());
            scopeGroup.setName(messageI18nService.getI18n(scopeTypeEnum.getI18nKey()));
            scopeGroup.setChildren(new ArrayList<>());
            id2ChildrenMap.put(scopeGroup.getId(), scopeGroup.getChildren());
            scopeGroupList.add(scopeGroup);
        }
        for (AppVO appVO : appList) {
            String scopeType = appVO.getScopeType();
            List<ScopeVO> mappedScopeList = id2ChildrenMap.get(scopeType);
            if (mappedScopeList == null) {
                log.warn("Unknown scopeType:{}, ignore app:{}", scopeType, appVO);
                continue;
            }
            // 不展示无权限的资源范围
            if (!scopePanelProperties.getShowNoPermissionScopes() && !appVO.getHasPermission()) {
                continue;
            }
            ScopeVO scopeVO = new ScopeVO();
            scopeVO.setId(appVO.getScopeId());
            scopeVO.setName(appVO.getName());
            scopeVO.setFavor(appVO.getFavor());
            scopeVO.setFavorTime(appVO.getFavorTime());
            scopeVO.setHasPermission(appVO.getHasPermission());
            scopeVO.setTimeZone(appVO.getTimeZone());
            mappedScopeList.add(scopeVO);
        }
        return scopeGroupList;
    }

    /**
     * 判断是否应该向当前用户展示某个资源范围分组
     *
     * @param user          当前用户
     * @param scopeTypeEnum 资源范围类型
     * @return 布尔值
     */
    private boolean shouldDisplayScopeGroupForUser(User user, ResourceScopeTypeEnum scopeTypeEnum) {
        if (scopeTypeEnum != ResourceScopeTypeEnum.TENANT_SET) {
            return true;
        }
        // 只有系统租户下才展示租户集分组
        return TenantIdConstants.SYSTEM_TENANT_ID.equals(user.getTenantId());
    }

    /**
     * 判断当前用户是否能够主动申请权限
     *
     * @param username 当前用户名
     * @return 是否能够主动申请权限
     */
    private boolean decideIfUserCanApply(String username) {
        // 通过太湖登录的用户禁止主动申请权限，只能联系业务运维分配
        return !(StringUtils.isNotEmpty(username) && username.endsWith("@tai"));
    }
}

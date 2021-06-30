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

package com.tencent.bk.job.manage.common.consts.notify;

import com.tencent.bk.job.common.util.I18nUtil;
import com.tencent.bk.job.manage.model.web.vo.notify.RoleVO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;

import java.util.ArrayList;
import java.util.List;

/**
 * 在Job中的角色
 * 业务相关角色从CMDB获取，取二者并集
 */
@Getter
@AllArgsConstructor
public enum JobRoleEnum {
    /**
     * 资源所属者
     */
    JOB_RESOURCE_OWNER("job.manage.role.resource.owner"),

    /**
     * 任务执行人
     */
    JOB_RESOURCE_TRIGGER_USER("job.manage.role.resource.trigger.user"),

    /**
     * 额外通知人
     */
    JOB_EXTRA_OBSERVER("job.manage.role.extra.observer");

    private final String defaultName;

    public static List<RoleVO> getVOList() {
        List<RoleVO> resultList = new ArrayList<RoleVO>();
        val values = JobRoleEnum.values();
        for (int i = 0; i < values.length; i++) {
            resultList.add(new RoleVO(values[i].name(), I18nUtil.getI18nMessage(values[i].getDefaultName())));
        }
        return resultList;
    }

    public static RoleVO getVO(String role) {
        JobRoleEnum roleEnum = JobRoleEnum.valueOf(role);
        return new RoleVO(roleEnum.name(), I18nUtil.getI18nMessage(roleEnum.getDefaultName()));
    }
}

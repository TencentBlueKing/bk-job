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

package com.tencent.bk.job.common.model.permission;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 返回给前端的鉴权结果
 */
@Data
@ToString
@ApiModel("鉴权结果-前端")
public class AuthResultVO {
    /**
     * 权限是否通过
     */
    boolean pass;

    /**
     * 申请权限跳转url
     */
    @ApiModelProperty("申请权限跳转url")
    private String applyUrl;

    /**
     * 需要申请的权限列表
     */
    @ApiModelProperty("需要申请的权限列表")
    private List<RequiredPermissionVO> requiredPermissions;

    public static AuthResultVO pass() {
        AuthResultVO authResultVO = new AuthResultVO();
        authResultVO.pass = true;
        return authResultVO;
    }

    public static AuthResultVO fail() {
        AuthResultVO authResultVO = new AuthResultVO();
        authResultVO.pass = false;
        return authResultVO;
    }

    public AuthResultVO addRequiredPermission(String actionName, String resourceTypeName, String resourceName) {
        if (requiredPermissions == null) {
            requiredPermissions = new ArrayList<>();
        }
        RequiredPermissionVO requiredPermissionVO = null;
        for (RequiredPermissionVO permission : requiredPermissions) {
            if (permission.getActionName().equals(actionName)) {
                requiredPermissionVO = permission;
                break;
            }
        }
        if (requiredPermissionVO == null) {
            requiredPermissionVO = new RequiredPermissionVO();
            requiredPermissionVO.setActionName(actionName);
            if (StringUtils.isNotEmpty(resourceTypeName) && StringUtils.isNotEmpty(resourceName)) {
                requiredPermissionVO.addResource(resourceTypeName, resourceName);
            }
            requiredPermissions.add(requiredPermissionVO);
        } else {
            requiredPermissionVO.addResource(resourceTypeName, resourceName);
        }
        return this;
    }

}

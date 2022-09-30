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

package com.tencent.bk.job.manage.model.web.request;

import com.tencent.bk.job.manage.common.consts.whiteip.ActionScopeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ApiModel("主机检查请求报文")
public class HostCheckReq {

    @Deprecated
    @ApiModelProperty(value = "兼容字段，请勿使用：应用场景：脚本执行/文件分发")
    ActionScopeEnum actionScope;

    @ApiModelProperty(value = "应用场景：脚本执行/文件分发，Key为actionScope，Value分别为SCRIPT_EXECUTE/FILE_DISTRIBUTION")
    Map<String, Object> meta = new HashMap<>();

    @ApiModelProperty(value = "hostId列表", required = true)
    List<Long> hostIdList = new ArrayList<>();

    @ApiModelProperty(value = "IP列表，单个IP格式：cloudAreaId:ip或ip")
    List<String> ipList = new ArrayList<>();

    @ApiModelProperty(value = "IPv6列表，单个IPv6格式：cloudAreaId:ipv6或ipv6")
    List<String> ipv6List = new ArrayList<>();

    @ApiModelProperty(value = "关键字列表，可匹配主机名称")
    List<String> keyList = new ArrayList<>();

    public ActionScopeEnum getActionScope() {
        String actionScopeName = (String) meta.get("actionScope");
        if (actionScopeName == null) {
            return null;
        }
        return ActionScopeEnum.valueOf(actionScopeName);
    }

    public void setActionScope(ActionScopeEnum actionScope) {
        this.actionScope = actionScope;
        if (actionScope != null) {
            this.meta.put("actionScope", actionScope.name());
        }
    }
}



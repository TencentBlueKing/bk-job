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

package com.tencent.bk.job.manage.model.web.request.whiteip;

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.manage.model.web.request.ipchooser.HostIdWithMeta;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@ApiModel("IP白名单记录创建请求")
public class WhiteIPRecordCreateUpdateReq {

    @ApiModelProperty(value = "ID,更新IP白名单记录的时候需要传入，新建时不需要")
    private Long id;

    @ApiModelProperty(value = "是否对所有资源范围生效，默认为false")
    private boolean allScope = false;

    @ApiModelProperty(value = "多个资源范围列表")
    private List<ResourceScope> scopeList;

    @ApiModelProperty(value = "兼容字段，请勿再使用：云区域ID")
    private Long cloudAreaId;

    @CompatibleImplementation(name = "ipv6", explain = "兼容字段，保证发布过程中无损变更，下个版本删除", version = "3.8.0")
    @ApiModelProperty(value = "兼容字段，请勿再使用：IPs(换行分隔)")
    private String ipStr;

    @ApiModelProperty(value = "主机列表", required = true)
    private List<HostIdWithMeta> hostList = new ArrayList<>();

    @ApiModelProperty(value = "备注", required = true)
    private String remark;

    @ApiModelProperty(value = "生效范围（id列表）", required = true)
    private List<Long> actionScopeIdList;
}

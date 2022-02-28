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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("web页面操作预鉴权请求")
public class OperationPermissionReq {
    @ApiModelProperty("操作ID,取值为: [script/create,script/view,script/edit,script/delete,script/execute,script/clone]," +
        "[job_template/create,job_template/view,job_template/edit,job_template/delete,job_template/clone," +
        "job_template/debug],[job_plan/create,job_plan/view,job_plan/edit,job_plan/delete,job_plan/execute," +
        "job_plan/sync],[account/create,account/view,account/edit,account/delete],[public_script/create," +
        "public_script/view,public_script/edit,public_script/delete,public_script/execute],[whitelist/create," +
        "whitelist/view,whitelist/edit,whitelist/delete],[tag/create,tag/edit,tag/delete]")
    private String operation;

    @ApiModelProperty("资源ID,比如作业ID,定时任务ID;对于部分不需要资源ID的操作(新建),不需要传参")
    private String resourceId;

    @ApiModelProperty("业务ID")
    private Long appId;

    @ApiModelProperty("范畴类型:biz/business_set")
    private String scopeType;

    @ApiModelProperty("范畴ID")
    private String scopeId;

    @ApiModelProperty("是否返回详细的权限信息(依赖的权限，申请URL)。默认为false")
    private boolean returnPermissionDetail;
}

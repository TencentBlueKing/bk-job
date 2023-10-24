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

package com.tencent.bk.job.manage.model.web.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.StringJoiner;

/**
 * 脚本VO
 */
@Getter
@Setter
@ApiModel("脚本信息")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ScriptVO extends BasicScriptVO {

    /**
     * 脚本内容
     */
    @ApiModelProperty(value = "脚本内容")
    private String content;

    @ApiModelProperty(value = "脚本版本列表")
    private List<ScriptVO> scriptVersions;

    @ApiModelProperty(value = "关联的作业模板数量")
    private Integer relatedTaskTemplateNum;

    @ApiModelProperty(value = "关联的执行方案数量")
    private Integer relatedTaskPlanNum;

    /**
     * 是否支持同步
     */
    @ApiModelProperty(value = "是否支持同步")
    private Boolean syncEnabled = false;

    @Override
    public String toString() {
        return new StringJoiner(", ", ScriptVO.class.getSimpleName() + "[", "]")
            .add("content=******")
            .add("scriptVersions=" + scriptVersions)
            .add("relatedTaskTemplateNum=" + relatedTaskTemplateNum)
            .add("relatedTaskPlanNum=" + relatedTaskPlanNum)
            .add("syncEnabled=" + syncEnabled)
            .add("scriptVersionId=" + scriptVersionId)
            .add("id='" + id + "'")
            .add("name='" + name + "'")
            .add("type=" + type)
            .add("typeName='" + typeName + "'")
            .add("publicScript=" + publicScript)
            .add("scopeType='" + scopeType + "'")
            .add("scopeId='" + scopeId + "'")
            .add("category=" + category)
            .add("version='" + version + "'")
            .add("status=" + status)
            .add("statusDesc='" + statusDesc + "'")
            .add("canManage=" + canManage)
            .add("canClone=" + canClone)
            .add("creator='" + creator + "'")
            .add("createTime=" + createTime)
            .add("lastModifyUser='" + lastModifyUser + "'")
            .add("lastModifyTime=" + lastModifyTime)
            .add("tags=" + tags)
            .add("versionDesc='" + versionDesc + "'")
            .add("description='" + description + "'")
            .toString();
    }
}

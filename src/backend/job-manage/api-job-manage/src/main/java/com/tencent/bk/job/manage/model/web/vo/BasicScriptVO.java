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

package com.tencent.bk.job.manage.model.web.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import com.tencent.bk.job.manage.api.common.constants.JobResourceStatusEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.StringJoiner;

/**
 * 脚本基本信息
 *
 * @date 2019/09/19
 */
@Getter
@Setter
@ApiModel("脚本基本信息")
public class BasicScriptVO {
    /**
     * 脚本版本ID
     */
    @ApiModelProperty(value = "脚本版本ID")
    protected Long scriptVersionId;
    /**
     * 脚本ID，一个脚本下面包含多个版本的脚本
     */
    @ApiModelProperty(value = "脚本ID")
    protected String id;
    /**
     * 脚本名称
     */
    @ApiModelProperty(value = "脚本名称")
    protected String name;
    /**
     * 脚本类型
     */
    @ApiModelProperty(value = "脚本类型")
    protected Integer type;

    /**
     * 脚本类型名称
     */
    protected String typeName;

    /**
     * 是否公共脚本
     */
    @ApiModelProperty(value = "是否公共脚本")
    protected Boolean publicScript;

    /**
     * 资源范围类型
     */
    @ApiModelProperty(value = "资源范围类型", allowableValues = "biz-业务,biz_set-业务集")
    protected String scopeType;

    /**
     * 资源范围ID
     */
    @ApiModelProperty("资源范围ID")
    protected String scopeId;
    /**
     * 脚本大种类: 0 系统的执行脚本(如shell,bat,python等), 1 SQL执行脚本
     */
    @ApiModelProperty(value = "脚本分类: 0 系统的执行脚本(如shell,bat,python等), 1 SQL执行脚本")
    protected Integer category;
    /**
     * 脚本的版本号
     */
    @ApiModelProperty(value = "版本号")
    protected String version;

    /**
     * 脚本状态
     *
     * @see JobResourceStatusEnum
     */
    @ApiModelProperty(value = "脚本状态,0:未发布，1:已上线")
    protected Integer status;

    @ApiModelProperty(value = "脚本状态描述")
    protected String statusDesc;

    @ApiModelProperty("是否可以查看")
    private Boolean canView;
    @ApiModelProperty("是否可以管理")
    protected Boolean canManage;
    @ApiModelProperty("是否可以克隆")
    protected Boolean canClone;

    /**
     * 创建者
     */
    @ApiModelProperty(value = "创建者")
    protected String creator;

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    protected Long createTime;

    /**
     * 最后修改人
     */
    @ApiModelProperty(value = "最后更新者")
    protected String lastModifyUser;

    /**
     * 最后修改时间
     */
    @ApiModelProperty(value = "最后更新时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    protected Long lastModifyTime;

    /**
     * 脚本标签
     */
    @ApiModelProperty(value = "脚本标签")
    protected List<TagVO> tags;

    /**
     * 脚本版本描述
     */
    @ApiModelProperty(value = "脚本版本描述")
    protected String versionDesc;

    /**
     * 脚本描述
     */
    @ApiModelProperty(value = "脚本描述")
    protected String description;

    @Override
    public String toString() {
        return new StringJoiner(", ", BasicScriptVO.class.getSimpleName() + "[", "]")
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
            .add("canView=" + canView)
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

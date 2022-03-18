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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.StringJoiner;

/**
 * 脚本VO
 *
 * @date 2019/09/19
 */
@Getter
@Setter
@ApiModel("脚本信息")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ScriptVO {
    /**
     * 脚本版本ID
     */
    @ApiModelProperty(value = "脚本版本ID")
    private Long scriptVersionId;
    /**
     * 脚本ID，一个脚本下面包含多个版本的脚本
     */
    @ApiModelProperty(value = "脚本ID")
    private String id;
    /**
     * 脚本名称
     */
    @ApiModelProperty(value = "脚本名称")
    private String name;
    /**
     * 脚本类型
     */
    @ApiModelProperty(value = "脚本类型")
    private Integer type;
    /**
     * 脚本类型名称
     */
    private String typeName;
    /**
     * 是否公共脚本
     */
    @ApiModelProperty(value = "是否公共脚本")
    private Boolean publicScript;
    /**
     * 业务id
     */
    @ApiModelProperty("业务ID")
    @CompatibleImplementation(explain = "为了无损发布保留的历史字段，发布完成需要删除", version = "3.5.1")
    private Long appId;

    /**
     * 资源范围类型
     */
    @ApiModelProperty(value = "资源范围类型", allowableValues = "biz-业务,biz_set-业务集")
    private String scopeType;

    /**
     * 资源范围ID
     */
    @ApiModelProperty("资源范围ID")
    private String scopeId;
    /**
     * 脚本内容
     */
    @ApiModelProperty(value = "脚本内容")
    private String content;

    /**
     * 创建者
     */
    @ApiModelProperty(value = "创建者")
    private String creator;

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long createTime;

    /**
     * 最后修改人
     */
    @ApiModelProperty(value = "最后更新者")
    private String lastModifyUser;

    /**
     * 最后修改时间
     */
    @ApiModelProperty(value = "最后更新时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long lastModifyTime;

    /**
     * 脚本大种类: 0 系统的执行脚本(如shell,bat,python等), 1 SQL执行脚本
     */
    @ApiModelProperty(value = "脚本分类: 0 系统的执行脚本(如shell,bat,python等), 1 SQL执行脚本")
    private Integer category;
    /**
     * 脚本的版本号
     */
    @ApiModelProperty(value = "版本号")
    private String version;

    /**
     * 脚本标签
     */
    @ApiModelProperty(value = "脚本标签")
    private List<TagVO> tags;

    /**
     * 脚本状态
     *
     * @see JobResourceStatusEnum
     */
    @ApiModelProperty(value = "脚本状态,0:未发布，1:已上线")
    private Integer status;

    @ApiModelProperty(value = "脚本状态描述")
    private String statusDesc;

    @ApiModelProperty(value = "脚本版本列表")
    private List<ScriptVO> scriptVersions;

    @ApiModelProperty(value = "关联的作业模板数量")
    private Integer relatedTaskTemplateNum;

    @ApiModelProperty(value = "关联的执行方案数量")
    private Integer relatedTaskPlanNum;

    /**
     * 脚本版本描述
     */
    @ApiModelProperty(value = "脚本版本描述")
    private String versionDesc;

    /**
     * 脚本描述
     */
    @ApiModelProperty(value = "脚本描述")
    private String description;

    @ApiModelProperty("是否可以查看")
    private Boolean canView;
    @ApiModelProperty("是否可以管理")
    private Boolean canManage;
    @ApiModelProperty("是否可以克隆")
    private Boolean canClone;
    /**
     * 是否支持同步
     */
    @ApiModelProperty(value = "是否支持同步")
    private Boolean syncEnabled = false;


    @Override
    public String toString() {
        return new StringJoiner(", ", ScriptVO.class.getSimpleName() + "[", "]")
            .add("scriptVersionId=" + scriptVersionId)
            .add("id='" + id + "'")
            .add("name='" + name + "'")
            .add("type=" + type)
            .add("publicScript=" + publicScript)
            .add("appId=" + appId)
            .add("category=" + category)
            .add("version='" + version + "'")
            .add("tags='" + tags + "'")
            .add("status=" + status)
            .add("desc='" + versionDesc + "'")
            .add("content='*******'")
            .toString();
    }
}

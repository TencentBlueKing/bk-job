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

package com.tencent.bk.job.manage.model.web.vo.task;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.common.util.json.LongTimestampDeserializer;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import com.tencent.bk.job.manage.model.web.vo.TagVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@ApiModel("模版信息")
public class TaskTemplateVO {
    /**
     * 模版 ID
     */
    @ApiModelProperty(value = "模版 ID")
    private Long id;

    /**
     * 模版名称
     */
    @ApiModelProperty(value = "模版名称")
    private String name;

    /**
     * 模版标签
     */
    @ApiModelProperty(value = "模版标签")
    private List<TagVO> tags;

    /**
     * 模版状态
     */
    @ApiModelProperty(value = "模版状态 0 - 新创建，初始状态 1-审核中 2-审核通过，已发布 3-未通过，审核已驳回")
    private Integer status;

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
    @JsonDeserialize(using = LongTimestampDeserializer.class)
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
    @JsonDeserialize(using = LongTimestampDeserializer.class)
    private Long lastModifyTime;

    /**
     * 模版内的脚本状态
     */
    @ApiModelProperty(value = "模版内脚本状态 0 - 正常 1 - 需要更新 2 - 被禁用 3 - 需要更新和被禁用的都存在")
    private Integer scriptStatus;

    /**
     * 是否收藏
     */
    @ApiModelProperty(value = "是否收藏 0 - 未收藏 1 - 收藏")
    private Integer favored;

    /**
     * 模版描述
     */
    @ApiModelProperty(value = "模版描述 详细信息独有")
    private String description;

    /**
     * 模版变量列表
     */
    @ApiModelProperty(value = "模版变量列表 详细信息独有")
    private List<TaskVariableVO> variableList;

    /**
     * 模版脚本列表
     */
    @ApiModelProperty(value = "模版脚本列表 详细信息独有")
    private List<TaskStepVO> stepList;

    /**
     * 模版版本
     */
    @ApiModelProperty("模版版本")
    private String version;

    /**
     * 是否有查看权限
     */
    @ApiModelProperty("是否有查看权限")
    private Boolean canView;

    /**
     * 是否有编辑权限
     */
    @ApiModelProperty("是否有编辑权限")
    private Boolean canEdit;

    /**
     * 是否有删除权限
     */
    @ApiModelProperty("是否有删除权限")
    private Boolean canDelete;

    /**
     * 是否有调试权限
     */
    @ApiModelProperty("是否有调试权限")
    private Boolean canDebug;

    /**
     * 是否有克隆权限
     */
    @ApiModelProperty("是否有克隆权限")
    private Boolean canClone;

}

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

package com.tencent.bk.job.manage.model.query;

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.manage.common.consts.task.TaskTemplateStatusEnum;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 作业模板查询
 */
@Getter
@Setter
@ToString
@Builder
public class TaskTemplateQuery implements Cloneable {
    /**
     * 模板ID. 如果指定了id，那么忽略其他的检索条件
     */
    private Long id;
    /**
     * 业务ID
     */
    private Long appId;
    /**
     * 模板ID列表
     */
    private List<Long> ids;
    /**
     * 结果中需要排除的模板ID列表
     */
    private List<Long> excludeTemplateIds;
    /**
     * 模板名称
     */
    private String name;
    /**
     * 脚本状态
     */
    private Integer scriptStatus;
    /**
     * 模板状态
     */
    private TaskTemplateStatusEnum status;
    /**
     * 模板使用的标签列表。标签之间为逻辑与关系
     */
    private List<TagDTO> tags;
    /**
     * 是否过滤未标记的模板
     */
    private boolean untaggedTemplate;
    /**
     * 通用查询条件
     */
    private BaseSearchCondition baseSearchCondition;

    public boolean isExistIdCondition() {
        return this.id != null && this.id > 0;
    }

    public boolean isExistTagCondition() {
        return this.untaggedTemplate || CollectionUtils.isNotEmpty(this.tags);
    }

    public TaskTemplateQuery clone() {
        TaskTemplateQuery cloneQuery =  TaskTemplateQuery.builder().id(id).appId(appId).name(name)
            .scriptStatus(scriptStatus).status(status).untaggedTemplate(untaggedTemplate).build();
        if (CollectionUtils.isNotEmpty(ids)) {
            cloneQuery.setIds(new ArrayList<>(ids));
        }
        if (CollectionUtils.isNotEmpty(excludeTemplateIds)) {
            cloneQuery.setExcludeTemplateIds(new ArrayList<>(excludeTemplateIds));
        }
        if (CollectionUtils.isNotEmpty(tags)) {
            cloneQuery.setTags(tags.stream().map(TagDTO::clone).collect(Collectors.toList()));
        }
        if (baseSearchCondition != null) {
            cloneQuery.setBaseSearchCondition(baseSearchCondition.clone());
        }
        return cloneQuery;
    }
}

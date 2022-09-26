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

package com.tencent.bk.job.common.model.dto;

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * 资源范围
 */
@Getter
@Setter
@NoArgsConstructor
@ApiModel("资源范围实体")
public class ResourceScope {
    /**
     * 资源范围类型
     */
    @ApiModelProperty(value = "枚举，资源范围类型", required = true)
    private ResourceScopeTypeEnum type;
    /**
     * 资源范围ID,比如cmdb业务ID、cmdb业务集ID
     */
    @ApiModelProperty(value = "资源范围Id", required = true)
    private String id;

    public ResourceScope(String type, String id) {
        this.type = ResourceScopeTypeEnum.from(type);
        this.id = id;
    }

    public ResourceScope(ResourceScopeTypeEnum type, String id) {
        this.type = type;
        this.id = id;
    }


    @Override
    public String toString() {
        return new StringJoiner(", ", ResourceScope.class.getSimpleName() + "[", "]")
            .add("type=" + type)
            .add("id='" + id + "'")
            .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceScope that = (ResourceScope) o;
        return type == that.type &&
            id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, id);
    }
}

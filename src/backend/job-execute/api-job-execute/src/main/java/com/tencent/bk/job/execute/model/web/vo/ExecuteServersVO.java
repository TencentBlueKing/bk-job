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

package com.tencent.bk.job.execute.model.web.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.model.vo.DynamicGroupIdWithMeta;
import com.tencent.bk.job.common.util.json.JsonUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@ApiModel("目标服务器信息")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecuteServersVO {

    @ApiModelProperty("静态 IP 列表")
    @JsonProperty("hostList")
    private List<ExecuteHostVO> hostList;

    @ApiModelProperty("拓扑节点")
    @JsonProperty("nodeList")
    private List<ExecuteTopoNodeVO> nodeList;

    @ApiModelProperty("动态分组 ID")
    @JsonProperty("dynamicGroupList")
    private List<DynamicGroupIdWithMeta> dynamicGroupList;

    @JsonIgnore
    public List<String> getDynamicGroupIdList() {
        if (CollectionUtils.isEmpty(dynamicGroupList)) {
            return Collections.emptyList();
        }
        List<String> dynamicGroupIdList = new ArrayList<>();
        for (DynamicGroupIdWithMeta dynamicGroup : dynamicGroupList) {
            DynamicGroupIdWithMeta dynamicGroupIdWithMeta = JsonUtils.fromJson(
                JsonUtils.toJson(dynamicGroup),
                new TypeReference<DynamicGroupIdWithMeta>() {
                }
            );
            dynamicGroupIdList.add(dynamicGroupIdWithMeta.getId());
        }
        return dynamicGroupIdList;
    }
}

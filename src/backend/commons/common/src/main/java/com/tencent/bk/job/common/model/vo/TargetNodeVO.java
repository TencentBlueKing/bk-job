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

package com.tencent.bk.job.common.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.util.JobContextUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * @since 12/12/2019 15:07
 */
@Slf4j
@Data
@ApiModel("目标节点信息")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TargetNodeVO {
    @ApiModelProperty(value = "节点 ID，对应拓扑树节点中的instanceId", required = true)
    private Long instanceId;
    @CompatibleImplementation(name = "ipv6", explain = "发布过程中兼容实现，发布后删除", deprecatedVersion = "3.8.0")
    private Long id;

    @ApiModelProperty(value = "节点类型 biz-业务 set-集群 module-模块 xxx-用户自定义节点类型，对应拓扑树节点中的objectId", required = true)
    private String objectId;
    @CompatibleImplementation(name = "ipv6", explain = "发布过程中兼容实现，发布后删除", deprecatedVersion = "3.8.0")
    private String type;

    @CompatibleImplementation(name = "ipv6", explain = "发布过程中兼容实现，发布后删除", deprecatedVersion = "3.8.0")
    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
        this.id = instanceId;
    }

    @CompatibleImplementation(name = "ipv6", explain = "发布过程中兼容实现，发布后删除", deprecatedVersion = "3.8.0")
    public void setId(Long id) {
        this.id = id;
        this.instanceId = id;
        log.warn("Use compatible field id:{}", id);
    }

    @CompatibleImplementation(name = "ipv6", explain = "发布过程中兼容实现，发布后删除", deprecatedVersion = "3.8.0")
    public void setObjectId(String objectId) {
        this.objectId = objectId;
        this.type = objectId;
    }

    @CompatibleImplementation(name = "ipv6", explain = "发布过程中兼容实现，发布后删除", deprecatedVersion = "3.8.0")
    public void setType(String type) {
        this.type = type;
        this.objectId = type;
        log.warn("Use compatible field type:{}", type);
    }

    public boolean validate(boolean isCreate) {
        if (instanceId != null && instanceId > 0 && StringUtils.isNotBlank(objectId)) {
            return true;
        }
        JobContextUtil.addDebugMessage("Target node info does not have id or type");
        return false;
    }
}

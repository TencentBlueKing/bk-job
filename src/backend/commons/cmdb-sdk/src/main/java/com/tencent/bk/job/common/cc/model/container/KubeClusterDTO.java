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

package com.tencent.bk.job.common.cc.model.container;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Arrays;
import java.util.List;

/**
 * 容器集群
 */
@Data
public class KubeClusterDTO {

    /**
     * 集群模型 cmdb 字段
     */
    public interface Fields {
        String BK_BIZ_ID = "bk_biz_id";
        String ID = "id";
        String NAME = "name";
        String CLUSTER_UID = "uid";
        String BK_PROJECT_ID = "bk_project_id";
        String BK_PROJECT_NAME = "bk_project_name";

        /**
         * CMDB 模型的字段名
         */
        List<String> ALL = Arrays.asList(BK_BIZ_ID, ID, NAME, CLUSTER_UID, BK_PROJECT_ID, BK_PROJECT_NAME);
    }


    /**
     * 业务 ID
     */
    @JsonProperty("bk_biz_id")
    private Long bizId;

    /**
     * 集群资源 ID（集群在 cmdb 中注册资源的 ID）
     */
    private Long id;

    /**
     * 集群名称
     */
    private String name;

    /**
     * 集群 ID，比如： BCS-K8S-00000
     */
    private String uid;

    @JsonProperty("bk_project_id")
    private String bkProjectId;

    @JsonProperty("bk_project_name")
    private String bkProjectName;


}

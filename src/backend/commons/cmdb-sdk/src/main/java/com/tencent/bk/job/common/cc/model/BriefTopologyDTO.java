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

package com.tencent.bk.job.common.cc.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@NoArgsConstructor
@Data
public class BriefTopologyDTO {

    @JsonProperty("biz")
    private Biz biz;
    @JsonProperty("idle")
    private List<Node> idleNodeList;
    @JsonProperty("nds")
    private List<Node> childNodeList;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BriefTopologyDTO that = (BriefTopologyDTO) o;
        return biz.equals(that.biz);
    }

    @Override
    public int hashCode() {
        return Objects.hash(biz);
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static
    class Biz {
        @JsonProperty("id")
        private Long bizId;
        // 业务名称
        @JsonProperty("nm")
        private String bizName;
        // 业务类型，该值>=0，0: 表示该业务为普通业务。1: 表示该业务为资源池业务
        @JsonProperty("dft")
        private int bizType;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static
    class Node {
        @JsonProperty("obj")
        private String obj;
        @JsonProperty("id")
        private int id;
        @JsonProperty("nm")
        private String name;
        // 该值>=0，只有set和module有该字段，0:表示普通的集群或者模块，>1:表示为空闲机类的set或module。
        @JsonProperty("dft")
        private int dft;
        @JsonProperty("nds")
        private List<Node> childNodeList;
    }

}

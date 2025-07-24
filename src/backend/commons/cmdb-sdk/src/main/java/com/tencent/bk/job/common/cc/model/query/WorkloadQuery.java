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

package com.tencent.bk.job.common.cc.model.query;

import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * workload 查询
 */
@Getter
@ToString
public class WorkloadQuery {
    private final Long bizId;

    private final List<Long> ids;

    private final List<String> names;

    private final List<Long> bkClusterIds;

    private final List<Long> bkNamespaceIds;

    private final String kind;

    private WorkloadQuery(Builder builder) {
        bizId = builder.bizId;
        ids = builder.ids;
        names = builder.names;
        bkClusterIds = builder.bkClusterIds;
        bkNamespaceIds = builder.bkNamespaceIds;
        kind = builder.kind;
    }


    public static final class Builder {
        private final Long bizId;
        private List<Long> ids;
        private List<String> names;
        private List<Long> bkClusterIds;
        private List<Long> bkNamespaceIds;
        private final String kind;

        private Builder(long bizId, String kind) {
            this.bizId = bizId;
            this.kind = kind;
        }

        public static Builder builder(long bizId, String kind) {
            return new Builder(bizId, kind);
        }

        public Builder ids(List<Long> ids) {
            this.ids = ids;
            return this;
        }

        public Builder names(List<String> names) {
            this.names = names;
            return this;
        }

        public Builder bkClusterIds(List<Long> bkClusterIds) {
            this.bkClusterIds = bkClusterIds;
            return this;
        }

        public Builder bkNamespaceIds(List<Long> bkNamespaceIds) {
            this.bkNamespaceIds = bkNamespaceIds;
            return this;
        }

        public WorkloadQuery build() {
            return new WorkloadQuery(this);
        }
    }
}

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

package com.tencent.bk.job.common.cc.model.query;

import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * namespace 查询
 */
@Getter
@ToString
public class NamespaceQuery {
    private final Long bizId;

    private final List<Long> ids;

    private final List<String> names;

    private final List<Long> bkClusterIds;

    private NamespaceQuery(Builder builder) {
        bizId = builder.bizId;
        ids = builder.ids;
        names = builder.names;
        bkClusterIds = builder.bkClusterIds;
    }


    public static final class Builder {
        private final Long bizId;
        private List<Long> ids;
        private List<String> names;
        private List<Long> bkClusterIds;

        private Builder(long bizId) {
            this.bizId = bizId;
        }

        public static Builder builder(long bizId) {
            return new Builder(bizId);
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

        public NamespaceQuery build() {
            return new NamespaceQuery(this);
        }
    }
}

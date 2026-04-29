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

package com.tencent.bk.job.common.util;

import lombok.Getter;
import lombok.ToString;

/**
 * 三区分页查询配置。
 * <p>
 * 将数据按总量划分为三个区域，每个区域采用不同的分页策略：
 * <ul>
 *   <li>浅层区（Shallow）：数据量 ≤ shallowThreshold，使用 offset 完全并行分页</li>
 *   <li>中层区（Middle）：shallowThreshold < 数据量 ≤ deepThreshold，段间并行 + 段内串行 keyset 分页</li>
 *   <li>深层区（Deep）：数据量 > deepThreshold，单任务串行 keyset 分页</li>
 * </ul>
 */
@Getter
@ToString
public class TieredPageQueryConfig {

    /**
     * 每页大小
     */
    private final int pageSize;

    /**
     * 浅层区阈值（数据量 ≤ 该值时使用浅层区策略）
     */
    private final int shallowThreshold;

    /**
     * 深层区阈值（数据量 > 该值时使用深层区策略；shallowThreshold < 数据量 ≤ 该值时使用中层区策略）
     */
    private final int deepThreshold;

    /**
     * 浅层区并发线程数
     */
    private final int shallowConcurrency;

    /**
     * 中层区并发线程数
     */
    private final int middleConcurrency;

    /**
     * 中层区每个任务包含的页数
     */
    private final int middlePagesPerTask;

    private TieredPageQueryConfig(Builder builder) {
        this.pageSize = builder.pageSize;
        this.shallowThreshold = builder.shallowThreshold;
        this.deepThreshold = builder.deepThreshold;
        this.shallowConcurrency = builder.shallowConcurrency;
        this.middleConcurrency = builder.middleConcurrency;
        this.middlePagesPerTask = builder.middlePagesPerTask;
    }

    /**
     * 根据数据总量判断所属区域
     */
    public Tier getTier(int totalCount) {
        if (totalCount <= shallowThreshold) {
            return Tier.SHALLOW;
        } else if (totalCount <= deepThreshold) {
            return Tier.MIDDLE;
        } else {
            return Tier.DEEP;
        }
    }

    /**
     * 区域枚举
     */
    public enum Tier {
        /**
         * 浅层区：完全并行 offset 分页
         */
        SHALLOW,
        /**
         * 中层区：段间并行 + 段内串行 keyset 分页
         */
        MIDDLE,
        /**
         * 深层区：单任务串行 keyset 分页
         */
        DEEP
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer pageSize;
        private Integer shallowThreshold;
        private Integer deepThreshold;
        private Integer shallowConcurrency;
        private Integer middleConcurrency;
        private Integer middlePagesPerTask;

        public Builder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder shallowThreshold(int shallowThreshold) {
            this.shallowThreshold = shallowThreshold;
            return this;
        }

        public Builder deepThreshold(int deepThreshold) {
            this.deepThreshold = deepThreshold;
            return this;
        }

        public Builder shallowConcurrency(int shallowConcurrency) {
            this.shallowConcurrency = shallowConcurrency;
            return this;
        }

        public Builder middleConcurrency(int middleConcurrency) {
            this.middleConcurrency = middleConcurrency;
            return this;
        }

        public Builder middlePagesPerTask(int middlePagesPerTask) {
            this.middlePagesPerTask = middlePagesPerTask;
            return this;
        }

        public TieredPageQueryConfig build() {
            requirePositive(pageSize, "pageSize");
            requirePositive(shallowThreshold, "shallowThreshold");
            requirePositive(deepThreshold, "deepThreshold");
            requirePositive(shallowConcurrency, "shallowConcurrency");
            requirePositive(middleConcurrency, "middleConcurrency");
            requirePositive(middlePagesPerTask, "middlePagesPerTask");
            if (deepThreshold <= shallowThreshold) {
                throw new IllegalArgumentException("deepThreshold must be greater than shallowThreshold");
            }
            return new TieredPageQueryConfig(this);
        }

        private void requirePositive(Integer value, String name) {
            if (value == null) {
                throw new IllegalArgumentException(name + " must be set");
            }
            if (value <= 0) {
                throw new IllegalArgumentException(name + " must be positive");
            }
        }
    }
}

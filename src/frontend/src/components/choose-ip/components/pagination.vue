<!--
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
-->

<template>
    <div class="server-pagination">
        <div class="pagination-total">
            {{ $t('共计') }} {{ total }} {{ $t('条.total') }}
        </div>
        <div class="pagination-change">
            <div
                class="page-last"
                @click="handlePageChange(-1)">
                <Icon type="down-small" />
            </div>
            <input
                class="page-current"
                :value="current"
                @blur="handleInputSubmit"
                @change="handleInputSubmit">
            <div class="page-line">
                /
            </div>
            <div class="page-count">
                {{ totalPage }}
            </div>
            <div
                class="page-next"
                @click="handlePageChange(1)">
                <Icon type="down-small" />
            </div>
        </div>
    </div>
</template>
<script>
    export default {
        name: '',
        props: {
            pageSize: {
                type: Number,
                default: 10,
            },
            total: Number,
            page: {
                type: Number,
                default: 0,
            },
        },
        data () {
            return {
                current: 1,
            };
        },
        computed: {
            totalPage () {
                return Math.ceil(this.total / this.pageSize);
            },
        },
        watch: {
            page: {
                handler (newPage) {
                    this.current = newPage;
                },
                immediate: true,
            },
        },
        methods: {
            handleInput (event) {
                const $target = event.target;
                let value = parseInt($target.value, 10);
                if (!value) {
                    value = 1;
                }
                if (value < 1) {
                    value = 1;
                }
                if (value > this.totalPage) {
                    value = this.totalPage;
                }
                this.current = value;
                this.$nextTick(() => {
                    $target.value = value;
                });
            },
            handleInputSubmit (event) {
                const $target = event.target;
                let value = parseInt($target.value, 10);
                if (!value) {
                    value = 1;
                }
                if (value < 1) {
                    value = 1;
                }
                if (value > this.totalPage) {
                    value = this.totalPage;
                }
                this.current = value;
                this.$nextTick(() => {
                    $target.value = value;
                });
                if (this.current === this.page) {
                    return;
                }
                this.$emit('on-change', this.current);
            },
            handlePageChange (step) {
                const newPage = this.page + step;
                if (newPage < 1) {
                    return;
                }
                if (newPage > this.totalPage) {
                    return;
                }
                this.$emit('on-change', newPage);
            },
        },
    };
</script>
<style lang='postcss'>
    .server-pagination {
        display: flex;
        align-items: center;
        height: 64px;
        font-size: 12px;
        color: #63656e;

        .pagination-change {
            display: flex;
            align-items: center;
            margin-left: auto;
            user-select: none;

            .page-current,
            .page-count {
                height: 32px;
                padding: 0 14px;
                line-height: 32px;
            }

            .page-current {
                width: 50px;
                text-align: center;
                border: 1px solid #c4c6cc;
                border-radius: 2px;
                outline-color: #3a84ff;
            }

            .page-line {
                padding-left: 14px;
            }

            .page-last,
            .page-next {
                font-size: 24px;
                color: #c4c6cc;
                cursor: pointer;
            }

            .page-last {
                transform: rotateZ(90deg);
            }

            .page-next {
                transform: rotateZ(-90deg);
            }
        }
    }
</style>

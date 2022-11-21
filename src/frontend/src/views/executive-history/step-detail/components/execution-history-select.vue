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
    <div
        v-if="isNeedRender"
        ref="target"
        class="step-execution-history-select">
        <span>{{ retryCountText }}</span>
        <Icon
            style="font-size: 16px;"
            type="down-small" />
        <div
            ref="content"
            class="dropdown-menu">
            <div
                v-for="item in executionList"
                :key="item.retryCount"
                class="menu-item"
                :class="{
                    active: item.retryCount === retryCount,
                }"
                @click="handleSelectRetryCount(item.retryCount)">
                <div class="retry-count">
                    {{ item.text }}
                </div>
                <div class="time">
                    {{ item.createTime }}
                </div>
            </div>
        </div>
    </div>
</template>
<script>
    import _ from 'lodash';

    import TaskExecuteService from '@service/task-execute';

    import { ordinalSuffixOf } from '@utils/assist';

    export default {
        name: '',
        props: {
            stepInstanceId: {
                type: Number,
                required: true,
            },
            retryCount: {
                type: [Number, String],
                default: 0,
            },
            batch: {
                type: [Number, String],
                default: '',
            },
        },
        data () {
            return {
                isNeedRender: false,
                executionList: [],
            };
        },
        computed: {
            retryCountText () {
                // eslint-disable-next-line no-plusplus
                for (let i = 0; i < this.executionList.length; i++) {
                    if (this.executionList[i].retryCount === this.retryCount) {
                        return this.executionList[i].text;
                    }
                }
                return 'LATEST';
            },
        },
        watch: {
            stepInstanceId: {
                handler (stepInstanceId) {
                    this.popperDestroy();
                    if (!stepInstanceId) {
                        return;
                    }
                    this.fetchStepExecutionHistory();
                },
                immediate: true,
            },
            batch: {
                handler () {
                    if (!this.stepInstanceId) {
                        return;
                    }
                    this.fetchStepExecutionHistory('batch');
                },
                immediate: true,
            },
        },
        beforeDestroy () {
            this.popperDestroy();
        },
        methods: {
            /**
             * @desc 组件外部调用api，刷新数据
             */
            reLoading () {
                this.fetchStepExecutionHistory();
            },
            /**
             * @desc 获取步骤执行历史
             *
             * retryCount的最大值显示为LATEST
             * 如果重试次数大于0，显示retryCount切换列表
             */
            fetchStepExecutionHistory: _.debounce(function (from) {
                TaskExecuteService.fetchStepExecutionHistory({
                    stepInstanceId: this.stepInstanceId,
                    batch: this.batch,
                }).then((data) => {
                    const num = data.length;
                    const result = data.map((item, index) => {
                        const {
                            retryCount,
                            createTime,
                        } = item;

                        return {
                            retryCount,
                            createTime,
                            text: index === 0 ? 'LATEST' : ordinalSuffixOf(num - index),
                        };
                    });
                    this.executionList = Object.freeze(result);
                    
                    // 重试次数大于1才需要显示
                    this.isNeedRender = this.executionList.length > 1;

                    // 切换批次导致的数据刷新，需要获取最新重试次数
                    if (from === 'batch') {
                        const first = _.first(result);
                        this.$emit('on-change', first ? first.retryCount : 0);
                    }

                    if (this.isNeedRender) {
                        this.$nextTick(() => {
                            if (!this.popperInstance) {
                                this.popperInstance = this.$bkPopover(this.$el, {
                                    theme: 'light step-execution-history-menu',
                                    arrow: false,
                                    interactive: true,
                                    placement: 'bottom-start',
                                    content: this.$refs.content,
                                    animation: 'slide-toggle',
                                    trigger: 'click',
                                    width: '240px',
                                });
                            }
                        });
                    } else {
                        this.popperDestroy();
                    }
                });
            }, 100),
            /**
             * @desc 销毁popover实例
             */
            popperDestroy () {
                if (this.popperInstance) {
                    this.popperInstance.destroy();
                    this.popperInstance = null;
                }
            },
            /**
             * @desc 切换重试次数
             * @param retryCount [Number] 重试记录
             *
             * 切换成功后需要将retryCount的最新值更新到url上
             */
            handleSelectRetryCount (retryCount) {
                this.popperInstance && this.popperInstance.hide();
                this.$emit('on-change', retryCount);
                const searchParams = new URLSearchParams(window.location.search);
                searchParams.set('retryCount', retryCount);
                window.history.replaceState({}, '', `?${searchParams.toString()}`);
            },
        },
    };
</script>
<style lang='postcss'>
    .step-execution-history-select {
        position: relative;
        display: flex;
        height: 20px;
        padding-right: 2px;
        padding-left: 10px;
        margin-left: 8px;
        color: #63656e;
        cursor: pointer;
        background: #e6e7eb;
        border-radius: 2px;
        align-items: center;
    }

    .step-execution-history-menu-theme {
        .menu-item {
            display: flex;
            align-items: center;
            justify-content: space-between;
            height: 32px;
            padding: 0 10px;
            font-size: 12px;
            color: #979ba5;
            cursor: pointer;

            &:hover {
                background: #f4f6fa;
            }

            &.active {
                background: #eaf3ff;

                .retry-count {
                    color: #3a84ff;
                }
            }

            .retry-count {
                color: #63656e;
            }
        }

        .tippy-content {
            margin: 0 -0.6rem;
        }
    }
</style>

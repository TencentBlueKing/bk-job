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
        class="exection-status-bar"
        :class="[data.displayStyle]">
        <component
            :is="themeCom"
            :data="data"
            :title-max-width="titleMaxWidth">
            <slot />
        </component>
    </div>
</template>
<script>
    import Step from './step';
    import Task from './task';

    export default {
        name: '',
        components: {
            Task,
            Step,
        },
        props: {
            type: {
                type: String,
                required: true,
            },
            data: {
                type: Object,
                default: () => ({}),
            },
        },
        data () {
            return {
                titleMaxWidth: 100,
                offsetRight: 'unset',
                initialStatusWidth: 0,
            };
        },
        computed: {
            themeCom () {
                const comMap = {
                    task: Task,
                    step: Step,
                };
                if (!Object.prototype.hasOwnProperty.call(comMap, this.type)) {
                    return 'div';
                }
                return comMap[this.type];
            },
        },
        mounted () {
            const $container = document.querySelector('#sitePageTitle');
            const containerWidth = $container.getBoundingClientRect().width;
            const $target = document.querySelector('#siteHeaderStatusBar');

            $target.appendChild(this.$el);
            const statusWidth = $target.getBoundingClientRect().width;
            if (!this.initialStatusWidth) {
                this.initialStatusWidth = statusWidth;
            }
            
            const titleMaxWidth = containerWidth - statusWidth - 40;
            this.titleMaxWidth = titleMaxWidth < 100 ? 100 : titleMaxWidth;

            this.$once('hook:beforeDestroy', () => {
                if (!this.$el) {
                    return;
                }
                try {
                    $target.removeChild(this.$el);
                } catch {}
            });
        },
    };
</script>
<style lang='postcss'>
    .exection-status-bar {
        flex: 1;
        font-size: 14px;
        color: #63656e;
        white-space: nowrap;

        &.fail,
        &.confirm-forced {
            .status-text {
                color: #ea3636;
            }
        }

        &.loading {
            .status-text {
                color: #3a84ff;
            }
        }

        &.ingore {
            .status-text {
                color: #abd88a;
            }
        }

        &.success,
        &.forced {
            .status-text {
                color: #2dcb8d;
            }
        }

        &.confirm {
            .status-text {
                color: #ff9c01;
            }
        }

        &.disable {
            .status-text {
                color: #c4c6cc;
            }
        }

        .title {
            display: flex;
            align-items: center;
            margin-right: 54px;
            font-size: 12px;

            .title-text {
                height: 21px;
            }
        }

        .status-box {
            position: absolute;
            top: 0;
            bottom: 0;
            left: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            width: 500px;
            transform: translateX(-50%);
        }

        .status {
            margin-right: 30px;
        }

        .time {
            min-width: 120px;
            padding-right: 10px;

            .value {
                display: inline-block;
                color: #313238;
            }
        }

        .step-instance-action {
            margin-right: 0;
        }
    }
</style>

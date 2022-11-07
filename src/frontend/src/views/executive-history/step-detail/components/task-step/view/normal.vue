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
    <div class="task-execute-bar-step-description-popover">
        <div>
            {{ data.typeDesc }}:<span class="value">{{ data.name }}</span>
        </div>
        <div>
            {{ $t('history.步骤状态') }}:<span
                class="value"
                :class="[data.displayStyle]">{{ data.statusDesc }}</span>
        </div>
        <div>
            {{ $t('history.步骤耗时') }}:<span class="value">{{ data.totalTimeText }}</span>
        </div>
    </div>
</template>
<script>
    export default {
        name: '',
        props: {
            data: {
                type: Object,
                required: true,
            },
        },
    };
</script>
<style lang='postcss'>
    .task-execute-bar-step-description-popover {
        padding: 0.3rem 0.6rem;

        &::before {
            position: absolute;
            top: 50%;
            left: -6px;
            width: 10px;
            height: 10px;
            margin-top: -8px;
            background: #fff;
            border: none;
            border: 1px solid #dcdee5;
            border-right: none;
            border-bottom: none;
            content: "";
            transform: translateY(-50%) rotateZ(-45deg);
        }

        .value {
            margin-left: 1em;

            &.loading {
                color: #3a84ff;
            }

            &.success {
                color: #2dcb9d;
            }

            &.disabled {
                color: #c4c6cc;
            }

            &.fail,
            &.forced {
                color: #ff5656;
            }

            &.confirm {
                color: #ff9c01;
            }
        }
    }
</style>

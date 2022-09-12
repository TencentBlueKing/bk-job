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

<template functional>
    <div class="task-exection-status-bar">
        <div
            v-if="props.data.name"
            class="title">
            （<div class="title-text">
                {{ props.data.name }}
            </div>）
        </div>
        <div class="status-box">
            <div class="status">
                <span>{{ parent.$t('history.状态.execute') }}：</span>
                <span class="status-text">{{ props.data.statusDesc }}</span>
            </div>
            <div class="time">
                <span>{{ parent.$t('history.总耗时') }}：</span>
                <span
                    class="value"
                    :style="{
                        width: `${((props.data.totalTimeText && props.data.totalTimeText.length) || 1) * 9}px`,
                    }">
                    {{ props.data.totalTimeText }}
                </span>
            </div>
            <div class="action">
                <slot />
            </div>
        </div>
    </div>
</template>
<style lang='postcss'>
    .task-exection-status-bar {
        .step-instance-action {
            border: 1px solid #c4c6cc;

            &:hover {
                border-color: #ea3636;
            }
        }
    }
</style>

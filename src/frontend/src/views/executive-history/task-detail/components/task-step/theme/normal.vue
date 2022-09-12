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
        class="execution-step-box theme-normal"
        :class="[data.displayStyle]">
        <div class="step-icon">
            <Icon :type="data.icon" />
        </div>
        <div
            class="theme-normal-wraper"
            @click="choose">
            <div class="step-desc">
                <div class="name-text">
                    {{ data.name }}
                </div>
                <div
                    v-if="!data.isNotStart"
                    class="step-status-desc">
                    <span
                        v-if="data.totalTime"
                        class="time">{{ data.totalTimeText }}</span>
                    <Icon
                        v-if="['fail', 'forced'].includes(data.displayStyle)"
                        class="step-error-flag"
                        type="close" />
                </div>
            </div>
            <div
                v-if="!data.isNotStart"
                class="step-info">
                <detail-layout>
                    <detail-item :label="`${$t('history.步骤名称')}：`">
                        {{ data.name }}
                    </detail-item>
                    <detail-item :label="`${$t('history.开始时间.label')}：`">
                        {{ data.startTime }}
                    </detail-item>
                    <detail-item :label="`${$t('history.结束时间')}：`">
                        {{ data.endTime || '--' }}
                    </detail-item>
                </detail-layout>
            </div>
        </div>
        <Icon
            class="step-process"
            :class="data.lastStepIcon"
            svg
            :type="data.lastStepIcon" />
        <img
            v-if="data.isDoing"
            class="loading-progress"
            src="/static/images/task-loading.png">
        <div
            class="step-action"
            @click.stop="">
            <step-action
                v-for="action in data.actions"
                :key="action"
                :confirm-handler="operationCode => handleChangeStatus(operationCode)"
                :name="action" />
        </div>
    </div>
</template>
<script>
    import DetailLayout from '@components/detail-layout';
    import DetailItem from '@components/detail-layout/item';

    import StepAction from '../../../../common/step-action';

    export default {
        components: {
            DetailLayout,
            DetailItem,
            StepAction,
        },
        props: {
            data: {
                type: Object,
            },
            choose: {
                type: Function,
            },
            handleChangeStatus: {
                type: Function,
            },
        },
    };
</script>

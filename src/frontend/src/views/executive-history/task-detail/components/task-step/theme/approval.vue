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
        class="execution-step-box theme-approval"
        :class="[data.displayStyle]"
        @click="choose">
        <div class="step-desc">
            <div class="step-icon">
                <Icon :type="data.icon" />
            </div>
            <div>{{ data.name }}</div>
            <div class="confirm-flag">
                {{ $t('history.已确认') }}
            </div>
        </div>
        <div class="approval-container">
            <div class="approval-info">
                <div
                    v-if="data.roleNameList.length || data.userList.length"
                    class="approval-person">
                    <span class="persion-label">{{ $t('history.确认人') }}：</span>
                    <div
                        v-for="item in data.roleNameList"
                        :key="`role_${item}`"
                        class="person">
                        <Icon
                            class="role-flag"
                            type="user-group-gray" />
                        {{ item }}
                    </div>
                    <div
                        v-for="item in data.userList"
                        :key="`user_${item}`"
                        class="person">
                        <Icon
                            class="role-flag"
                            type="user" />
                        {{ item }}
                    </div>
                </div>
                <div
                    v-if="data.notifyChannelNameList.length > 0"
                    class="approval-channel">
                    {{ $t('history.通知方式') }}：<span>{{ data.notifyChannelNameList.join('，') }}</span>
                </div>
            </div>
            <div
                v-if="data.confirmMessage"
                class="step-message">
                {{ data.confirmMessage }}
            </div>
            <bk-input
                v-if="data.isApprovaling"
                v-model="confirmReason"
                class="confirm-reason"
                :maxlength="100"
                :placeholder="$t('history.可在此处输入确认或终止的因由')"
                :rows="3"
                type="textarea" />
            <div
                v-else-if="data.operator"
                class="confirm-reason-text">
                <div class="person">
                    {{ data.operator }}
                </div>
                <span v-html="data.confirmReasonHtml" />
            </div>
            <div
                class="step-action"
                @click.stop="">
                <step-action
                    v-for="action in data.actions"
                    :key="action"
                    :confirm-handler="operationCode => handleChangeStatus(operationCode, confirmReason)"
                    :name="action" />
            </div>
        </div>
        <Icon
            class="step-process"
            svg
            :type="data.lastStepIcon" />
    </div>
</template>
<script>
    import StepAction from '../../../../common/step-action';

    export default {
        components: {
            StepAction,
        },
        props: {
            data: {
                type: Object,
                required: true,
            },
            choose: {
                type: Function,
                default: () => {},
            },
            handleChangeStatus: {
                type: Function,
                default: () => {},
            },
        },
        data () {
            return {
                confirmReason: '',
            };
        },
    };
</script>

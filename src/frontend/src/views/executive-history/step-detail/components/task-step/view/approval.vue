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
        ref="popover"
        class="task-execute-bar-step-confirm-popover-view"
        :class="{
            [data.displayStyle]: true,
            'not-start': data.isNotStart,
        }">
        <div class="confirm-wraper">
            <div class="step-name">
                {{ data.name }}
            </div>
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
                    {{ $t('history.通知方式') }}：{{ data.notifyChannelNameList.join('，') }}
                </div>
            </div>
            <div
                v-if="data.confirmMessage"
                class="step-desc">
                {{ data.confirmMessage }}
            </div>
            <template v-if="!data.isNotStart">
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
                    v-if="data.actions.length > 0"
                    class="step-action"
                    @click.stop="">
                    <step-action
                        v-for="action in data.actions"
                        :key="action"
                        :confirm-handler="operationCode => handleChangeStatus(operationCode)"
                        :name="action" />
                </div>
            </template>
        </div>
        <Icon
            v-if="data.displayStyle !== 'success'"
            class="approval-close"
            type="close"
            @click="handleClose" />
    </div>
</template>
<script>
    import TaskExecuteService from '@service/task-execute';

    import StepAction from '../../../../common/step-action';

    import I18n from '@/i18n';

    export default {
        name: '',
        components: {
            StepAction,
        },
        props: {
            data: {
                type: Object,
                required: true,
            },
        },
        data () {
            return {
                confirmReason: '',
            };
        },

        methods: {
            handleChangeStatus (operationCode) {
                return TaskExecuteService.updateTaskExecutionStepOperate({
                    id: this.data.stepInstanceId,
                    operationCode,
                    confirmReason: this.confirmReason,
                }).then((data) => {
                    this.$bkMessage({
                        limit: 1,
                        theme: 'success',
                        message: I18n.t('history.操作成功'),
                    });
                    this.$emit('on-update', data);
                    return true;
                });
            },
            handleClose () {
                this.$emit('on-close');
            },
        },
    };
</script>
<style lang='postcss'>
    .task-execute-bar-step-confirm-popover-view {
        position: relative;
        width: 472px;
        background: #fff;

        &.not-start {
            &::before {
                background: #dcdee5;
                border-color: #dcdee5;
            }

            .confirm-wraper {
                border-left-color: #dcdee5;
            }
        }

        &.success {
            &::before {
                background: #2dcb9d;
                border-color: #2dcb9d;
            }

            .confirm-wraper {
                border-left-color: #2dcb9d;
            }
        }

        &.confirm-forced {
            &::before {
                background: #ff5656;
                border-color: #ff5656;
            }

            .confirm-wraper {
                border-left-color: #ff5656;
            }
        }

        &.middle {
            &::before {
                top: 50%;
                transform: translateY(-50%) rotateZ(-45deg);
            }
        }

        &.top {
            &::before {
                top: 10px;
                transform: rotateZ(-45deg);
            }
        }

        &.bottom {
            &::before {
                bottom: 20px;
                transform: rotateZ(-45deg);
            }
        }

        &::before {
            position: absolute;
            left: -6px;
            width: 10px;
            height: 10px;
            background: #ffb848;
            border: 1px solid #dcdee5;
            border-color: #ffb848;
            content: "";
        }

        .confirm-wraper {
            position: relative;
            padding: 20px 36px 24px 30px;
            background: #fff;
            border-left: 6px solid #ffb848;
        }

        .step-name {
            font-size: 16px;
            font-weight: bold;
            line-height: 21px;
            color: #313238;
        }

        .approval-info {
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            padding-top: 4px;
            font-size: 12px;
            line-height: 20px;
            color: #b2b5bd;
        }

        .approval-person {
            display: flex;
            flex-wrap: wrap;

            .persion-label,
            .person {
                margin-top: 10px;
            }

            .role-flag {
                margin-right: 4px;
            }
        }

        .person {
            display: inline-flex;
            height: 22px;
            padding: 0 6px;
            margin-right: 6px;
            font-size: 12px;
            color: #63656e;
            background: #f0f1f5;
            border-radius: 2px;
            align-items: center;
        }

        .approval-channel {
            margin-top: 10px;
        }

        .confirm-reason,
        .confirm-reason-text {
            margin-top: 20px;
        }

        .confirm-reason-text {
            font-size: 14px;
            line-height: 22px;
            color: #63656e;
        }

        .step-desc {
            padding-top: 14px;
            white-space: normal;
        }

        .step-action {
            display: flex;
            justify-content: flex-end;
            margin-top: 20px;

            .step-instance-action {
                width: 76px;
                height: 26px;
                padding: 0;
                margin-right: 0;
                margin-left: 10px;
                font-size: 12px;
                border-radius: 2px;

                &.confirm {
                    color: #fff;
                    background: #3a84ff;
                }

                &.confirm-forced {
                    color: #63656e !important;
                    background: #fff;
                    border: 1px solid #c4c6cc;
                }

                &.retry {
                    width: auto;
                    padding: 0 14px;
                    color: #fff;
                    background: #3a84ff;
                }

                .job-icon {
                    display: none;
                }
            }
        }

        .approval-close {
            position: absolute;
            top: 0;
            right: 0;
            padding: 10px;
            font-size: 24px;
            color: #c4c6cc;
            cursor: pointer;
        }
    }
</style>

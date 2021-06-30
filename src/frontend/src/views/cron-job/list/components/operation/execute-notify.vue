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
    <div class="execute-notify-box" v-bkloading="{ isLoading }">
        <jb-form-item :label="notifyOffsetLabel" layout="inline" :rules="rules.notifyOffset">
            <bk-select :value="formData.notifyOffset" class="time-select" @change="handleNotifyOffsetChange">
                <bk-option :id="10" :name="`10 ${$t('cron.分钟')}`">10 {{ $t('cron.分钟') }}</bk-option>
                <bk-option :id="30" :name="`30 ${$t('cron.分钟')}`">30 {{ $t('cron.分钟') }}</bk-option>
                <bk-option :id="45" :name="`45 ${$t('cron.分钟')}`">45 {{ $t('cron.分钟') }}</bk-option>
                <bk-option :id="60" :name="`1 ${$t('cron.小时')}`">1 {{ $t('cron.小时') }}</bk-option>
            </bk-select>
        </jb-form-item>
        <jb-form-item :label="$t('cron.通知对象')" layout="inline">
            <jb-user-selector
                :placeholder="$t('cron.输入通知对象')"
                class="input"
                :user="formData.notifyUser.userList"
                :role="formData.notifyUser.roleList"
                :filter-list="['JOB_EXTRA_OBSERVER']"
                @on-change="handleApprovalUserChange" />
        </jb-form-item>
        <jb-form-item :label="$t('cron.通知方式')" style="margin-bottom: 0;" layout="inline">
            <div class="notify-channel-wraper">
                <bk-checkbox
                    @click.native="handleToggleAllChannel"
                    :checked="isChannelAll"
                    :indeterminate="isChannelIndeterminate">
                    {{ $t('cron.全部') }}
                </bk-checkbox>
                <bk-checkbox-group
                    :value="formData.notifyChannel"
                    class="all-channel"
                    @change="handleNotifyChannelChange">
                    <bk-checkbox
                        v-for="channel in channleList"
                        :key="channel.code"
                        :value="channel.code">
                        {{ channel.name }}
                    </bk-checkbox>
                </bk-checkbox-group>
            </div>
        </jb-form-item>
    </div>
</template>
<script>
    import I18n from '@/i18n';
    import QueryGlobalSettingService from '@service/query-global-setting';
    import JbUserSelector from '@components/jb-user-selector';

    export default {
        name: '',
        components: {
            JbUserSelector,
        },
        props: {
            notifyOffsetLabel: {
                type: String,
                required: true,
            },
            formData: {
                type: Object,
                required: true,
            },
            mode: {
                type: String,
                default: 'finish-before', // execute-beofre / finish-before
            },
        },
        data () {
            return {
                isLoading: false,
                channleList: [],
            };
        },
        computed: {
            isChannelAll () {
                if (this.channleList.length < 1) {
                    return false;
                }
                return this.formData.notifyChannel.length === this.channleList.length;
            },
            isChannelIndeterminate () {
                if (this.formData.notifyChannel.length < 1) {
                    return false;
                }
                return this.formData.notifyChannel.length !== this.channleList.length;
            },
        },
        created () {
            this.fetchAllChannel();
            this.$emit('on-change', {
                notifyOffset: 10,
            });
            this.rules = {
                notifyOffset: [],
            };
            if (this.mode === 'execute-beofre') {
                this.rules.notifyOffset = [
                    {
                        // 执行时间 - 执行前通知的时间 > 当前时间
                        validator: () => new Date(this.formData.executeTime).getTime()
                            - parseInt(this.formData.notifyOffset, 10) * 6000 > Date.now(),
                        message: I18n.t('cron.设置的提醒时间已过期'),
                        trigger: 'change',
                    },
                ];
            }
        },
        beforeDestroy () {
            this.$emit('on-change', {
                notifyOffset: 0,
                notifyUser: {
                    userList: [],
                    roleList: [],
                },
                notifyChannel: [],
            });
        },
        methods: {
            fetchAllChannel () {
                QueryGlobalSettingService.fetchActiveNotifyChannel()
                    .then((data) => {
                        this.channleList = data;
                    })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            handleToggleAllChannel () {
                if (this.isChannelAll) {
                    this.handleNotifyChannelChange([]);
                } else {
                    this.handleNotifyChannelChange(this.channleList.map(_ => _.code));
                }
            },
            handleNotifyOffsetChange (value) {
                this.$emit('on-change', {
                    notifyOffset: value,
                });
            },
            handleApprovalUserChange (userList, roleList) {
                this.$emit('on-change', {
                    notifyUser: {
                        userList,
                        roleList,
                    },
                });
            },
            handleNotifyChannelChange (value) {
                this.$emit('on-change', {
                    notifyChannel: value,
                });
            },
        },
    };
</script>
<style lang='postcss'>
    html[lang='en-US'] .notify-channel-wraper {
        .bk-form-checkbox {
            width: 64px;
        }
    }

    .execute-notify-box {
        display: flex;
        flex-direction: column;

        .row {
            display: flex;
            margin-bottom: 20px;

            &:last-child {
                margin-bottom: 0;
            }

            .label {
                font-size: 14px;
                color: #63656e;
                text-align: left;
                flex: 0 0 80px;
            }
        }

        .time-select {
            width: 100%;
            background: #fff;
        }

        .notify-channel-wraper {
            display: flex;
            align-items: flex-start;
            min-height: 32px;
            white-space: nowrap;

            .all-channel {
                display: flex;
                flex-wrap: wrap;
            }
        }

        .bk-form-checkbox {
            display: flex;
            flex: 0 0 auto;
            margin-top: 6px;
            margin-right: 40px;
        }

        .bk-checkbox {
            background: #fff;
        }
    }
</style>

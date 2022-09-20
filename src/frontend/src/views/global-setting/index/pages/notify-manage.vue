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
        v-bkloading="{ isLoading }"
        class="notify-set-manange">
        <jb-form
            v-if="!isLoading"
            class="wraper"
            :model="formData">
            <div class="block-title">
                {{ $t('setting.用户可选择的通知渠道') }}：
            </div>
            <notify-channel
                :channel-code="formData.channelCode"
                :channle-list="channleList"
                :handle-edit-template="handleEditTemplate"
                :handle-toggle-channel="handleToggleChannel" />
            <div class="backlist block-title">
                <span v-bk-tooltips="backlistConfig">{{ $t('setting.通讯黑名单') }}:</span>
            </div>
            <div>
                <jb-user-selector
                    :placeholder="$t('setting.请输入')"
                    :show-role="false"
                    :user="formData.users"
                    @on-change="handleBlackListChange" />
            </div>
            <div class="action-box">
                <bk-button
                    class="w120 mr10"
                    :loading="isSubmiting"
                    theme="primary"
                    @click="handleSave">
                    {{ $t('setting.保存') }}
                </bk-button>
                <bk-button @click="handleReset">
                    {{ $t('setting.重置') }}
                </bk-button>
            </div>
        </jb-form>
        <jb-sideslider
            :is-show.sync="showTemplateEdit"
            :title="$t('setting.消息模板编辑')"
            :width="680">
            <edit-of-template
                ref="editTemplate"
                :data="templateDetail"
                @on-change="handleNotifyContent" />
            <template #footer>
                <bk-button
                    class="slider-action"
                    :loading="isSaveLoading"
                    theme="primary"
                    @click="handleTriggerSave">
                    {{ $t('setting.保存') }}
                </bk-button>
                <bk-button
                    class="slider-action"
                    @click="handleTriggerReset">
                    {{ $t('setting.重置') }}
                </bk-button>
                <bk-button
                    class="slider-action"
                    @click="handleTriggerInit">
                    {{ $t('setting.初始化') }}
                </bk-button>
            </template>
        </jb-sideslider>
    </div>
</template>
<script>
    import _ from 'lodash';

    import GlobalSettingService from '@service/global-setting';

    import JbSideslider from '@components/jb-sideslider';
    import JbUserSelector from '@components/jb-user-selector';

    import editOfTemplate from '../components/edit-of-template';
    import NotifyChannel from '../components/notify-channel-table';

    import I18n from '@/i18n';

    export default {
        name: 'NotifyManage',
        components: {
            JbUserSelector,
            NotifyChannel,
            JbSideslider,
            editOfTemplate,
        },
        data () {
            return {
                isLoading: true,
                isSubmiting: false,
                isSaveLoading: false,
                showTemplateEdit: false,
                formData: {
                    channelCode: [],
                    users: [],
                },
                templateDetail: {},
                currentTemplate: {},
                currentDefaultTemplate: {},
                channleList: [],
                channelCode: [],
                users: [],
            };
        },
        created () {
            this.backlistConfig = {
                width: 202,
                placement: 'top',
                content: I18n.t('setting.「通讯黑名单」的人员将不会接收到任何来自作业平台的消息'),
            };
            this.fetchAllChannelConfig();
            this.fetchAllUserBlacklist();
            this.channelCodeMemo = [];
            this.usersMemo = [];
        },
        methods: {
            fetchAllChannelConfig () {
                GlobalSettingService.fetchAllNotifyChannelConfig({}, {
                    permission: 'page',
                }).then((data) => {
                    this.channleList = data;
                    this.formData.channelCode = data.reduce((result, item) => {
                        if (item.isActive) {
                            result.push(item.code);
                        }
                        return result;
                    }, []);
                    this.channelCodeMemo = [...this.channelCode];
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            fetchAllUserBlacklist () {
                GlobalSettingService.fetchAllUserBlacklist({}, {
                    permission: 'page',
                }).then((data) => {
                    this.formData.users = data.map(item => item.username);
                    this.usersMemo = [...this.users];
                });
            },
            handleBlackListChange (user, role) {
                this.formData.users = user;
            },
            handleToggleChannel (code) {
                const index = this.formData.channelCode.indexOf(code);
                if (index > -1) {
                    this.formData.channelCode.splice(index, 1);
                } else {
                    this.formData.channelCode.push(code);
                }
            },
            handleEditTemplate (channel, template) {
                GlobalSettingService.fetchChannelTemplate({
                    channelCode: channel,
                    messageTypeCode: template,
                }).then((data) => {
                    const { currentTemplate, defaultTemplate } = data;
                    this.templateDetail = _.cloneDeep(currentTemplate || defaultTemplate);
                    this.currentTemplate = _.cloneDeep(currentTemplate || defaultTemplate);
                    this.currentDefaultTemplate = _.cloneDeep(defaultTemplate);
                })
                    .finally(() => {
                        this.showTemplateEdit = true;
                    });
            },
            handleSave () {
                this.isSubmiting = true;
                Promise.all([
                    GlobalSettingService.updateNotifyChannel({
                        channelCodeStr: this.formData.channelCode.join(','),
                    }).catch(() => {
                        this.messageError(I18n.t('setting.保存通知渠道失败'));
                    }),
                    GlobalSettingService.updateUserBlacklist({
                        usersStr: this.formData.users.join(','),
                    }).catch(() => {
                        this.messageError(I18n.t('setting.保存黑名单失败'));
                    }),
                ]).then(() => {
                    window.changeConfirm = false;
                    this.messageSuccess(I18n.t('setting.保存成功'));
                })
                    .finally(() => {
                        this.isSubmiting = false;
                    });
            },
            handleReset () {
                this.formData.channelCode = [...this.channelCodeMemo];
                this.formData.users = [...this.usersMemo];
            },
            handleNotifyContent (value, filed) {
                this.templateDetail[filed] = value;
            },
            handleTriggerSave () {
                this.$refs.editTemplate.$refs.templateForm.validate().then(() => {
                    const { code, messageTypeCode, content, title } = this.templateDetail;
                    const params = {
                        channelCode: code,
                        messageTypeCode,
                        content,
                        title,
                    };
                    GlobalSettingService.updateNotifyTemplate(params)
                        .then((data) => {
                            this.showTemplateEdit = false;
                            this.messageSuccess(I18n.t('setting.保存成功'));
                        })
                        .finally(() => {
                            this.isSaveLoading = false;
                        });
                });
            },
            handleTriggerReset () {
                this.templateDetail = _.cloneDeep(this.currentTemplate);
            },
            handleTriggerInit () {
                this.templateDetail = _.cloneDeep(this.currentDefaultTemplate);
            },
        },
    };
</script>
<style lang='postcss'>
    .notify-set-manange {
        display: flex;
        justify-content: center;
        padding-top: 40px;
        padding-bottom: 40px;

        .wraper {
            width: 960px;
        }

        .block-title {
            margin-bottom: 12px;
        }

        .backlist {
            margin-top: 38px;

            span {
                border-bottom: 1px dashed #c4c6cc;
            }
        }

        .action-box {
            margin-top: 30px;
        }
    }

    .member-item {
        &.disabled {
            color: #c4c6cc;
        }

        .job-icon-user {
            margin-right: 8px;
        }
    }

    .slider-action {
        margin-right: 10px;
    }
</style>

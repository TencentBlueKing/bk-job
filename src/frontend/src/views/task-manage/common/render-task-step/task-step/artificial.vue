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
    <div>
        <jb-form ref="form" :model="formData" :rules="rules" fixed :label-width="110">
            <item-factory
                name="stepName"
                field="name"
                :placeholder="$t('template.推荐按步骤实际处理的场景行为来取名...')"
                :form-data="formData"
                @on-change="handleNameChange" />
            <jb-form-item :label="$t('template.确认人')" :required="true" property="approvalUser">
                <jb-user-selector
                    :placeholder="$t('template.输入确认人')"
                    class="input"
                    :user="formData.approvalUser.userList"
                    :role="formData.approvalUser.roleList"
                    :filter-list="['JOB_EXTRA_OBSERVER']"
                    @on-change="handleApprovalUserChange" />
            </jb-form-item>
            <jb-form-item :label="$t('template.通知方式')">
                <div class="notify-channel-wraper">
                    <bk-checkbox
                        @click.native="handleToggleAllChannel"
                        :checked="isChannelAll"
                        :indeterminate="isChannelIndeterminate">
                        {{ $t('template.全部') }}
                    </bk-checkbox>
                    <bk-checkbox-group v-model="formData.notifyChannel" class="all-channel">
                        <bk-checkbox
                            v-for="channel in channleList"
                            :key="channel.code"
                            :value="channel.code">
                            {{ channel.name }}
                        </bk-checkbox>
                    </bk-checkbox-group>
                </div>
            </jb-form-item>
            <jb-form-item :label="$t('template.确认描述')">
                <bk-input class="input" type="textarea" v-model="formData.approvalMessage" :maxlength="1000" />
            </jb-form-item>
        </jb-form>
    </div>
</template>
<script>
    import _ from 'lodash';
    import I18n from '@/i18n';
    import QueryGlobalSettingService from '@service/query-global-setting';
    import JbUserSelector from '@components/jb-user-selector';
    import ItemFactory from '@components/task-step/file/item-factory';
    import {
        getScriptName,
    } from '@utils/assist';

    const getDefaultData = () => ({
        id: 0,
        // 步骤名称
        name: getScriptName(I18n.t('template.步骤人工确认')),
        // 删除标记
        delete: 0,
        // 审批消息
        approvalMessage: '',
        // 审批类型 暂未启用 1-任意人审批 2-所有人审批
        approvalType: 1,
        // 审批人
        approvalUser: {
            roleList: [
                'JOB_RESOURCE_TRIGGER_USER',
            ],
            userList: [],
        },
        notifyChannel: [],
    });
    export default {
        name: '',
        components: {
            JbUserSelector,
            ItemFactory,
        },
        inheritAttrs: false,
        props: {
            data: {
                type: Object,
                default: () => ({}),
            },
        },
        data () {
            return {
                formData: getDefaultData(),
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
        watch: {
            data: {
                handler (newData) {
                    if (_.isEmpty(newData)) {
                        return;
                    }
                    this.formData = {
                        ...this.formData,
                        ...newData,
                    };
                    setTimeout(() => {
                        this.$refs.form.validate();
                    });
                },
                immediate: true,
            },
        },
        created () {
            this.rules = {
                approvalUser: [
                    {
                        validator: approvalUser => approvalUser.roleList.length + approvalUser.userList.length > 0,
                        message: I18n.t('template.确认人必填'),
                        trigger: 'blur',
                    },
                ],
            };
            this.fetchAllChannel();
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
                    this.formData.notifyChannel = [];
                } else {
                    this.formData.notifyChannel = this.channleList.map(_ => _.code);
                }
            },
            handleNameChange (field, name) {
                this.formData[field] = name.trim();
            },
            
            handleApprovalUserChange (user, role) {
                this.formData.approvalUser.roleList = role;
                this.formData.approvalUser.userList = user;
            },
            submit () {
                const {
                    name,
                    id,
                    approvalMessage,
                    approvalType,
                    approvalUser,
                    notifyChannel,
                } = this.formData;

                const result = {
                    id,
                    name,
                    delete: this.formData.delete,
                    type: 3,
                    approvalStepInfo: {
                        approvalMessage,
                        approvalType,
                        approvalUser,
                        notifyChannel,
                    },
                };

                return this.$refs.form.validate()
                    .then(() => {
                        this.$emit('on-change', result, true);
                    }, () => {
                        this.$emit('on-change', result, false);
                    });
            },
        },
    };
</script>
<style lang='postcss' scoped>
    .notify-channel-wraper {
        display: flex;
        align-items: center;
        height: 32px;
        white-space: nowrap;

        .bk-form-checkbox {
            display: flex;
            flex: 0 0 auto;
            margin-right: 40px;
        }

        .all-channel {
            display: flex;
        }
    }

    .input {
        width: 495px;
    }
</style>

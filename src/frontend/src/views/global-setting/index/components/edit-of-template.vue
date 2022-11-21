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
        <div class="detail-layout-wrapper">
            <div class="channel-detail-layout">
                <div class="layout-left">
                    <div class="detail-item">
                        <label>{{ $t('setting.渠道类型：') }}</label>
                        <span>{{ formData.name }}</span>
                    </div>
                    <div class="detail-item">
                        <label>{{ $t('setting.消息类型：') }}</label>
                        <span>{{ formData.messageTypeLabel }}</span>
                    </div>
                </div>
                <div class="layout-right">
                    <div class="detail-item">
                        <label>{{ $t('setting.最近修改人：') }}</label>
                        <span>{{ formData.lastModifier }}</span>
                    </div>
                    <div class="detail-item">
                        <label>{{ $t('setting.最近修改时间：') }}</label>
                        <span>{{ formData.lastModifyTime }}</span>
                    </div>
                </div>
            </div>
        </div>
        <jb-form
            ref="templateForm"
            class="notify-template-form"
            form-type="vertical"
            :model="formData"
            :rules="rules">
            <jb-form-item
                v-if="formData.code === 'mail'"
                :label="$t('setting.邮件主题')"
                :property="'title'"
                required>
                <bk-input
                    v-model="formData.title"
                    @change="value => handleChange(value, 'title')" />
            </jb-form-item>
            <jb-form-item
                :label="$t('setting.模板内容')"
                :property="'content'"
                required>
                <bk-button
                    text
                    @click="toggleShowVariable(true)">
                    {{ $t('setting.内置变量') }}
                </bk-button>
                <bk-input
                    v-model="formData.content"
                    type="textarea"
                    @change="value => handleChange(value, 'content')" />
            </jb-form-item>
        </jb-form>
        <div class="message-preview">
            <bk-button
                text
                @click="isShowPreviewSend = !isShowPreviewSend">
                {{ $t('setting.消息预览') }}
            </bk-button>
            <render-strategy
                v-if="isShowPreviewSend"
                left="20">
                <div class="send-message-content">
                    <jb-user-selector
                        class="input"
                        :placeholder="$t('setting.请输入接收消息预览的用户名（请确保接受人对应的账号配置正常）')"
                        :show-role="false"
                        :user="reciverList"
                        @on-change="handleApprovalUserChange" />
                    <bk-button
                        :disabled="!reciverList.length"
                        :loading="isLoading"
                        theme="primary"
                        @click="handleSend">
                        {{ $t('setting.发送') }}
                    </bk-button>
                </div>
            </render-strategy>
        </div>
        <jb-dialog
            v-model="isShowVariable"
            class="internal-variable-dialog"
            :show-footer="false"
            :width="960">
            <internalVariable :handle-close="toggleShowVariable" />
        </jb-dialog>
    </div>
</template>

<script>
    import GlobalSettingService from '@service/global-setting';

    import JbUserSelector from '@components/jb-user-selector';
    import RenderStrategy from '@components/render-strategy';

    import InternalVariable from './internal-variable';

    import I18n from '@/i18n';

    const getDefaultData = () => ({
        // 渠道code
        code: '',
        // 消息模板正文
        content: '',
        // 最近修改人
        lastModifier: '',
        // 最近修改时间
        lastModifyTime: '',
        // 消息类型
        messageTypeCode: '',
        // 消息类型名称
        messageTypeLabel: '',
        // 渠道名称
        name: '',
        // 消息模板标题
        title: '',
    });

    export default {
        components: {
            RenderStrategy,
            InternalVariable,
            JbUserSelector,
        },
        props: {
            data: {
                type: Object,
                default: () => ({}),
            },
        },
        data () {
            return {
                isShowPreviewSend: false,
                isShowVariable: false,
                isLoading: false,
                reciverList: [],
                formData: getDefaultData(),
            };
        },
        watch: {
            data (newVal) {
                if (newVal) this.formData = newVal;
            },
        },
        created () {
            this.formData = this.data || {};
            this.rules = {
                content: [
                    { required: true, message: I18n.t('setting.模板内容必填'), trigger: 'blur' },
                ],
            };
            if (this.formData.code === 'mail') {
                this.rules.title = [
                    { required: true, message: I18n.t('setting.邮件主题必填'), trigger: 'blur' },
                ];
            }
        },
        methods: {
            toggleShowVariable (isShow) {
                this.isShowVariable = isShow;
            },
            handleChange (value) {
                this.$emit('on-change', value);
            },
            handleApprovalUserChange (user, role) {
                this.reciverList = user;
            },
            handleSend () {
                const { code, messageTypeCode, content, title } = this.formData;
                const params = {
                    channelCode: code,
                    messageTypeCode,
                    content,
                    title,
                    receiverStr: this.reciverList.join(','),
                };
                this.isLoading = true;
                GlobalSettingService.sendNotifyPreview(params)
                    .then((data) => {
                        this.messageSuccess(I18n.t('setting.发送成功'));
                    })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
        },
    };
</script>

<style lang="postcss">
    .channel-detail-layout {
        display: flex;

        .layout-left,
        .layout-right {
            flex: 1;
        }

        .detail-item {
            margin-bottom: 20px;
            font-size: 14px;

            label {
                color: #b2b5bd;
                word-break: keep-all;
                white-space: pre;
            }

            span {
                color: #63656e;
            }
        }

        .detail-label {
            justify-content: flex-start;
        }
    }

    .notify-template-form {
        position: relative;
        margin-top: 20px;

        .bk-form-textarea {
            max-height: 400px;
            min-height: 280px;
        }

        .bk-button-text {
            position: absolute;
            top: -32px;
            right: 0;
        }
    }

    .message-preview {
        margin-bottom: 20px;
    }

    .send-message-content {
        display: flex;

        .bk-button {
            margin-left: 10px;
        }
    }

    .internal-variable-dialog {
        .bk-dialog-tool {
            display: none;
        }
    }
</style>

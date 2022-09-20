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
        class="page-platform-info">
        <smart-action offset-target="bk-form-content">
            <div class="wraper">
                <jb-form
                    ref="platformForm"
                    :model="formData"
                    :rules="rules">
                    <bk-popover
                        :distance="-10"
                        placement="top"
                        theme="light"
                        :width="320">
                        <div class="backlist block-title">
                            <span>{{ $t('setting.网页 Title 设置:') }}</span>
                        </div>
                        <div
                            slot="content"
                            class="title-example-popover">
                            <img
                                class="example-image"
                                src="/static/images/title-example.png">
                        </div>
                    </bk-popover>
                    <hgroup>
                        <jb-form-item
                            :label="$t('setting.平台名称')"
                            property="titleHead"
                            required>
                            <jb-input
                                v-model="formData.titleHead"
                                style="width: 240px;" />
                        </jb-form-item>
                        <jb-form-item
                            class="title-separator-item"
                            :label="$t('setting.分隔符')"
                            property="titleSeparator"
                            required>
                            <jb-input
                                v-model="formData.titleSeparator"
                                style="width: 240px;" />
                        </jb-form-item>
                        <bk-button
                            class="reset"
                            size="small"
                            text
                            @click="handleRestore">
                            {{ $t('setting.恢复默认') }}
                        </bk-button>
                    </hgroup>
                    <div class="block-title">
                        <span>{{ $t('setting.页脚信息设置') }}:</span>
                    </div>
                    <jb-form-item :label="$t('setting.联系方式')">
                        <jb-input
                            v-model="formData.footerLink"
                            style="width: 680px;" />
                    </jb-form-item>
                    <jb-form-item :label="$t('setting.版权信息')">
                        <jb-input
                            v-model="formData.footerCopyRight" />
                    </jb-form-item>
                </jb-form>
            </div>
            <template #action>
                <bk-button
                    class="w120 mr10"
                    :loading="isSubmitting"
                    theme="primary"
                    @click="handleSave">
                    {{ $t('setting.保存') }}
                </bk-button>
                <bk-button @click="handleReset">
                    {{ $t('setting.重置') }}
                </bk-button>
            </template>
        </smart-action>
    </div>
</template>
<script>
    import _ from 'lodash';

    import GlobalSettingService from '@service/global-setting';

    import JbInput from '@components/jb-input';
    import SmartAction from '@components/smart-action';

    import I18n from '@/i18n';

    const getDefaultData = () => ({
        titleHead: '',
        titleSeparator: '',
        footerLink: '',
        footerCopyRight: '',
    });

    export default {
        name: '',
        components: {
            SmartAction,
            JbInput,
        },
        data () {
            return {
                isLoading: false,
                isSubmitting: false,
                formData: getDefaultData(),
                currentTitleFooter: {},
                defaultTitleFooter: {},
            };
        },
        created () {
            this.fetchTitleAndFooter();
            this.rules = {
                titleHead: [
                    {
                        required: true,
                        message: I18n.t('setting.平台名称必填'),
                        trigger: 'blur',
                    },
                ],
                titleSeparator: [
                    {
                        required: true,
                        message: I18n.t('setting.分隔符必填'),
                        trigger: 'blur',
                    },
                ],
            };
        },
        methods: {
            fetchTitleAndFooter () {
                this.isLoading = true;
                GlobalSettingService.fetchTitleAndFooterConfig()
                    .then((data) => {
                        this.defaultTitleFooter = _.cloneDeep(data.defaultTitleFooter);
                        this.currentTitleFooter = _.cloneDeep(data.currentTitleFooter);
                        this.formData = { ...this.formData, ...data.currentTitleFooter };
                    })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            handleRestore () {
                this.formData = _.cloneDeep(this.defaultTitleFooter);
            },
            handleReset () {
                this.formData = _.cloneDeep(this.currentTitleFooter);
            },
            handleSave () {
                this.$refs.platformForm.validate()
                    .then((validator) => {
                        this.isSubmitting = true;
                        GlobalSettingService.updateTitleAndFooterConfig(this.formData)
                            .then(() => {
                                window.changeConfirm = false;
                                this.messageSuccess(I18n.t('setting.保存成功'));
                            })
                            .finally(() => {
                                this.isSubmitting = false;
                            });
                    });
            },
        },
    };
</script>
<style lang='postcss'>
    .page-platform-info {
        display: flex;
        justify-content: center;
        padding-top: 10px;
        padding-bottom: 40px;

        .wraper {
            margin-bottom: 10px;
        }

        .backlist {
            margin-top: 38px;

            span {
                border-bottom: 1px dashed #c4c6cc;
            }
        }

        .block-title {
            padding-bottom: 6px;
            margin-bottom: 12px;
        }

        .action-box {
            margin-top: 30px;
        }

        hgroup {
            display: flex;
            justify-content: flex-start;
            align-items: self-start;
            margin-bottom: 20px;

            .bk-label {
                text-align: left;
            }

            .bk-form-item {
                margin-top: 0;
                margin-bottom: 10px;
            }

            .reset {
                margin-top: 4px;
                margin-left: 2px;
            }

            .title-separator-item {
                .bk-label {
                    text-align: right;
                }
            }
        }
    }

    .title-example-popover {
        padding: 20px 10px;
        text-align: center;

        .example-image {
            width: 270px;
            height: 100px;
        }
    }
</style>

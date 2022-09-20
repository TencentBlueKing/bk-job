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
    <div class="export-task-step3-page">
        <div class="form">
            <jb-form
                ref="form"
                :model="formData"
                :rules="rules">
                <jb-form-item
                    :label="$t('template.压缩包名')"
                    property="packageName"
                    required>
                    <bk-input
                        v-model="formData.packageName"
                        :maxlength="40"
                        :placeholder="$t('template.压缩包名仅支持大小写英文、数字、- 或 _')" />
                </jb-form-item>
                <jb-form-item
                    :label="$t('template.密文变量值处理')"
                    property="secretHandler"
                    required>
                    <bk-select
                        v-model="formData.secretHandler"
                        :clearable="false">
                        <bk-option
                            :id="1"
                            :name="$t('template.保存为空值')" />
                        <bk-option
                            :id="2"
                            :name="$t('template.保存真实值')" />
                    </bk-select>
                </jb-form-item>
                <jb-form-item
                    :label="$t('template.文件加密')"
                    property="isEncrypt"
                    required>
                    <div class="encrypt-wraper">
                        <bk-radio-group v-model="formData.isEncrypt">
                            <bk-radio :value="1">
                                {{ $t('template.是') }}
                            </bk-radio>
                            <bk-radio :value="2">
                                {{ $t('template.否') }}
                            </bk-radio>
                        </bk-radio-group>
                    </div>
                </jb-form-item>
                <div v-show="isPasswordRequired">
                    <jb-form-item
                        :label="$t('template.密码设置')"
                        property="password"
                        :required="isPasswordRequired">
                        <bk-input
                            v-model="formData.password"
                            v-bk-tooltips="htmlConfig"
                            :placeholder="$t('template.请设置6-20个字符密码')"
                            type="password" />
                    </jb-form-item>
                    <jb-form-item
                        :label="$t('template.二次确认')"
                        property="confirmPassword"
                        :required="isPasswordRequired">
                        <bk-input
                            v-model="formData.confirmPassword"
                            :placeholder="$t('template.请输入同样的密码，以确认密码准确')"
                            type="password" />
                    </jb-form-item>
                </div>
                <jb-form-item
                    :label="$t('template.文件有效期')"
                    property="expireTime"
                    required>
                    <bk-radio-group
                        v-model="formData.expireTime"
                        class="expire-time-box">
                        <bk-radio-button
                            v-for="item in expireTimeList"
                            :key="item.id"
                            :value="item.id">
                            {{ item.name }}
                        </bk-radio-button>
                    </bk-radio-group>
                </jb-form-item>
                <jb-form-item
                    v-if="formData.expireTime === 'custom'"
                    class="expire-time-custom"
                    label=" "
                    property="customNum">
                    <bk-input
                        v-model="formData.customNum"
                        :placeholder="$t('template.请输入整数，不可超过365')">
                        <template slot="append">
                            <div class="group-text">
                                <span class="text">{{ $t('template.天') }}</span>
                            </div>
                        </template>
                    </bk-input>
                </jb-form-item>
            </jb-form>
        </div>
        <div
            id="html-config-password"
            class="html-config-password">
            <div
                class="item"
                :class="{ active: passwordLengthResult }">
                {{ $t('template.长度6-20个字符') }}
            </div>
            <div
                class="item"
                :class="{ active: passwordFormatResult }">
                {{ $t('template.必须包含英文字母、数字和特殊符号') }}
            </div>
        </div>
        <div class="action-footer">
            <bk-button
                class="mr10"
                @click="handleCancel">
                {{ $t('template.取消') }}
            </bk-button>
            <bk-button
                class="mr10"
                @click="handleLast">
                {{ $t('template.上一步') }}
            </bk-button>
            <bk-button
                class="w120"
                :loading="isSubmiting"
                theme="primary"
                @click="handleNext">
                {{ $t('template.下一步') }}
            </bk-button>
        </div>
    </div>
</template>
<script>
    import BackupService from '@service/backup';

    import { genDefaultName } from '@utils/assist';
    import { taskExport } from '@utils/cache-helper';

    import I18n from '@/i18n';

    export default {
        data () {
            this.rules = {};
            return {
                isSubmiting: false,
                formData: {
                    packageName: genDefaultName(`bk_job_export_${window.PROJECT_CONFIG.SCOPE_TYPE}_${window.PROJECT_CONFIG.SCOPE_ID}`).slice(0, 40),
                    secretHandler: 1,
                    isEncrypt: 2,
                    password: '',
                    confirmPassword: '',
                    expireTime: 0,
                    customNum: null,
                },
            };
        },
        computed: {
            isPasswordRequired () {
                return this.formData.isEncrypt === 1;
            },
            passwordLengthResult () {
                return this.formData.password.length >= 6 && this.formData.password.length <= 20;
            },
            passwordFormatResult () {
                return !/^[A-Za-z0-9]*$/.test(this.formData.password);
            },
        },
        watch: {
            /**
             * @desc 文件加密时需要验证密码
             */
            'formData.isEncrypt': {
                handler (isEncrypt) {
                    const passwordRule = [
                        {
                            validator: (val) => {
                                if (!val) {
                                    return true;
                                }
                                if (val.length < 6 || val.length > 20) {
                                    return false;
                                }
                                return !/^[a-zA-Z0-9]*$/.test(val);
                            },
                            message: I18n.t('template.密码长度为6-20个字符，必须包含英文字母、数字和特殊符号'),
                            trigger: 'blur',
                        },
                    ];
                    const confirmPasswordRule = [
                        {
                            validator: val => this.formData.password === val,
                            message: I18n.t('template.两次输入的密码不一致'),
                            trigger: 'blur',
                        },
                    ];
                    if (isEncrypt === 1) {
                        passwordRule.unshift({
                            required: true,
                            message: I18n.t('template.密码必填'),
                            trigger: 'blur',
                        });
                        confirmPasswordRule.unshift({
                            required: true,
                            message: I18n.t('template.确认密码必填'),
                            trigger: 'blur',
                        });
                    }
                    if (this.$refs.form) {
                        this.$refs.form.clearError('password');
                        this.$refs.form.clearError('confirmPassword');
                    }
                    this.formData.password = '';
                    this.formData.confirmPassword = '';
                    this.rules.password = passwordRule;
                    this.rules.confirmPassword = confirmPasswordRule;
                },
                immediate: true,
            },
            /**
             * @desc 自定义文件有效期时对输入框的值进行验证
             */
            'formData.expireTime' () {
                if (this.formData.expireTime === 'custom') {
                    this.rules.customNum = [
                        {
                            required: true,
                            message: I18n.t('template.文件有效期必填'),
                            trigger: 'blur',
                        },
                        {
                            validator: val => val >= 1 && val <= 365,
                            message: I18n.t('template.文件有效期须大于1或不超过365'),
                            trigger: 'blur',
                        },
                    ];
                } else {
                    delete this.rules.customNum;
                }
            },
        },
        created () {
            this.expireTimeList = [
                { id: 0, name: I18n.t('template.永久') },
                { id: 1, name: I18n.t('template.1 天') },
                { id: 3, name: I18n.t('template.3 天') },
                { id: 7, name: I18n.t('template.7 天') },
                { id: 'custom', name: I18n.t('template.自定义') },
            ];
            this.htmlConfig = {
                allowHtml: true,
                width: 250,
                trigger: 'click',
                theme: 'light',
                content: '#html-config-password',
                placement: 'right-start',
            };
            this.rules = Object.assign(this.rules, {
                packageName: [
                    {
                        required: true,
                        message: I18n.t('template.压缩包名必填'),
                        trigger: 'blur',
                    },
                    {
                        validator: val => /^[A-Za-z0-9_-]*$/.test(val),
                        message: I18n.t('template.压缩包名仅支持大小写英文、数字、- 或 _'),
                        trigger: 'blur',
                    },
                ],
                secretHandler: [
                    {
                        required: true,
                        message: I18n.t('template.密文变量值处理必填'),
                        trigger: 'blur',
                    },
                ],
                isEncrypt: [
                    {
                        required: true,
                        message: I18n.t('template.文件加密必填'),
                        trigger: 'blur',
                    },
                ],
                expireTime: [
                    {
                        required: true,
                        message: I18n.t('template.文件有效期必填'),
                        trigger: 'blur',
                    },
                ],
            });
        },
        methods: {
            handleCancel () {
                this.$emit('on-cancle');
            },
            handleLast () {
                this.$emit('on-change', 2);
            },
            handleNext () {
                const templateInfo = taskExport.getItem('templateInfo');
                if (!templateInfo) {
                    return;
                }
                this.isSubmiting = true;
                this.$refs.form.validate()
                    .then(() => {
                        const { packageName, password, secretHandler, expireTime, customNum } = this.formData;
                        return BackupService.export({
                            packageName,
                            password,
                            secretHandler,
                            expireTime: expireTime === 'custom' ? parseInt(customNum, 10) : expireTime,
                            templateInfo,
                        }).then((data) => {
                            window.changeConfirm = false;
                            taskExport.setItem('id', data.id);
                            this.$emit('on-change', 4);
                        });
                    })
                    .finally(() => {
                        this.isSubmiting = false;
                    });
            },
        },
    };
</script>
<style lang="postcss">
    .export-task-step3-page {
        .form {
            display: flex;
            justify-content: center;
            padding-top: 60px;

            .encrypt-wraper {
                display: flex;
                align-items: center;
                height: 100%;
            }

            .expire-time-box {
                .bk-radio-button-text {
                    width: 93px;
                }
            }

            .expire-time-custom {
                position: relative;
                right: 0;
            }
        }

        .bk-form-radio {
            margin-right: 28px;
        }

        .group-text {
            position: relative;
            width: 87px;
            overflow: unset !important;
            text-align: center;

            &::before {
                position: absolute;
                top: 0;
                right: 0;
                bottom: 0;
                left: 0;
                z-index: 1;
                background: #f5f7fa;
                content: "";
            }

            &::after {
                position: absolute;
                top: -5px;
                left: 50%;
                width: 11px;
                height: 11px;
                background: #f5f7fa;
                border-top: 1px solid #c4c6cc;
                border-right: 1px solid #c4c6cc;
                content: "";
                transform: translateX(-50%) rotateZ(-45deg);
            }

            .text {
                position: relative;
                z-index: 1;
            }
        }
    }

    .html-config-password {
        .item {
            &::before {
                display: inline-block;
                width: 8px;
                height: 8px;
                margin-right: 10px;
                background-color: #ccc;
                border-radius: 50%;
                content: "";
            }

            &.active {
                &::before {
                    background-color: #3fc06d;
                }
            }
        }
    }
</style>

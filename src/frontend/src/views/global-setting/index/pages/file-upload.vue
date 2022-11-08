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
        class="file-upload-manage-page"
        v-bkloading="{ isLoading }">
        <smart-action offset-target="input-wraper">
            <jb-form style="width: 480px; margin-bottom: 20px;">
                <div class="block-title">
                    {{ $t('setting.本地文件上传大小限制') }}:
                </div>
                <jb-form-item>
                    <div class="input-wraper">
                        <bk-input
                            v-model="info.amount"
                            type="number"
                            :min="1" />
                        <bk-select
                            v-model="info.unit"
                            class="unit-item"
                            :clearable="false">
                            <bk-option id="GB" name="GB" />
                            <bk-option id="MB" name="MB" />
                        </bk-select>
                    </div>
                </jb-form-item>
                <div class="block-title">
                    {{ $t('setting.本地文件上传后缀限制') }}:
                </div>
                <jb-form-item style="margin-bottom: 10px;">
                    <bk-radio-group
                        v-model="info.restrictMode"
                        class="restrict-mode-radio"
                        @change="handleRestSuffixError">
                        <bk-radio-button :value="-1">
                            {{ $t('setting.不限制') }}
                        </bk-radio-button>
                        <bk-radio-button :value="1">
                            {{ $t('setting.设置允许范围') }}
                        </bk-radio-button>
                        <bk-radio-button :value="0">
                            {{ $t('setting.设置禁止范围') }}
                        </bk-radio-button>
                    </bk-radio-group>
                </jb-form-item>
                <div v-if="info.restrictMode > -1">
                    <jb-form-item>
                        <bk-tag-input
                            v-model="info.suffixList"
                            allow-create
                            has-delete-icon
                            :key="info.restrictMode"
                            @change="handleRestSuffixError" />
                    </jb-form-item>
                    <div class="form-item-error" v-html="suffixError" />
                </div>
            </jb-form>
            <template #action>
                <bk-button
                    class="w120"
                    theme="primary"
                    :loading="isSubmiting"
                    @click="handleSubmit">
                    {{ $t('setting.保存') }}
                </bk-button>
            </template>
        </smart-action>
    </div>
</template>
<script>
    import GlobalSettingService from '@service/global-setting';
    import I18n from '@/i18n';

    const checkSuffixError = (suffixList) => {
        if (suffixList.length < 1) {
            return I18n.t('setting.不允许为空');
        }
        const errorStack = [];
        const renameStack = [];
        const lengthStack = [];
        const ruleMap = [];
        suffixList.forEach((rule) => {
            // . 开头，后面跟上不超过24个英文字符
            if (rule.length > 25) {
                lengthStack.push(rule);
                return;
            }
            const realRule = rule.toLowerCase();
            // . 开头，中间不允许出现空格
            if (!/^\.[a-zA-Z0-9]+(.[a-zA-Z0-9_-]+)*$/.test(realRule)) {
                errorStack.push(realRule);
                return;
            }
            // 大小写不敏感
            if (!ruleMap[realRule]) {
                ruleMap[realRule] = [];
            } else {
                renameStack.push(ruleMap[realRule]);
            }
            ruleMap[realRule].push(rule);
        });
        let suffixError = '';
        if (lengthStack.length > 0) {
            suffixError += `${lengthStack.join(',')};`;
        }
        if (errorStack.length > 0) {
            suffixError += `${errorStack.join(',')};`;
        }
        if (renameStack.length > 0) {
            const renameError = renameStack.reduce((result, item) => {
                result.push(item.join(','));
                return result;
            }, []).join('；');
            suffixError += renameError;
        }
        return suffixError ? `${I18n.t('setting..开头，后面跟上数字、字母、横杠(-)、下划线(_)：')}${suffixError}` : '';
    };

    export default {
        data () {
            return {
                isLoading: true,
                isSubmiting: false,
                info: {
                    amount: 0,
                    unit: '',
                    restrictMode: -1,
                    suffixList: [],
                },
                suffixError: '',
            };
        },
        created () {
            this.fetchJobConfig();
        },
        methods: {
            /**
             * @desc 获取配置信息
             */
            fetchJobConfig () {
                this.isLoading = true;
                GlobalSettingService.fetchFileUpload()
                    .then((data) => {
                        this.info = data;
                        if (!data.suffixList || data.suffixList.length < 1) {
                            this.info = {
                                ...this.info,
                                restrictMode: -1,
                                suffixList: [],
                            };
                        }
                    })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            handleRestSuffixError () {
                this.suffixError = '';
            },
            /**
             * @desc 提交修改
             */
            handleSubmit () {
                const params = { ...this.info };

                this.suffixError = '';
                if (params.restrictMode === -1) {
                    params.suffixList = [];
                } else {
                    this.suffixError = checkSuffixError(params.suffixList);
                }
                
                if (this.suffixError) {
                    return;
                }

                this.isSubmiting = true;
                GlobalSettingService.saveFileUpload(params)
                    .then(() => {
                        this.messageSuccess(I18n.t('setting.保存成功'));
                    })
                    .finally(() => {
                        this.isSubmiting = false;
                    });
            },
        },
    };
</script>
<style lang='postcss'>
    .file-upload-manage-page {
        display: flex;
        justify-content: center;
        padding: 40px 0;

        .input-wraper {
            display: flex;
        }

        .unit-item {
            width: 80px;
            margin-left: -2px;
            background: #f5f7fa;
            border-bottom-left-radius: 0;
            border-top-left-radius: 0;
        }

        .restrict-mode-radio {
            display: flex;

            .bk-form-radio-button {
                flex: 1;

                .bk-radio-button-text {
                    width: 100%;
                }
            }
        }

        .form-item-error {
            margin: -18px 0 0;
            font-size: 12px;
            line-height: 18px;
            color: #ea3636;
        }
    }
</style>

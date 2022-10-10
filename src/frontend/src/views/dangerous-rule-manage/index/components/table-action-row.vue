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
    <tbody>
        <tr v-if="!isEdit">
            <td colspan="5">
                <bk-button
                    text
                    @click="handleToggle">
                    <Icon type="plus" />
                    {{ $t('dangerousRule.新增检测规则') }}
                </bk-button>
            </td>
        </tr>
        <tr v-else>
            <td>
                <bk-input
                    v-model="formData.expression"
                    class="input" />
            </td>
            <td>
                <bk-input
                    v-model="formData.description"
                    class="input" />
            </td>
            <td>
                <bk-select
                    v-model="formData.scriptTypeList"
                    :clearable="false"
                    multiple
                    show-select-all>
                    <bk-option
                        v-for="item in scriptTypeList"
                        :id="item.id"
                        :key="item.id"
                        :name="item.name" />
                </bk-select>
            </td>
            <td>
                <bk-select
                    v-model="formData.action"
                    :clearable="false">
                    <bk-option
                        :id="1"
                        :name="$t('dangerousRule.扫描')" />
                    <bk-option
                        :id="2"
                        :name="$t('dangerousRule.拦截')" />
                </bk-select>
            </td>
            <td>
                <bk-button
                    text
                    @click="handleSubmit">
                    {{ $t('dangerousRule.保存') }}
                </bk-button>
                <bk-button
                    text
                    @click="handleCancel">
                    {{ $t('dangerousRule.取消') }}
                </bk-button>
            </td>
        </tr>
    </tbody>
</template>
<script>
    import DangerousRuleService from '@service/dangerous-rule';
    import PublicScriptManageService from '@service/public-script-manage';

    import I18n from '@/i18n';

    const generatorDefautlData = () => ({
        expression: '',
        description: '',
        scriptTypeList: 1,
        action: 1,
    });

    export default {
        data () {
            return {
                isEdit: false,
                isSubmiting: false,
                formData: generatorDefautlData(),
                scriptTypeList: [],
            };
        },
        created () {
            this.fetchScriptType();
        },
        methods: {
            /**
             * @desc 获取脚本类型列表
             */
            fetchScriptType () {
                PublicScriptManageService.scriptTypeList()
                    .then((data) => {
                        this.scriptTypeList = data;
                    });
            },
            /**
             * @desc 切换编辑状态
             */
            handleToggle () {
                this.isEdit = true;
            },
            /**
             * @desc 取消编辑状态，重置表单数据
             */
            handleCancel () {
                this.isEdit = false;
                this.formData = generatorDefautlData();
            },
            /**
             * @desc 提交用户数据
             */
            handleSubmit () {
                if (this.isSubmiting) {
                    return;
                }
                if (!this.formData.expression || !this.formData.description) {
                    this.messageError(I18n.t('dangerousRule.请填写完整的语法检测表达式和说明'));
                    return;
                }
                this.isSubmiting = true;
                DangerousRuleService.update({
                    id: -1,
                    ...this.formData,
                }).then(() => {
                    this.messageSuccess(I18n.t('dangerousRule.新增成功'));
                    this.$emit('on-change');
                    this.handleCancel();
                })
                    .finally(() => {
                        this.isSubmiting = false;
                    });
            },
        },
    };
</script>

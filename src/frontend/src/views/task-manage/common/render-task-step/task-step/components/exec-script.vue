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
    <jb-form ref="form" :model="formData" fixed :label-width="110">
        <item-factory
            name="scriptName"
            field="name"
            :placeholder="$t('template.推荐按步骤实际处理的场景行为来取名...')"
            :form-data="formData"
            @on-change="handleChange" />
        <item-factory
            name="scriptSourceOfTemplate"
            script-source-field="scriptSource"
            content-field="content"
            language-field="scriptLanguage"
            script-id-field="scriptId"
            script-version-id-field="scriptVersionId"
            script-status-field="status"
            :form-data="formData"
            @on-change="handleChange"
            @on-reset="handleScriptContentReset" />
        <item-factory
            name="scriptContent"
            script-source-field="scriptSource"
            content-field="content"
            language-field="scriptLanguage"
            :form-data="formData"
            :script-variables="scriptVariables"
            @on-change="handleChange" />
        <item-factory
            name="scriptParam"
            param-field="scriptParam"
            secure-field="secureParam"
            :form-data="formData"
            @on-change="handleChange" />
        <item-factory
            name="scriptTimeout"
            field="timeout"
            :form-data="formData"
            @on-change="handleChange" />
        <item-factory
            name="errorHandle"
            field="ignoreError"
            :form-data="formData"
            @on-change="handleChange" />
        <item-factory
            name="scriptAccount"
            field="account"
            script-language-field="scriptLanguage"
            :form-data="formData"
            @on-change="handleChange" />
        <item-factory
            name="executeTargetOfTemplate"
            field="executeTarget"
            :variable="variable"
            :form-data="formData"
            @on-change="handleChange" />
    </jb-form>
</template>
<script>
    import TaskStepModel from '@model/task/task-step';
    import TaskHostNodeModel from '@model/task-host-node';
    import {
        scriptErrorConfirm,
    } from '@utils/assist';
    import ItemFactory from '@components/task-step/script/item-factory';

    const getDefaultData = () => ({
        isScriptContentLoading: false,
        // 脚本步骤名称
        name: '',
        // 错误处理
        ignoreError: 0,
        // 脚本步骤的id
        id: -1,
        // 删除标记
        delete: 0,
        // 需要升级
        status: 0,
        // 脚本来源类型 1-本地脚本 2-引用脚本 3-引用公共脚本
        scriptSource: TaskStepModel.scriptStep.TYPE_SOURCE_LOCAL,
        // 引用的脚本 ID
        scriptId: '',
        // 引用脚本的版本 ID
        scriptVersionId: '',
        // 脚本内容的类型语言，默认shell
        scriptLanguage: 1,
        // 脚本内容
        content: '',
        // 脚本参数
        scriptParam: '',
        // 超时时间
        timeout: 7200,
        // 敏感参数 （0-关闭 1-开启）
        secureParam: 0,
        // 执行账号
        account: '',
        // 执行目标信息 （主机和全局变量二选一）
        executeTarget: new TaskHostNodeModel({}),

    });

    export default {
        name: '',
        components: {
            ItemFactory,
        },
        inheritAttrs: false,
        props: {
            data: {
                type: Object,
                default: () => [],
            },
            variable: {
                type: Array,
                default: () => [],
            },
            scriptVariables: {
                type: Array,
                default: () => [],
            },
            stepName: {
                type: Array,
                default: () => [],
            },
        },
        data () {
            return {
                formData: getDefaultData(),
            };
        },
        watch: {
            data: {
                handler (newData) {
                    // 本地新建的步骤id为-1，已提交后端保存的id大于0
                    this.formData = Object.assign({}, this.formData, newData);
                    // 有数据需要自动验证一次
                    if (newData.id) {
                        setTimeout(() => {
                            this.$refs.form.validate();
                        });
                    }
                },
                immediate: true,
            },
        },
        mounted () {
            window.IPInputScope = 'SCRIPT_EXECUTE';
            this.$once('hook:beforeDestroy', () => {
                window.IPInputScope = '';
            });
        },
        methods: {
            handleChange (field, value) {
                this.formData[field] = value;
            },
            handleScriptContentReset (payload) {
                this.formData = {
                    ...this.formData,
                    ...payload,
                };
            },
            submit () {
                const {
                    name,
                    id,
                    ignoreError,
                    scriptParam,
                    timeout,
                    secureParam,
                    scriptSource,
                    scriptId,
                    content,
                    account,
                    status,
                    scriptVersionId,
                    scriptLanguage,
                    executeTarget,
                } = this.formData;

                const result = {
                    id,
                    name,
                    delete: this.formData.delete,
                    type: 1,
                    scriptStepInfo: {
                        ignoreError,
                        scriptParam,
                        timeout,
                        scriptSource,
                        scriptId,
                        account,
                        content,
                        scriptLanguage,
                        status,
                        scriptVersionId,
                        executeTarget,
                        secureParam,
                    },
                };

                return this.$refs.form.validate()
                    .then(() => true, () => false)
                    .then(validate => scriptErrorConfirm().then(() => {
                        this.$emit('on-change', result, validate);
                    }));
            },
        },
    };
</script>

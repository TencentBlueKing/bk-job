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
    <jb-form-item
        ref="item"
        :label="$t('脚本内容')"
        :property="contentField"
        required
        :rules="rules">
        <ace-editor
            ref="aceEditor"
            v-bkloading="{ isLoading: isContentLoading, opacity: .2 }"
            :constants="scriptVariables"
            :lang="lang"
            :readonly="isReadonly"
            :readonly-tips="$t('引用的脚本不支持编辑')"
            :value="formData[contentField]"
            @change="handleChange"
            @on-mode-change="handleTypeChange" />
    </jb-form-item>
</template>
<script>
    import _ from 'lodash';

    import ScriptManageService from '@service/script-manage';

    import TaskStepModel from '@model/task/task-step';

    import AceEditor from '@components/ace-editor';

    import I18n from '@/i18n';
    import {
        formatScriptTypeValue,
    } from '@/utils/assist';

    export default {
        components: {
            AceEditor,
        },
        props: {
            contentField: {
                type: String,
                required: true,
            },
            scriptSourceField: {
                type: String,
                required: true,
            },
            languageField: {
                type: String,
                required: true,
            },
            formData: {
                type: Object,
                default: () => ({}),
            },
            scriptVariables: {
                type: Array,
                default: () => [],
            },
        },
        data () {
            return {
                lang: 'Shell',
                isReadonly: false,
            };
        },
        computed: {
            isContentLoading () {
                return this.formData.isScriptContentLoading;
            },
        },
        watch: {
            formData: {
                handler (newData) {
                    this.isReadonly = newData[this.scriptSourceField] !== TaskStepModel.scriptStep.TYPE_SOURCE_LOCAL;
                    this.lang = formatScriptTypeValue(newData[this.languageField]);
                    if (this.isReadonly) {
                        this.rules = [];
                    } else {
                        this.rules = [
                            {
                                required: true,
                                message: I18n.t('脚本内容必填'),
                                trigger: 'change',
                            },
                            {
                                validator: value => ScriptManageService.getScriptValidation({
                                    content: value,
                                    scriptType: newData[this.languageField],
                                }).then((data) => {
                                    // 高危语句报错状态需要全局保存
                                    const dangerousContent = _.find(data, _ => _.isDangerous);
                                    this.$store.commit('setScriptCheckError', dangerousContent);
                                    return true;
                                }),
                                message: I18n.t('脚本内容检测失败'),
                                trigger: 'blur',
                            },
                        ];
                    }
                },
                deep: true,
                immediate: true,
            },
            'formData.content' (value) {
                if (value) {
                    this.$refs.item.clearValidator();
                }
            },
        },
        created () {
            this.rules = [];
        },
        methods: {
            handleTypeChange (lang) {
                this.$emit('on-change', this.languageField, formatScriptTypeValue(lang));
            },
            handleChange (value) {
                this.$emit('on-change', this.contentField, value);
            },
        },
    };
</script>

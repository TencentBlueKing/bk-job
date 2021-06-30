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
    <layout :title="$t('script.新建脚本')" class="copy-create-script-box" offset-target="bk-form-content" v-bind="$attrs">
        <jb-form ref="form" class="create-script-form" :model="formData" :rules="rules">
            <jb-form-item :label="$t('script.版本号.label')" required property="version">
                <div class="script-version input">
                    <jb-input
                        :value="formData.version"
                        :placeholder="$t('script.输入版本号')"
                        :maxlength="30"
                        property="version"
                        @change="handleVersionChange" />
                    <Icon type="new" class="new-flag" />
                </div>
            </jb-form-item>
            <jb-form-item :label="$t('script.版本日志')">
                <bk-input class="input" v-model="formData.versionDesc" type="textarea" :maxlength="100" />
            </jb-form-item>
            <jb-form-item :label="$t('script.脚本内容')" required property="content" style="margin-bottom: 0;">
                <div ref="content">
                    <ace-editor
                        v-if="contentHeight > 0"
                        v-model="formData.content"
                        :lang="formData.typeName"
                        :height="contentHeight"
                        :options="[formData.typeName]"
                        @on-mode-change="handleTypeChange" />
                </div>
            </jb-form-item>
        </jb-form>
        <template #footer>
            <bk-button
                class="w120 mr10"
                :loading="isSubmiting"
                theme="primary"
                @click.stop.prevent="handleSubmit">
                {{ $t('script.提交') }}
            </bk-button>
            <bk-button class="mr10" @click="handleDebugScript">调试</bk-button>
            <bk-button @click="handleCancel">{{ $t('script.取消') }}</bk-button>
        </template>
    </layout>
</template>
<script>
    import _ from 'lodash';
    import I18n from '@/i18n';
    import ScriptService from '@service/script-manage';
    import PublicScriptService from '@service/public-script-manage';
    import JbInput from '@components/jb-input';
    import AceEditor from '@components/ace-editor';
    import {
        formatScriptTypeValue,
        getScriptVersion,
        checkPublicScript,
        getOffset,
        leaveConfirm,
        scriptErrorAlert,
    } from '@utils/assist';
    import {
        debugScriptCache,
    } from '@utils/cache-helper';
    import {
        scriptVersionRule,
    } from '@utils/validator';
    import Layout from './layout';

    const genDefaultFormData = () => ({
        id: '',
        typeName: 'Shell',
        version: getScriptVersion(),
        versionDesc: '',
        type: 1,
        content: '',
    });

    export default {
        name: '',
        components: {
            JbInput,
            AceEditor,
            Layout,
        },
        props: {
            scriptInfo: {
                type: Object,
                required: true,
            },
            scriptVersionList: {
                type: Array,
                default: () => [],
            },
            scriptVersionId: {
                type: Number,
            },
        },
        data () {
            return {
                isLoading: true,
                isSubmiting: false,
                formData: genDefaultFormData(),
                contentHeight: 0,
            };
        },
        computed: {
            versionMap () {
                return this.scriptVersionList.reduce((result, item) => {
                    if (item.scriptVersionId < 1) {
                        return result;
                    }
                    result[item.version] = true;
                    return result;
                }, {});
            },
        },
        watch: {
            scriptInfo: {
                handler (scriptInfo) {
                    if (!scriptInfo.id) {
                        return;
                    }
                    const { content, id, name, versionDesc, type, typeName } = scriptInfo;
                    this.formData = {
                        ...genDefaultFormData(),
                        id,
                        name,
                        versionDesc,
                        type,
                        typeName,
                        content,
                    };
                },
                immediate: true,
            },
        },
        created () {
            this.publicScript = checkPublicScript(this.$route);
            this.scriptServiceHandler = this.publicScript ? PublicScriptService : ScriptService;
            this.rules = {
                version: [
                    {
                        required: true,
                        message: I18n.t('script.版本号必填'),
                        trigger: 'blur',
                    },
                    {
                        validator: scriptVersionRule.validator,
                        message: scriptVersionRule.message,
                        trigger: 'blur',
                    },
                    {
                        validator: value => !this.versionMap[value],
                        message: I18n.t('script.版本号已存在，请重新输入'),
                        trigger: 'blur',
                    },
                ],
                content: [
                    {
                        required: true,
                        message: I18n.t('script.脚本内容不能为空'),
                        trigger: 'change',
                    },
                    {
                        validator: value => ScriptService.getScriptValidation({
                            content: value,
                            scriptType: this.formData.type,
                        }).then((data) => {
                            // 高危语句报错状态需要全局保存
                            this.$store.commit('setScriptCheckError', data.some(_ => _.isDangerous));
                            return true;
                        }),
                        message: I18n.t('script.脚本内容检测失败'),
                        trigger: 'blur',
                    },
                ],
            };
        },
        mounted () {
            this.init();
            window.addEventListener('resize', this.init);
            this.$once('hook:beforeDestroy', () => {
                window.removeEventListener('resize', this.init);
            });
        },
        methods: {
            /**
             * @desc 计算内容去高度
             */
            init () {
                const contentOffsetTop = getOffset(this.$refs.content).top;
                const contentHeight = window.innerHeight - contentOffsetTop - 100;
                this.contentHeight = contentHeight < 480 ? 480 : contentHeight;
            },
            /**
             * @desc 脚本版本修改
             */
            handleVersionChange: _.debounce(function (value) {
                this.formData.version = value;
                const { scriptVersionId } = this.scriptInfo;
                this.$emit('on-create-change', scriptVersionId, {
                    version: value,
                });
            }, 100),
            /**
             * @desc 脚本语言
             * @param {String} scriptType 脚本语言
             */
            handleTypeChange (scriptType) {
                this.formData.type = formatScriptTypeValue(scriptType);
            },
            /**
             * @desc 保存脚本
             */
            handleSubmit () {
                this.isSubmiting = true;
                this.$refs.form.validate()
                    .then(() => {
                        if (this.$store.state.scriptCheckError) {
                            scriptErrorAlert();
                            return;
                        }
                        return this.scriptServiceHandler.scriptUpdate({
                            ...this.formData,
                        }).then((data) => {
                            this.$emit('on-create', {
                                scriptVersionId: data.scriptVersionId,
                            });
                            window.changeAlert = false;
                            this.messageSuccess(I18n.t('script.操作成功'));
                        });
                    })
                    .finally(() => {
                        this.isSubmiting = false;
                    });
            },
            /**
             * @desc 跳转到快速执行脚本页面执行
             */
            handleDebugScript () {
                debugScriptCache.setItem(this.scriptInfo.content);
                const { href } = this.$router.resolve({
                    name: 'fastExecuteScript',
                    query: {
                        model: 'debugScript',
                    },
                });
                window.open(href);
            },
            /**
             * @desc 取消新建
             */
            handleCancel () {
                leaveConfirm()
                    .then(() => {
                        this.$emit('on-create-cancel');
                    });
            },
        },
    };
</script>
<style lang='postcss'>
    .copy-create-script-box {
        .script-version {
            position: relative;

            .new-flag {
                position: absolute;
                top: 50%;
                right: 0;
                margin-right: -10px;
                color: #ff9c01;
                transform: translateY(-50%) translateX(100%);
            }
        }

        .input {
            width: 510px;
            background: #fff;
        }
    }
</style>

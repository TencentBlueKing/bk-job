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
    <layout class="script-manage-copy-create-box">
        <div slot="title">
            {{ $t('script.新建脚本') }}
        </div>
        <template slot="sub-header">
            <Icon
                v-bk-tooltips="$t('上传脚本')"
                v-test="{ type: 'button', value: 'uploadScript' }"
                type="upload"
                @click="handleUploadScript" />
            <Icon
                v-bk-tooltips="$t('历史缓存')"
                v-test="{ type: 'button', value: 'scriptEditHistory' }"
                type="history"
                @click.stop="handleShowHistory" />
            <Icon
                v-bk-tooltips="$t('全屏')"
                v-test="{ type: 'button', value: 'scriptEditFullscreen' }"
                type="full-screen"
                @click="handleFullScreen" />
        </template>
        <div slot="left">
            <jb-form
                ref="form"
                v-test="{ type: 'form', value: 'copyCreateScript' }"
                form-type="vertical"
                :model="formData"
                :rules="rules">
                <jb-form-item
                    :label="$t('script.版本号.label')"
                    property="version"
                    required>
                    <div class="script-version">
                        <jb-input
                            :maxlength="30"
                            :placeholder="$t('script.输入版本号')"
                            property="version"
                            :value="formData.version"
                            @change="handleVersionChange" />
                        <Icon
                            class="new-flag"
                            svg
                            type="new-dark" />
                    </div>
                </jb-form-item>
                <jb-form-item :label="$t('script.版本日志')">
                    <bk-input
                        v-model="formData.versionDesc"
                        :maxlength="100"
                        :rows="5"
                        type="textarea" />
                </jb-form-item>
            </jb-form>
        </div>
        <div ref="content">
            <ace-editor
                v-if="contentHeight > 0"
                ref="aceEditor"
                v-model="formData.content"
                :height="contentHeight"
                :lang="formData.typeName"
                :options="formData.typeName"
                @on-mode-change="handleTypeChange" />
        </div>
        <template #footer>
            <bk-button
                v-test="{ type: 'button', value: 'copyCreateScriptSubmit' }"
                class="w120 mr10"
                :loading="isSubmiting"
                theme="primary"
                @click="handleSubmit">
                {{ $t('script.提交') }}
            </bk-button>
            <bk-button
                v-test="{ type: 'button', value: 'debugScript' }"
                class="mr10"
                @click="handleDebugScript">
                {{ $t('script.调试') }}
            </bk-button>
            <bk-button
                v-test="{ type: 'button', value: 'copyCreateScriptCancel' }"
                @click="handleCancel">
                {{ $t('script.取消') }}
            </bk-button>
        </template>
    </layout>
</template>
<script>
    import _ from 'lodash';

    import PublicScriptManageService from '@service/public-script-manage';
    import ScriptManageService from '@service/script-manage';

    import {
        checkPublicScript,
        formatScriptTypeValue,
        genDefaultScriptVersion,
        getOffset,
        leaveConfirm,
        scriptErrorConfirm,
    } from '@utils/assist';
    import { debugScriptCache } from '@utils/cache-helper';
    import { scriptVersionRule } from '@utils/validator';

    import AceEditor from '@components/ace-editor';
    import JbInput from '@components/jb-input';

    import Layout from './components/layout';

    import I18n from '@/i18n';

    const genDefaultFormData = () => ({
        id: '',
        typeName: 'Shell',
        version: genDefaultScriptVersion(),
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
        inheritAttrs: false,
        props: {
            scriptInfo: {
                type: Object,
                required: true,
            },
            scriptVersionList: {
                type: Array,
                default: () => [],
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
                    const {
                        content,
                        id,
                        name,
                        versionDesc,
                        type,
                        typeName,
                    } = scriptInfo;
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
            this.scriptManageServiceHandler = this.publicScript ? PublicScriptManageService : ScriptManageService;
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
                        validator: value => ScriptManageService.getScriptValidation({
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
            this.calcContentHeight();
            const handleResize = _.throttle(this.calcContentHeight, 60);
            window.addEventListener('resize', handleResize);
            this.$once('hook:beforeDestroy', () => {
                window.removeEventListener('resize', handleResize);
            });
        },
        methods: {
            /**
             * @desc 计算内容去高度
             */
            calcContentHeight () {
                const contentOffsetTop = getOffset(this.$refs.content).top;
                this.contentHeight = window.innerHeight - contentOffsetTop - 66;
            },
            handleUploadScript () {
                this.$refs.aceEditor.handleUploadScript();
            },
            handleShowHistory () {
                this.$refs.aceEditor.handleShowHistory();
            },
            handleFullScreen () {
                this.$refs.aceEditor.handleFullScreen();
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
                if (!this.formData.content) {
                    this.messageError(I18n.t('script.脚本内容不能为空'));
                    return;
                }
                this.isSubmiting = true;
                Promise.all([
                    // 验证表单
                    this.$refs.form.validate(),
                    // 脚本高危语句检测
                    ScriptManageService.getScriptValidation({
                        content: this.formData.content,
                        scriptType: this.formData.type,
                    }).then((data) => {
                        // 高危语句报错状态需要全局保存
                        const dangerousContent = _.find(data, _ => _.isDangerous);
                        this.$store.commit('setScriptCheckError', dangerousContent);
                        return true;
                    }),
                ])
                    .then(scriptErrorConfirm)
                    .then(() => {
                        this.scriptManageServiceHandler.scriptUpdate({
                            ...this.formData,
                        }).then((data) => {
                            this.$emit('on-create', {
                                scriptVersionId: data.scriptVersionId,
                            });
                            window.changeConfirm = false;
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
                debugScriptCache.setItem(this.formData.content);
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
    .script-manage-copy-create-box {
        .script-version {
            position: relative;

            .new-flag {
                position: absolute;
                top: -32px;
                left: 55px;
                font-size: 32px;
                color: #7d6b50;
            }
        }
    }
</style>

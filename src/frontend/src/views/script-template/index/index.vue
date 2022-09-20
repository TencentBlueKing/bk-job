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
        ref="container"
        v-bkloading="{ isLoading }"
        class="script-template-page">
        <ace-editor
            ref="editor"
            :before-lang-change="beforeLangChange"
            class="script-template-editor"
            :custom-enable="false"
            :height="editorHeight"
            :lang="scriptLanguage"
            @change="handleContentChange"
            @on-mode-change="handleLangChange">
            <template slot="action">
                <Icon
                    v-bk-tooltips="$t('scriptTemplate.内置变量')"
                    type="variable"
                    @click="handleSidePanelShow('renderVariable')" />
            </template>
            <div
                v-if="sidePanelComponentName"
                slot="side"
                class="script-editor-right-panel">
                <component
                    :is="renderSideComponent"
                    :parent-width="editorWidth"
                    :script-content="scriptContent"
                    :script-language="scriptLanguage"
                    @on-resize="handleSidePanelResize" />
                <div
                    class="panel-close"
                    @click="handleSidePanelHide">
                    <Icon type="close" />
                </div>
            </div>
        </ace-editor>
        <div class="action-box">
            <bk-button
                class="mr10"
                :loading="isSubmiting"
                style="width: 86px;"
                theme="primary"
                @click="handleSave">
                {{ $t('scriptTemplate.保存') }}
            </bk-button>
            <div
                class="action-btn mr10"
                @click="handleEditReset">
                {{ $t('scriptTemplate.重置') }}
            </div>
            <div
                class="action-btn"
                @click="handleUseDefault">
                {{ $t('scriptTemplate.还原默认') }}
            </div>
            <div
                class="action-btn"
                :class="{
                    active: sidePanelComponentName === 'previewTemplate',
                }"
                style="margin-left: 60px;"
                @click="handleSidePanelShow('previewTemplate')">
                {{ $t('scriptTemplate.渲染预览') }}
            </div>
        </div>
    </div>
</template>
<script>
    import _ from 'lodash';

    import ScriptTemplateService from '@service/script-template';

    import {
        formatScriptTypeValue,
        getOffset,
        leaveConfirm,
    } from '@utils/assist';

    import AceEditor, { builtInScript } from '@components/ace-editor';

    import PreviewTemplate from './components/preview-template';
    import RenderVariable from './components/render-variable';

    import I18n from '@/i18n';

    export default {
        name: '',
        components: {
            AceEditor,
            RenderVariable,
            PreviewTemplate,
        },
        data () {
            return {
                isLoading: false,
                isSubmiting: false,
                editorHeight: 0,
                editorWidth: 0,
                isShowVariable: false,
                scriptLanguage: 'Shell',
                scriptContent: '',
                sidePanelComponentName: '',
            };
        },
        computed: {
            renderSideComponent () {
                const com = {
                    previewTemplate: PreviewTemplate,
                    renderVariable: RenderVariable,
                };
                return com[this.sidePanelComponentName];
            },
        },
        created () {
            // 已经存储的脚本模板
            this.templateMap = Object.assign({}, builtInScript);
            this.fetchOriginalTemplate();
        },
        mounted () {
            this.calcEditorSize();
            window.addEventListener('resize', this.calcEditorSize);
            this.$once('hook:beforeDestroy', () => {
                window.removeEventListener('resize', this.calcEditorSize);
            });
        },
        methods: {
            /**
             * @desc 获取用户自定义模板
             */
            fetchOriginalTemplate () {
                this.isLoading = true;
                ScriptTemplateService.fetchOriginalTemplate()
                    .then((data) => {
                        data.forEach((item) => {
                            this.templateMap[formatScriptTypeValue(item.scriptLanguage)] = item.scriptContent;
                        });
                        // 如果有自定义脚本模板，通过编辑器 setValue 方法设置值
                        if (_.has(this.templateMap, this.scriptLanguage)) {
                            this.$refs.editor.setValue(this.templateMap[this.scriptLanguage]);
                        }
                    })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 计算编辑器的尺寸
             */
            calcEditorSize () {
                const {
                    top: offsetTop,
                } = getOffset(this.$refs.container);
                const windowHeight = window.innerHeight;
                this.editorHeight = windowHeight - offsetTop - 72;
                this.editorWidth = this.$refs.container.getBoundingClientRect().width;
            },
            /**
             * @desc 脚本预览类型切换编辑状态检测
             * @return {Object} 切换二次确认
             */
            beforeLangChange () {
                window.changeConfirm = this.scriptContent !== this.templateMap[this.scriptLanguage];
                return leaveConfirm();
            },
            /**
             * @desc 切换脚本模板语言
             * @param {String} scriptLanguage 脚本语言
             */
            handleLangChange (scriptLanguage) {
                this.scriptLanguage = scriptLanguage;
                setTimeout(() => {
                    if (_.has(this.templateMap, this.scriptLanguage)) {
                        this.$refs.editor.setValue(this.templateMap[this.scriptLanguage]);
                    }
                });
            },
            /**
             * @desc 编辑脚本模板内容
             * @param {String} content 脚本语言
             */
            handleContentChange (content) {
                this.scriptContent = content;
            },
            /**
             * @desc 显示编辑器右侧面板
             * @param {String} componentName 脚本语言
             */
            handleSidePanelShow (componentName) {
                if (this.sidePanelComponentName === componentName) {
                    this.sidePanelComponentName = '';
                } else {
                    this.sidePanelComponentName = componentName;
                }
            },
            /**
             * @desc 关闭编辑器右侧面板
             */
            handleSidePanelHide () {
                this.sidePanelComponentName = '';
            },
            /**
             * @desc 脚本编辑器 resize
             */
            handleSidePanelResize () {
                this.$refs.editor.resize();
            },
            /**
             * @desc 保存用户自定义模板
             */
            handleSave () {
                this.isSubmiting = true;
                ScriptTemplateService.updateOriginalTemplate({
                    scriptLanguage: formatScriptTypeValue(this.scriptLanguage),
                    scriptContent: this.scriptContent,
                }).then(() => {
                    window.changeConfirm = false;
                    this.templateMap[this.scriptLanguage] = this.scriptLanguage;
                    this.messageSuccess(I18n.t('scriptTemplate.保存成功'));
                })
                    .finally(() => {
                        this.isSubmiting = false;
                    });
            },
            /**
             * @desc 重置用户编辑状态
             */
            handleEditReset () {
                this.$refs.editor.setValue(this.templateMap[this.scriptLanguage]);
                this.messageSuccess(I18n.t('scriptTemplate.重置成功'));
            },
            /**
             * @desc 还原脚本模板为默认脚本
             */
            handleUseDefault () {
                this.$refs.editor.resetValue();
                this.messageSuccess(I18n.t('scriptTemplate.还原默认成功'));
            },
        },
    };
</script>
<style lang='postcss'>
    .script-template-page {
        .action-box {
            display: flex;
            height: 52px;
            padding-left: 16px;
            background: #2e2e2e;
            box-shadow: 0 -2px 4px 0 rgb(0 0 0 / 16%);
            align-items: center;

            .action-btn {
                width: 86px;
                height: 32px;
                font-size: 14px;
                line-height: 32px;
                color: #979ba5;
                text-align: center;
                cursor: pointer;
                background: #242424;
                border: 1px solid #5c5e66;
                border-radius: 2px;
                transition: all 0.15s;

                &:hover {
                    color: #b1b6c2;
                    border-color: #878b94;
                }

                &.active {
                    color: #699df4;
                    background: #242424;
                    border: 1px solid transparent;
                }
            }
        }

        .bk-loading {
            background: rgb(0 0 0 / 80%) !important;
        }
    }

    .script-template-editor {
        .jb-ace-title {
            background: #2e2e2e;

            .jb-ace-mode-item {
                &.active {
                    background: #1a1a1a;
                }
            }
        }
        /* stylelint-disable selector-class-pattern */
        .ace_editor {
            background: #1a1a1a;

            .ace_gutter {
                background: #1a1a1a;
            }
        }
    }

    .script-editor-right-panel {
        position: relative;
        height: 100%;
        font-size: 14px;
        line-height: 19px;
        color: #c4c6cc;
        border-left: 1px solid #333;

        .panel-close {
            position: absolute;
            top: 0;
            right: 0;
            display: flex;
            width: 26px;
            height: 26px;
            font-size: 18px;
            color: #63656e;
            cursor: pointer;
            border-radius: 50%;
            align-items: center;
            justify-content: center;

            &:hover {
                background-color: #313238;
            }
        }
    }
</style>

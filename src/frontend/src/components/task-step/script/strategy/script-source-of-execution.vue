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
    <div class="script-source-of-execution">
        <jb-form-item class="script-source-item" :label="$t('脚本来源')" required>
            <bk-radio-group @change="handleScriptSourceChange" :value="scriptSource">
                <bk-radio-button value="local">{{ $t('手工录入') }}</bk-radio-button>
                <bk-radio-button value="refer">{{ $t('脚本引用') }}</bk-radio-button>
            </bk-radio-group>
        </jb-form-item>
        <jb-form-item
            ref="scriptId"
            v-if="isScriptRefer"
            :label="$t('脚本引用')"
            required
            property="scriptId"
            :rules="rules">
            <div class="form-item-content refer-script-box">
                <compose-form-item>
                    <bk-select
                        style="width: 120px;"
                        @change="handleReferScriptTypeChange"
                        :value="referType"
                        :clearable="false">
                        <bk-option :id="2" :name="$t('业务脚本')">{{ $t('业务脚本') }}</bk-option>
                        <bk-option :id="3" :name="$t('公共脚本')">{{ $t('公共脚本') }}</bk-option>
                    </bk-select>
                    <bk-select
                        :key="referType"
                        :placeholder="$t('选择引用脚本')"
                        class="refer-script-list"
                        :value="formData[scriptVersionIdField]"
                        @change="handleScriptVersionIdChange"
                        :clearable="false"
                        searchable>
                        <auth-option
                            v-for="option in scripListDisplay"
                            :key="option.scriptVersionId"
                            :id="option.scriptVersionId"
                            :permission="option.canView"
                            :resource-id="option.id"
                            :name="option.name"
                            :auth="authView" />
                        <template slot="extension">
                            <auth-component :auth="authCreate">
                                <div @click="handleGoCreate" style="cursor: pointer;">
                                    <i class="bk-icon icon-plus-circle mr10" />{{ $t('新增.action') }}
                                </div>
                                <div slot="forbid">
                                    <i class="bk-icon icon-plus-circle mr10" />{{ $t('新增.action') }}
                                </div>
                            </auth-component>
                        </template>
                    </bk-select>
                </compose-form-item>
                <div
                    v-if="formData[scriptVersionIdField]"
                    class="refer-script-detail"
                    :tippy-tips="$t('脚本详情')"
                    @click="handleGoScriptDetail">
                    <Icon type="jump" />
                </div>
            </div>
        </jb-form-item>
    </div>
</template>
<script>
    import I18n from '@/i18n';
    import ScriptService from '@service/script-manage';
    import TaskStepModel from '@model/task/task-step';
    import ComposeFormItem from '@components/compose-form-item';

    export default {
        components: {
            ComposeFormItem,
        },
        props: {
            // 脚本来源字段名
            scriptSourceField: {
                type: String,
                required: true,
            },
            // 脚本id字段名
            scriptIdField: {
                type: String,
                required: true,
            },
            // 脚本版本id字段名
            scriptVersionIdField: {
                type: String,
            },
            // 脚本内容字段名
            contentField: {
                type: String,
                required: true,
            },
            // 脚本语言字段名
            languageField: {
                type: String,
                required: true,
            },
            formData: {
                type: Object,
                default: () => ({}),
            },
        },
        data () {
            return {
                scripList: [],
                publicScripList: [],
                scriptSource: 'local',
                referType: 2,
            };
        },
        computed: {
            /**
             * @desc 使用脚本资源需要的权限
             * @returns {String}
             */
            authView () {
                return this.formData[this.scriptSourceField] === TaskStepModel.scriptStep.TYPE_SOURCE_BUSINESS
                    ? 'script/view'
                    : 'public_script/view';
            },
            /**
             * @desc 脚本新建的权限
             * @returns { String }
             */
            authCreate () {
                return this.formData[this.scriptSourceField] === TaskStepModel.scriptStep.TYPE_SOURCE_BUSINESS
                    ? 'script/create'
                    : 'public_script/create';
            },
            /**
             * @desc 引用脚本类型
             * @returns { Boolean }
             */
            isScriptRefer () {
                return this.scriptSource === 'refer';
            },
            /**
             * @desc 脚本列表
             * @returns { Array }
             */
            scripListDisplay () {
                const scriptSource = this.formData[this.scriptSourceField];
                
                if (scriptSource === TaskStepModel.scriptStep.TYPE_SOURCE_BUSINESS) {
                    return this.scripList;
                }
                if (scriptSource === TaskStepModel.scriptStep.TYPE_SOURCE_PUBLIC) {
                    return this.publicScripList;
                }
                return [];
            },
            /**
             * @desc 表单想验证规则
             * @returns { Array }
             *
             * 引用类型的脚本时 scriptId 不能为空
             */
            rules () {
                if (this.isScriptRefer) {
                    return [{
                        required: true,
                        message: I18n.t('请选择引用脚本'),
                        trigger: 'blur',
                    }];
                }
                return [];
            },
        },
        watch: {
            formData: {
                handler () {
                    this.initScriptSource();
                },
                immediate: true,
            },
            'formData.scriptId' (value) {
                if (value) {
                    this.$refs.scriptId.clearValidator();
                }
            },
        },
        created () {
            if (this.formData[this.scriptVersionIdField]) {
                this.handleScriptVersionIdChange(this.formData[this.scriptVersionIdField]);
            }
            this.fetchScriptList();
            this.fetchPublicScriptList();
        },
        methods: {
            /**
             * @desc 获取业务脚本列表
             */
            fetchScriptList () {
                ScriptService.getOnlineScriptList()
                    .then((data) => {
                        this.scripList = data;
                    });
            },
            /**
             * @desc 获公共脚本列表
             */
            fetchPublicScriptList () {
                ScriptService.getOnlineScriptList({
                    publicScript: true,
                }).then((data) => {
                    this.publicScripList = data;
                });
            },
            /**
             * @desc 初始化脚本来源
             */
            initScriptSource () {
                if (this.formData[this.scriptSourceField] === TaskStepModel.scriptStep.TYPE_SOURCE_LOCAL) {
                    this.scriptSource = 'local';
                    return;
                }
                this.scriptSource = 'refer';
                // 如果是引用脚本，还需初始化引用类型
                this.referType = this.formData[this.scriptSourceField];
            },
            /**
             * @desc 更新脚本来源
             * @param {String} source 脚本来源
             */
            handleScriptSourceChange (source) {
                // 脚本来源改变重置脚本相关的信息
                const scriptSource = source === 'local'
                    ? TaskStepModel.scriptStep.TYPE_SOURCE_LOCAL
                    : TaskStepModel.scriptStep.TYPE_SOURCE_BUSINESS;
                this.$emit('on-reset', {
                    [this.scriptSourceField]: scriptSource,
                    [this.scriptIdField]: '',
                    [this.scriptVersionIdField]: '',
                });
            },
            /**
             * @desc 更新脚本引用来源类型
             * @param {String} scriptSource 脚本引用来源类型
             */
            handleReferScriptTypeChange (scriptSource) {
                if (scriptSource === this.formData[this.scriptSourceField]) {
                    return;
                }
                this.$emit('on-reset', {
                    [this.scriptSourceField]: scriptSource,
                    [this.scriptIdField]: '',
                    [this.scriptVersionIdField]: '',
                });
            },
            /**
             * @desc 更新脚本引用版本
             * @param {String} scriptVersionId 脚本引用来源类型
             */
            handleScriptVersionIdChange (scriptVersionId) {
                if (!scriptVersionId) {
                    return;
                }

                this.$emit('on-reset', {
                    isScriptContentLoading: true,
                });
                ScriptService.versionDetail({
                    id: scriptVersionId,
                }).then(({ id, content, type, publicScript }) => {
                    let scriptSource = TaskStepModel.scriptStep.TYPE_SOURCE_BUSINESS;
                    if (publicScript) {
                        scriptSource = TaskStepModel.scriptStep.TYPE_SOURCE_PUBLIC;
                    }
                    this.$emit('on-reset', {
                        [this.scriptSourceField]: scriptSource,
                        [this.scriptIdField]: id,
                        [this.contentField]: content,
                        [this.languageField]: type,
                        [this.scriptVersionIdField]: scriptVersionId,
                    });
                })
                    .finally(() => {
                        this.$emit('on-reset', {
                            isScriptContentLoading: false,
                        });
                    });
            },
            /**
             * @desc 跳转到选择的脚本版本详情
             */
            handleGoScriptDetail () {
                const routerName = this.formData[this.scriptSourceField] === TaskStepModel.scriptStep.TYPE_SOURCE_PUBLIC
                    ? 'publicScriptVersion'
                    : 'scriptVersion';

                const { href } = this.$router.resolve({
                    name: routerName,
                    params: {
                        id: this.formData[this.scriptIdField],
                    },
                    query: {
                        scriptVersionId: this.formData[this.scriptVersionIdField],
                    },
                });
                
                window.open(href);
            },
            /**
             * @desc 跳转新建脚本页面
             */
            handleGoCreate () {
                const routerName = this.formData[this.scriptSourceField] === TaskStepModel.scriptStep.TYPE_SOURCE_PUBLIC
                    ? 'createPublicScript'
                    : 'createScript';

                const { href } = this.$router.resolve({
                    name: routerName,
                });
                
                window.open(href);
            },
        },
    };
</script>
<style lang='postcss'>
    @import '@/css/mixins/media';

    .script-source-of-execution {
        .script-source-item {
            .bk-radio-button-text {
                width: 120px;
                text-align: center;
            }
        }

        .refer-script-box {
            position: relative;
            display: flex;

            .refer-script-detail {
                position: absolute;
                top: 0;
                right: -40px;
                bottom: 0;
                display: flex;
                align-items: center;
                justify-content: center;
                width: 40px;
                font-size: 16px;
                color: #3a84ff;
                cursor: pointer;
            }

            .refer-script-list {
                width: calc(500px - 120px);

                @media (--small-viewports) {
                    width: calc(500px - 120px);
                }

                @media (--medium-viewports) {
                    width: calc(560px - 120px);
                }

                @media (--large-viewports) {
                    width: calc(620px - 120px);
                }

                @media (--huge-viewports) {
                    width: calc(680px - 120px);
                }
            }
        }
    }
</style>

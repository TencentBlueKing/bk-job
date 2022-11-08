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
        class="exec-script-view"
        :class="{ loading: isLoading }">
        <detail-item :label="$t('template.脚本来源：')">
            {{ stepInfo.scriptSourceText }}
        </detail-item>
        <detail-item
            v-if="isReferScript"
            :label="$t('template.脚本引用：')">
            <span>{{ scriptName }}</span>
            <Icon
                v-bk-tooltips="$t('template.脚本详情')"
                class="script-detail"
                type="jump"
                @click="handleGoScriptDetail" />
            <div
                v-if="stepInfo.isNeedUpdate || stepInfo.isDisabled"
                class="script-update-flag">
                <span v-html="stepInfo.scriptStatusHtml" />
                <bk-button
                    v-if="stepInfo.isNeedUpdate"
                    text
                    @click="handleGoScriptVersion">
                    {{ $t('template.版本对比') }}
                </bk-button>
                <bk-button
                    text
                    @click="handleUpdateScript">
                    {{ $t('template.去更新') }}
                </bk-button>
            </div>
        </detail-item>
        <detail-item
            :label="$t('template.脚本内容：')"
            layout="vertical">
            <ace-editor
                :lang="language"
                :options="languageOption"
                readonly
                :value="stepInfo.content" />
        </detail-item>
        <div>
            <detail-item :label="$t('template.脚本参数：')">
                <jb-edit-textarea
                    field="scriptParam"
                    readonly
                    :value="stepInfo.scriptParam" />
            </detail-item>
            <detail-item :label="$t('template.超时时长：')">
                {{ stepInfo.timeout }}（s）
            </detail-item>
            <detail-item :label="$t('template.错误处理：')">
                {{ stepInfo.ignoreErrorText }}
            </detail-item>
            <detail-item :label="$t('template.执行账号：')">
                {{ executeAccountText }}
            </detail-item>
        </div>
        <detail-item
            v-if="stepInfo.executeTarget.variable"
            :label="$t('template.执行目标：')">
            <render-global-variable
                :data="variable"
                :name="stepInfo.executeTarget.variable"
                :type="$t('template.执行目标')" />
        </detail-item>
        <detail-item
            v-else
            :label="$t('template.执行目标：')"
            layout="vertical">
            <!-- <server-panel
                detail-fullscreen
                :host-node-info="stepInfo.executeTarget.hostNodeInfo" /> -->
            <ip-selector
                readonly
                show-view
                :value="stepInfo.executeTarget.hostNodeInfo" />
        </detail-item>
        <slot />
    </div>
</template>
<script>
    import AccountManageService from '@service/account-manage';
    import ScriptService from '@service/script-manage';

    import {
        formatScriptTypeValue,
    } from '@utils/assist';

    import AceEditor from '@components/ace-editor';
    // import ServerPanel from '@components/choose-ip/server-panel';
    import DetailItem from '@components/detail-layout/item';
    import JbEditTextarea from '@components/jb-edit/textarea';

    import RenderGlobalVariable from './components/render-global-variable';

    export default {
        name: '',
        components: {
            AceEditor,
            // ServerPanel,
            DetailItem,
            JbEditTextarea,
            RenderGlobalVariable,
            
        },
        props: {
            data: {
                type: Object,
                default: () => ({}),
            },
            variable: {
                type: Array,
                default: () => [],
            },
        },
        data () {
            return {
                stepInfo: {},
                executeAccountText: '',
                language: '',
                scriptName: '',
                scriptInfo: {},
                isShowScriptSource: false,
                requestQueue: [],
            };
        },
        computed: {
            isLoading () {
                return this.requestQueue.length > 0;
            },
            isReferScript () {
                return this.data.scriptStepInfo.scriptSource && this.data.scriptStepInfo.scriptSource !== 1;
            },
        },
        created () {
            this.stepInfo = Object.freeze(this.data.scriptStepInfo);
            this.language = formatScriptTypeValue(this.stepInfo.scriptLanguage);
            this.languageOption = [
                this.language,
            ];
            if (this.stepInfo.scriptVersionId) {
                this.fetchScriptDetail();
            }
            this.fetchAccount();
        },
        methods: {
            /**
             * @desc 更新脚本版本获取版本详情
             */
            fetchScriptDetail () {
                this.requestQueue.push(true);
                ScriptService.versionDetail({
                    id: this.stepInfo.scriptVersionId,
                }).then((data) => {
                    this.scriptName = data.name;
                    this.stepInfo.content = data.content;
                    this.scriptInfo = Object.freeze(data);
                })
                    .finally(() => {
                        this.requestQueue.pop();
                    });
            },
            /**
             * @desc 获取完整的账号列表
             */
            fetchAccount () {
                this.requestQueue.push(true);
                AccountManageService.fetchAccountWhole()
                    .then((data) => {
                        const accountData = data.find(item => item.id === this.stepInfo.account);
                        if (accountData) {
                            this.executeAccountText = accountData.alias;
                        } else {
                            this.executeAccountText = '--';
                        }
                    })
                    .finally(() => {
                        this.requestQueue.pop();
                    });
            },
            /**
             * @desc 新开窗口跳转脚本版本列表
             */
            handleGoScriptDetail () {
                const routerName = this.scriptInfo.publicScript ? 'publicScriptVersion' : 'scriptVersion';

                const router = this.$router.resolve({
                    name: routerName,
                    params: {
                        id: this.stepInfo.scriptId,
                    },
                    query: {
                        scriptVersionId: this.scriptInfo.scriptVersionId,
                    },
                });
                window.open(router.href);
            },
            /**
             * @desc 脚本版本对比
             */
            handleGoScriptVersion () {
                const routerName = this.isReferPublicScript ? 'publicScriptVersion' : 'scriptVersion';
                const { href } = this.$router.resolve({
                    name: routerName,
                    params: {
                        id: this.stepInfo.scriptId,
                    },
                });
                window.open(href);
            },
            /**
             * @desc 编辑作业模板，更新步骤引用的脚本版本
             */
            handleUpdateScript () {
                this.$router.push({
                    name: 'templateEdit',
                    params: {
                        id: this.$route.params.id,
                        stepId: this.data.id,
                    },
                    query: {
                        from: 'templateDetail',
                    },
                });
            },
        },
    };
</script>
<style lang="postcss">
    .exec-script-view {
        &.loading {
            height: calc(100vh - 100px);
        }

        .detail-item {
            margin-bottom: 0;
        }

        .script-detail {
            color: #3a84ff;
            cursor: pointer;
        }

        .script-update-flag {
            display: inline-block;

            .script-update {
                color: #ff5656;
            }
        }
    }
</style>

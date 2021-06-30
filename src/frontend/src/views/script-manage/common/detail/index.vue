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
    <layout class="script-manage-detail-box" offset-target="content-dispaly" v-bind="$attrs">
        <div class="script-detail">
            <div class="script-version">{{ scriptInfo.version }}</div>
            <div class="script-status" v-html="scriptInfo.statusHtml" />
            <div class="script-lastModifyUser">
                <span class="label">{{ $t('script.更新人：') }}</span>
                <span class="value">{{ scriptInfo.lastModifyUser }}</span>
            </div>
            <div class="script-lastModifyTime">
                <span class="label">{{ $t('script.更新时间：') }}</span>
                <span class="value">{{ scriptInfo.lastModifyTime }}</span>
            </div>
        </div>
        <div class="version-content">
            <div class="content-tab">
                <div class="content-tab-item" :class="{ active: contentTab === 'content' }" @click="handleChangeDispaly('content')">{{ $t('script.脚本内容') }}</div>
                <div class="content-tab-item" :class="{ active: contentTab === 'log' }" @click="handleChangeDispaly('log')">{{ $t('script.版本日志') }}</div>
            </div>
            <div ref="content" class="content-dispaly" :style="contentStyles">
                <keep-alive v-if="contentHeight > 0">
                    <component
                        :is="contentCom"
                        readonly
                        :height="contentHeight"
                        :value="scriptInfo.content"
                        :lang="scriptInfo.typeName"
                        :options="[scriptInfo.typeName]"
                        :version-desc="scriptInfo.versionDesc" />
                </keep-alive>
            </div>
        </div>
        <template #footer>
            <auth-button
                v-if="scriptInfo.isOnline"
                key="execute"
                :permission="scriptInfo.canManage"
                auth="script/execute"
                :resource-id="scriptInfo.id"
                class="w120 mr10"
                theme="primary"
                :loading="isExceLoading"
                @click="handleGoExce">
                {{ $t('script.去执行') }}
            </auth-button>
            <jb-popover-confirm
                v-if="!scriptInfo.isOnline"
                key="online"
                class="mr10"
                :title="$t('script.确定上线该版本？')"
                :content="$t('script.上线后，之前的线上版本将被置为「已下线」状态，但不影响作业使用')"
                :disabled="scriptInfo.isDisabledOnline"
                :confirm-handler="handleOnline">
                <auth-button
                    :permission="scriptInfo.canManage"
                    :resource-id="scriptInfo.id"
                    auth="script/edit"
                    theme="primary"
                    class="w120"
                    :disabled="scriptInfo.isDisabledOnline">
                    {{ $t('script.上线') }}
                </auth-button>
            </jb-popover-confirm>
            <auth-button
                v-if="!scriptInfo.isDraft"
                key="create"
                :permission="scriptInfo.canClone"
                :resource-id="scriptInfo.id"
                auth="script/clone"
                class="w120 mr10"
                @click="handleCopyAndCreate(scriptInfo)">
                {{ $t('script.复制并新建') }}
            </auth-button>
            <bk-button class="mr10" @click="handleDebugScript">调试</bk-button>
            
            <auth-button
                v-if="scriptInfo.isOnline"
                key="sync"
                :permission="scriptInfo.canManage"
                :resource-id="scriptInfo.id"
                auth="script/edit"
                class="mr10"
                :disabled="!scriptInfo.syncEnabled"
                @click="handleGoSync">
                {{ $t('script.同步') }}
            </auth-button>
            <auth-button
                v-if="scriptInfo.isDraft"
                key="edit"
                :permission="scriptInfo.canManage"
                :resource-id="scriptInfo.id"
                auth="script/edit"
                class="mr10"
                @click="handleEdit(scriptInfo)">
                {{ $t('script.编辑') }}
            </auth-button>
            <jb-popover-confirm
                v-if="scriptInfo.isVersionEnableRemove"
                key="delete"
                class="mr10"
                :title="$t('script.确定删除该版本？')"
                :content="$t('script.删除后不可恢复，请谨慎操作！')"
                :confirm-handler="handleRemove">
                <auth-button
                    :permission="scriptInfo.canManage"
                    :resource-id="scriptInfo.id"
                    auth="script/delete">
                    {{ $t('script.删除') }}
                </auth-button>
            </jb-popover-confirm>
            <jb-popover-confirm
                v-if="scriptInfo.isOnline"
                key="offline"
                style="margin-left: auto;"
                :title="$t('script.确定禁用该版本？')"
                :content="$t('script.一旦禁用成功，线上引用该版本的作业脚本步骤都会执行失败，请务必谨慎操作！')"
                :confirm-handler="handleOffline">
                <auth-button
                    :permission="scriptInfo.canManage"
                    :resource-id="scriptInfo.id"
                    auth="script/edit">
                    {{ $t('script.禁用') }}
                </auth-button>
            </jb-popover-confirm>
        </template>
    </layout>
</template>
<script>
    import I18n from '@/i18n';
    import ScriptService from '@service/script-manage';
    import PublicScriptService from '@service/public-script-manage';
    import {
        checkPublicScript,
        getOffset,
    } from '@utils/assist';
    import {
        debugScriptCache,
    } from '@utils/cache-helper';
    import AceEditor from '@components/ace-editor';
    import DetailLayout from '@components/detail-layout';
    import DetailItem from '@components/detail-layout/item';
    import JbPopoverConfirm from '@components/jb-popover-confirm';
    import Layout from '../layout';
    import RenderLog from './components/render-log';

    export default {
        name: '',
        components: {
            AceEditor,
            DetailLayout,
            DetailItem,
            JbPopoverConfirm,
            Layout,
            RenderLog,
        },
        inheritAttrs: false,
        props: {
            scriptVersionList: {
                type: Array,
                default: () => [],
            },
            scriptInfo: {
                type: Object,
                required: true,
            },
        },
        data () {
            return {
                isLoading: false,
                isExceLoading: false,
                contentTab: 'content',
                contentHeight: 0,
            };
        },
        computed: {
            contentCom () {
                const comMap = {
                    content: AceEditor,
                    log: RenderLog,
                };
                return comMap[this.contentTab];
            },
            contentStyles () {
                return {
                    height: `${this.contentHeight}px`,
                };
            },
        },
        watch: {
            scriptInfo () {
                this.contentTab = 'content';
            },
        },
        created () {
            this.publicScript = checkPublicScript(this.$route);
            this.serviceHandler = this.publicScript ? PublicScriptService : ScriptService;
            window.addEventListener('resize', this.init);
            this.$once('hook:beforeDestroy', () => {
                window.removeEventListener('resize', this.init);
            });
        },
        mounted () {
            this.init();
        },
        methods: {
            /**
             * @desc 计算内容区高度
             */
            init () {
                const contentOffsetTop = getOffset(this.$refs.content).top;
                const contentHeight = window.innerHeight - contentOffsetTop - 101;
                this.contentHeight = contentHeight < 480 ? 480 : contentHeight;
            },
            /**
             * @desc 脚本内容和脚本版本日志切换
             * @param {String} tab 切换面板
             */
            handleChangeDispaly (tab) {
                this.contentTab = tab;
            },
            /**
             * @desc 上线脚本
             */
            handleOnline () {
                return this.serviceHandler.scriptVersionOnline({
                    id: this.scriptInfo.id,
                    versionId: this.scriptInfo.scriptVersionId,
                }).then(() => {
                    this.messageSuccess(I18n.t('script.操作成功'));
                    this.$emit('on-edit-change');
                });
            },
            /**
             * @desc 删除脚本
             */
            handleRemove () {
                return this.serviceHandler.scriptVersionRemove({
                    versionId: this.scriptInfo.scriptVersionId,
                }).then(() => {
                    this.messageSuccess(I18n.t('script.删除成功'));
                    // 如果删除的是最后一个版本，成功后跳转到脚本列表
                    if (this.scriptVersionList.length < 2) {
                        const routerName = this.publicScript ? 'publicScriptList' : 'scriptList';
                        this.$router.push({
                            name: routerName,
                        });
                    } else {
                        setTimeout(() => {
                            this.$emit('on-delete', true);
                        });
                    }
                });
            },
            /**
             * @desc 下线脚本
             */
            handleOffline () {
                return this.serviceHandler.scriptVersionOffline({
                    id: this.scriptInfo.id,
                    versionId: this.scriptInfo.scriptVersionId,
                }).then(() => {
                    this.messageSuccess(I18n.t('script.操作成功'));
                    this.$emit('on-edit-change');
                });
            },
            /**
             * @desc 复制新建
             */
            handleCopyAndCreate () {
                this.$emit('on-go-copy-create', {
                    scriptVersionId: this.scriptInfo.scriptVersionId,
                });
            },
            /**
             * @desc 调整到快速执行脚本页面调试脚本
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
             * @desc 同步脚本
             */
            handleGoSync () {
                const routerName = this.publicScript ? 'scriptPublicSync' : 'scriptSync';
                this.$router.push({
                    name: routerName,
                    params: {
                        scriptId: this.scriptInfo.id,
                        scriptVersionId: this.scriptInfo.scriptVersionId,
                    },
                });
            },
            /**
             * @desc 编辑脚本
             */
            handleEdit (payload) {
                this.$emit('on-go-edit', {
                    scriptVersionId: this.scriptInfo.scriptVersionId,
                });
            },
            /**
             * @desc 调整到快速执行脚本页面执行脚本
             */
            handleGoExce () {
                this.$router.push({
                    name: 'fastExecuteScript',
                    params: {
                        taskInstanceId: 0,
                        scriptVersionId: this.scriptInfo.scriptVersionId,
                    },
                    query: {
                        from: 'scriptVersion',
                    },
                });
            },
        },
    };
</script>
<style lang='postcss'>
    @import '@/css/mixins/scroll';

    %tab-item {
        display: flex;
        align-items: center;
        height: 43px;
        padding-left: 20px;
        font-size: 12px;
    }

    .script-manage-detail-box {
        .script-detail {
            display: flex;
            align-items: center;
            height: 25px;
            margin-bottom: 28px;
            font-size: 14px;

            .script-version {
                font-size: 18px;
                color: #000;
            }

            .script-status {
                margin-right: auto;
                margin-left: 10px;
            }

            .script-lastModifyTime {
                margin-left: 10px;
            }

            .label {
                color: #b2b5bd;
            }

            .value {
                color: #63656e;
            }
        }

        .render-script-version {
            display: flex;
        }

        .version-content {
            display: flex;
            flex-direction: column;
            flex: 1;
            border: 1px solid #dcdee5;

            .content-tab {
                display: flex;
                background-color: #f0f1f5;
                border-bottom: 1px solid #dcdee5;
            }

            .content-tab-item {
                @extend %tab-item;

                padding-left: 0;
                font-size: 14px;
                color: #313238;
                cursor: pointer;
                border-right: 1px solid #dcdee5;
                flex: 0 0 120px;
                align-items: center;
                justify-content: center;

                &.active {
                    color: #3a84ff;
                    background: #fff;
                    border-top: 2px solid #3a84ff;
                }
            }

            .content-dispaly {
                background: #fff;
            }
        }

        .render-version-log {
            height: 100%;
            padding: 12px 10px;
            overflow-y: auto;
            font-size: 12px;
            line-height: 20px;
            color: #63656e;
            white-space: pre-line;

            @mixin scroller;
        }
    }
</style>

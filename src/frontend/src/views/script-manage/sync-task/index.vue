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
    <div class="script-manage-sync-task-page" v-bkloading="{ isLoading }">
        <div class="retry-btn">
            <bk-button :disabled="isRetryAllDisable" @click="handleAllRetry">{{ $t('script.全部重试') }}</bk-button>
        </div>
        <div class="table-top">
            {{ $t('script.同步作业模版') }}
            <span class="version-sum">
                （{{ $t('script.共') }} {{ data.length }} {{ $t('script.个.result') }}）
            </span>
        </div>
        <bk-table :data="data" row-class-name="sync-script-record">
            <bk-table-column
                :label="$t('script.作业模板名称')"
                prop="name"
                sortable>
                <template slot-scope="{ row }">
                    <router-link
                        target="_blank"
                        :to="{
                            name: 'templateDetail',
                            params: {
                                id: row.templateId,
                            },
                        }">
                        {{ row.templateName }}
                        <Icon type="edit" class="template-link" />
                    </router-link>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('script.步骤名称')" prop="stepName" />
            <bk-table-column :label="$t('script.引用的版本号')" prop="version">
                <template slot-scope="{ row }">
                    <bk-button
                        text
                        @click="handleShowSideslider(row.scriptVersionId)">
                        {{ row.scriptVersion }}
                    </bk-button>
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('script.引用版本状态')"
                prop="status"
                :filters="statusFilters"
                :filter-method="statusFilterMethod"
                :filter-multiple="true">
                <template slot-scope="{ row }">
                    <span v-html="row.statusHtml" />
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('script.状态')"
                width="350"
                prop="syncStatus">
                <template slot-scope="{ row, $index }">
                    <Icon :type="row.syncIcon" svg class="mr10" />
                    <span>{{ row.syncStatusMsg }}</span>
                    <bk-button
                        v-if="row.isSyncFailed"
                        :tippy-tips="row.failMsg"
                        text
                        style="margin-left: 15px;"
                        @click="handleRetry(row, $index)">
                        {{ $t('script.重试') }}
                    </bk-button>
                </template>
            </bk-table-column>
        </bk-table>
        <action-bar>
            <bk-button
                class="w120"
                theme="primary"
                @click="handleFinish">
                {{ $t('script.完成') }}
            </bk-button>
        </action-bar>
        <script-detail :is-show.sync="showSideslider" :script-version-id="selectScriptVersionId" />
        <element-teleport v-if="lastVersionScriptInfo.version">
            <span> - {{ $t('script.同步至') }}</span>
            <span>{{ lastVersionScriptInfo.version }}</span>
        </element-teleport>
    </div>
</template>
<script>
    import I18n from '@/i18n';
    import {
        checkPublicScript,
        leaveConfirm,
    } from '@utils/assist';
    import ScriptService from '@service/script-manage';
    import PublicScriptService from '@service/public-script-manage';
    import ActionBar from '../common/action-bar';
    import ScriptDetail from './components/script-detail';

    export default {
        components: {
            ActionBar,
            ScriptDetail,
        },
        data () {
            return {
                data: [],
                isLoading: false,
                showSideslider: false,
                selectScriptVersionId: 0,
                lastVersionScriptInfo: {},
            };
        },
        computed: {
            isRetryAllDisable () {
                // eslint-disable-next-line no-plusplus
                for (let i = 0; i < this.data.length; i++) {
                    if (this.data[i].isSyncFailed) {
                        return false;
                    }
                }
                return true;
            },
        },
        created () {
            this.statusFilters = [
                { value: 0, text: I18n.t('script.未上线') },
                { value: 1, text: I18n.t('script.已上线') },
                { value: 2, text: I18n.t('script.已下线') },
                { value: 3, text: I18n.t('script.已禁用') },
            ];
            this.publicScript = checkPublicScript(this.$route);
            this.serviceHandler = this.publicScript ? PublicScriptService : ScriptService;
            this.scriptId = this.$route.params.scriptId;
            this.scriptVersionId = this.$route.params.scriptVersionId;
            this.stepList = JSON.parse(localStorage.getItem('SYNC_TEMPLATE_STEP_SCRIPT'));
            if (this.stepList.length < 1) {
                this.messageError(I18n.t('script.请先选择作业模版步骤'));
                return;
            }
            this.fetchData();
            this.fetchLastScriptVersionDetail();
        },
        methods: {
            fetchData () {
                this.isLoading = true;
                this.serviceHandler.scriptVersionSync({
                    scriptId: this.scriptId,
                    scriptVersionId: this.scriptVersionId,
                    steps: this.stepList,
                }).then((data) => {
                    this.data = data;
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            fetchLastScriptVersionDetail () {
                this.serviceHandler.versionDetail({
                    id: this.$route.params.scriptVersionId,
                }).then((data) => {
                    this.lastVersionScriptInfo = Object.freeze(data);
                });
            },
            handleShowSideslider (id) {
                this.selectScriptVersionId = id;
                this.showSideslider = true;
            },
            handleAllRetry () {
                this.fetchData();
            },
            handleRetry (row, index) {
                this.serviceHandler.scriptVersionSync({
                    scriptId: this.scriptId,
                    scriptVersionId: this.scriptVersionId,
                    steps: [
                        {
                            templateId: row.templateId,
                            stepId: row.stepId,
                        },
                    ],
                }).then((data) => {
                    this.data.splice(index, 1, data[0]);
                });
            },
            statusFilterMethod (value, row, column) {
                return row.scriptStatus === value;
            },
            handleFinish () {
                this.routerBack();
            },
            routerBack () {
                window.changeAlert = !this.isRetryAllDisable;
                leaveConfirm(I18n.t('script.部分作业模板同步失败，请留意'))
                    .then(() => {
                        const routerName = this.publicScript ? 'publicScriptVersion' : 'scriptVersion';
                        this.$router.push({
                            name: routerName,
                            params: {
                                id: this.scriptId,
                            },
                        });
                    });
            },
        },
    };
</script>
<style lang="postcss">
    .script-manage-sync-task-page {
        .sync-script-record {
            &:hover {
                .template-link {
                    opacity: 1;
                }
            }
        }

        .template-link {
            font-size: 12px;
            opacity: 0;
        }

        .retry-btn {
            display: flex;
            align-items: center;
            justify-content: flex-end;
            margin-bottom: 20px;
        }

        .table-top {
            height: 42px;
            padding: 0 20px;
            margin-top: 16px;
            font-size: 12px;
            line-height: 42px;
            background-color: #f0f1f5;
        }

        .version-sum {
            color: #a3a6af;
        }

        .status-action {
            display: inline-block;
            width: 46px;
            height: 16px;
            font-size: 12px;
            color: #9fa2ac;
            text-align: center;
            background-color: #f0f1f5;
        }

        .failed-sync-status {
            border-bottom: 1px dashed #c4c6cc;
        }

        .try-show {
            display: visible;
            padding-left: 10px;
        }

        .try-hide {
            display: none;
        }
    }
</style>

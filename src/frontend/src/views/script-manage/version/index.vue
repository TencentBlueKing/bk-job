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
    <div class="script-manage-version-page">
        <script-basic />
        <div class="version-list-wraper">
            <list-action-layout>
                <bk-button
                    ref="newVersion"
                    @click="handlNewVersion">
                    {{ $t('script.新建版本') }}
                </bk-button>
                <bk-button
                    :disabled="disableDiff"
                    @click="handlShowDiff">
                    {{ $t('script.版本对比') }}
                </bk-button>
                <template #right>
                    <jb-search-select
                        :data="searchSelect"
                        :placeholder="$t('script.直接输入 版本号 或 更新人 进行全局模糊搜索')"
                        :show-condition="false"
                        style="width: 420px;"
                        @on-change="handleSearch" />
                </template>
            </list-action-layout>
            <div ref="list">
                <layout
                    :flod="isListFlod"
                    @on-flod="handleLayoutFlod">
                    <bk-table
                        v-if="tableHeight"
                        class="script-version-list"
                        :data="renderList"
                        :max-height="tableHeight"
                        :outer-border="false"
                        :row-class-name="rowClassName"
                        :size="tableSize"
                        @row-click="handleRowSelect"
                        @sort-change="handleSortChange">
                        <bk-table-column
                            key="selection"
                            align="center"
                            :resizable="false"
                            width="47">
                            <template slot-scope="{ row }">
                                <bk-checkbox
                                    :checked="isRowChecked(row)"
                                    :disabled="!isRowSelectable(row)"
                                    @change="checked => handleSelectionChange(row, checked)" />
                            </template>
                        </bk-table-column>
                        <bk-table-column
                            v-if="allColumnMap.scriptVersionId"
                            key="scriptVersionId"
                            align="left"
                            :label="$t('script.版本 ID')"
                            prop="scriptVersionId"
                            width="100" />
                        <bk-table-column
                            v-if="allColumnMap.version"
                            key="version"
                            align="left"
                            :label="$t('script.版本号.colHead')"
                            prop="version"
                            show-overflow-tooltip
                            :sortable="true">
                            <template slot-scope="{ row }">
                                <a @click="handleShowDetail(row)">{{ row.version || '--' }}</a>
                            </template>
                        </bk-table-column>
                        <bk-table-column
                            v-if="allColumnMap.relatedTaskNum"
                            key="relatedTaskNum"
                            align="right"
                            :label="$t('script.被引用.colHead')"
                            prop="relatedTaskNum"
                            :render-header="renderHeader"
                            width="150">
                            <template slot-scope="{ row }">
                                <bk-button
                                    v-bk-tooltips.allowHtml="`
                                    <div>${$t('script.作业模板引用')}: ${row.relatedTaskTemplateNum}</div>
                                    <div>${$t('script.执行方案引用')}: ${row.relatedTaskPlanNum}</div>`"
                                    class="mr20"
                                    text
                                    @click="handleShowRelated(row)">
                                    <span>
                                        {{ row.relatedTaskTemplateNum }}
                                    </span>
                                    <span> / </span>
                                    <span>
                                        {{ row.relatedTaskPlanNum }}
                                    </span>
                                </bk-button>
                            </template>
                        </bk-table-column>
                        <bk-table-column
                            v-if="allColumnMap.lastModifyUser"
                            key="lastModifyUser"
                            align="left"
                            :label="$t('script.更新人.colHead')"
                            prop="lastModifyUser"
                            width="160" />
                        <bk-table-column
                            v-if="allColumnMap.lastModifyTime"
                            key="lastModifyTime"
                            align="left"
                            :label="$t('script.更新时间')"
                            prop="lastModifyTime"
                            :sortable="true"
                            width="200" />
                        <bk-table-column
                            v-if="allColumnMap.statusDesc"
                            key="statusDesc"
                            align="left"
                            :label="$t('script.状态')"
                            prop="statusDesc"
                            :sortable="true"
                            width="120">
                            <template slot-scope="{ row }">
                                <span v-html="row.statusHtml" />
                                <Icon
                                    v-show="row.scriptVersionId === selectVersionId"
                                    class="select-flag"
                                    type="arrow-full-right" />
                            </template>
                        </bk-table-column>
                        <bk-table-column
                            v-if="!isListFlod"
                            key="action"
                            align="left"
                            :label="$t('script.操作')"
                            prop="action"
                            width="200">
                            <div
                                slot-scope="{ row }"
                                @click.stop="">
                                <jb-popover-confirm
                                    v-if="!row.isOnline"
                                    class="mr10"
                                    :confirm-handler="() => handleOnline(row.id, row.scriptVersionId)"
                                    :content="$t('script.上线后，之前的线上版本将被置为「已下线」状态，但不影响作业使用')"
                                    :disabled="row.isDisabledOnline"
                                    :title="$t('script.确定上线该版本？')">
                                    <auth-button
                                        auth="script/edit"
                                        :disabled="row.isDisabledOnline"
                                        :permission="row.canManage"
                                        :resource-id="row.id"
                                        text>
                                        {{ $t('script.上线') }}
                                    </auth-button>
                                </jb-popover-confirm>
                                <jb-popover-confirm
                                    v-if="row.isOnline"
                                    class="mr10"
                                    :confirm-handler="() => handleOffline(row.id, row.scriptVersionId)"
                                    :content="$t('script.一旦禁用成功，不可恢复！且线上引用该版本的作业步骤都会无法执行，请务必谨慎操作！')"
                                    :title="$t('script.确定禁用该版本？')">
                                    <auth-button
                                        auth="script/edit"
                                        :permission="row.canManage"
                                        :resource-id="row.id"
                                        text>
                                        {{ $t('script.禁用') }}
                                    </auth-button>
                                </jb-popover-confirm>
                                <auth-button
                                    v-if="row.isDraft"
                                    auth="script/edit"
                                    class="mr10"
                                    :permission="row.canManage"
                                    :resource-id="row.id"
                                    text
                                    @click="handleEdit(row)">
                                    {{ $t('script.编辑') }}
                                </auth-button>
                                <span
                                    v-if="!row.isDraft"
                                    class="mr10"
                                    :tippy-tips="isCopyCreateDisabled ? $t('script.已有[未上线]版本') : ''">
                                    <auth-button
                                        auth="script/clone"
                                        :disabled="isCopyCreateDisabled"
                                        :permission="row.canClone"
                                        :resource-id="row.id"
                                        text
                                        @click="handleToggleCopyCreate(row)">
                                        {{ $t('script.复制并新建') }}
                                    </auth-button>
                                </span>
                                <auth-button
                                    v-if="row.isOnline"
                                    auth="script/execute"
                                    :disabled="row.isExecuteDisable"
                                    :permission="row.canManage"
                                    :resource-id="row.id"
                                    text
                                    @click="handleGoExce(row)">
                                    {{ $t('script.去执行') }}
                                </auth-button>
                                <span
                                    v-if="!isPublicScript"
                                    :tippy-tips="!row.syncEnabled ? $t('script.所有关联作业模板已是当前版本') : ''">
                                    <auth-button
                                        v-if="row.isOnline"
                                        auth="script/edit"
                                        class="ml10"
                                        :disabled="!row.syncEnabled"
                                        :permission="row.canManage"
                                        :resource-id="row.id"
                                        text
                                        @click="handleSync(row)">
                                        {{ $t('script.同步') }}
                                    </auth-button>
                                </span>
                                <jb-popover-confirm
                                    v-if="row.isVersionEnableRemove"
                                    :confirm-handler="() => handleRemove(row.scriptVersionId)"
                                    :content="$t('script.删除后不可恢复，请谨慎操作！')"
                                    :title="$t('script.确定删除该版本？')">
                                    <auth-button
                                        auth="script/delete"
                                        :permission="row.canManage"
                                        :resource-id="row.id"
                                        text>
                                        {{ $t('script.删除') }}
                                    </auth-button>
                                </jb-popover-confirm>
                            </div>
                        </bk-table-column>
                        <bk-table-column type="setting">
                            <bk-table-setting-content
                                :fields="tableColumn"
                                :selected="selectedTableColumn"
                                :size="tableSize"
                                @setting-change="handleSettingChange" />
                        </bk-table-column>
                    </bk-table>
                    <template slot="flod">
                        <component
                            :is="curCom"
                            :script-info="selectVersion"
                            :script-version-list="data"
                            @on-create="handleCreateSubmit"
                            @on-create-cancel="handleCreateCancel"
                            @on-create-change="handleCreateValueChange"
                            @on-delete="handleDeleteSubmit"
                            @on-edit="handleEditSubmit"
                            @on-edit-cancel="handleEditCancel"
                            @on-edit-change="handleScriptChangeSubmit"
                            @on-go-copy-create="handleToggleCopyCreate"
                            @on-go-edit="handleToggleEdit" />
                    </template>
                </layout>
            </div>
        </div>
        <div style="display: none;">
            <div
                id="newVersionDisableActions"
                style="padding: 16px 12px;">
                <div style="margin-bottom: 17px; font-size: 14px; line-height: 22px; color: #313238;">
                    {{ $t('script.已有[未上线]版本') }}
                </div>
                <div>
                    <bk-button
                        size="small"
                        style="margin-right: 8px;"
                        theme="primary"
                        @click="handleEditDraftVersion">
                        {{ $t('script.前往编辑') }}
                    </bk-button>
                    <bk-button
                        size="small"
                        @click="handleNewVersionClose">
                        {{ $t('script.取消') }}
                    </bk-button>
                </div>
            </div>
        </div>
        <element-teleport v-if="currentVersionName">
            <span> - {{ currentVersionName }}</span>
        </element-teleport>
        <Diff
            v-if="showDiff"
            :data="dataMemo"
            :new-version-id="diffInfo.newVersionId"
            :old-version-id="diffInfo.oldVersionId"
            :title="$t('script.版本对比')"
            @close="handleDiffClose"
            @on-change="handleDiffVersionChange" />
        <jb-sideslider
            :is-show.sync="showRelated"
            quick-close
            :show-footer="false"
            :title="$t('script.被引用.label')"
            :width="695">
            <script-related-info
                :info="relatedScriptInfo"
                mode="scriptVersionList" />
        </jb-sideslider>
        <jb-dialog
            v-model="isShowNewVersion"
            header-position="left"
            :mask-close="false"
            :ok-text="$t('script.确定')"
            :title="$t('script.新建版本')"
            :width="480">
            <new-version
                :version-list="data"
                @on-close="handleNewVersionClose"
                @on-create="handleToggleCopyCreate" />
        </jb-dialog>
    </div>
</template>
<script>
    import Tippy from 'bk-magic-vue/lib/utils/tippy';
    import _ from 'lodash';

    import NotifyService from '@service/notify';
    import PublicScriptService from '@service/public-script-manage';
    import ScriptService from '@service/script-manage';

    import ScriptModel from '@model/script/script';

    import {
        checkPublicScript,
        encodeRegexp,
        genDefaultScriptVersion,
        getOffset,
        leaveConfirm,
    } from '@utils/assist';
    import { listColumnsCache } from '@utils/cache-helper';

    import JbPopoverConfirm from '@components/jb-popover-confirm';
    import JbSearchSelect from '@components/jb-search-select';
    import ListActionLayout from '@components/list-action-layout';

    import CopyCreate from '../common/copy-create';
    import DetailScript from '../common/detail/index';
    import Edit from '../common/edit';
    import ScriptRelatedInfo from '../common/script-related-info';

    import Diff from './components/diff';
    import Layout from './components/layout';
    import NewVersion from './components/new-version';
    import ScriptBasic from './components/script-basic';

    import I18n from '@/i18n';

    const TABLE_COLUMN_CACHE = 'script_version_list_columns';

    export default {
        name: 'ScriptVersion',
        components: {
            ListActionLayout,
            JbPopoverConfirm,
            JbSearchSelect,
            ScriptRelatedInfo,
            Diff,
            Layout,
            DetailScript,
            Edit,
            CopyCreate,
            ScriptBasic,
            NewVersion,
        },
        data () {
            return {
                isLoading: false,
                isListFlod: false,
                isShowNewVersion: false,
                showDiff: false,
                data: [],
                dataAppendList: [],
                scriptDetailInfo: {},
                tableHeight: '',
                selectedTableColumn: [],
                selectVersionId: '',
                choosedMap: {},
                showRelated: false,
                relatedScriptInfo: [],
                displayCom: '',
                tableSize: 'small',
            };
        },
        computed: {
            /**
             * @desc 骨架屏 Loading
             * @returns { Boolean }
             */
            isSkeletonLoading () {
                return this.isLoading;
            },
            /**
             * @desc 脚本展示状态组件
             * @returns { Object }
             */
            curCom () {
                if (!this.displayCom) {
                    return 'div';
                }
                const comMap = {
                    detail: DetailScript,
                    edit: Edit,
                    copyCreate: CopyCreate,
                };
                return comMap[this.displayCom];
            },
            /**
             * @desc 列表数据
             * @returns { Array }
             */
            renderList () {
                return [
                    ...this.dataAppendList,
                    ...this.data,
                ];
            },
            /**
             * @desc 已存在未上线版本不允许新建版本
             * @returns { Boolean }
             */
            isCopyCreateDisabled () {
                return !!_.find(this.dataMemo, scriptVersion => scriptVersion.isDraft);
            },
            /**
             * @desc 选中的脚本版本
             * @returns { Object }
             */
            selectVersion () {
                const current = _.find(this.renderList, _ => _.scriptVersionId === this.selectVersionId);
                if (!current) {
                    return {};
                }
                return current;
            },
            /**
             * @desc 需要选中两个脚本才能对比
             * @returns { Boolean }
             */
            disableDiff () {
                return Object.keys(this.choosedMap).length < 2;
            },
            /**
             * @desc 将要对比的脚本数据
             * @returns { Object }
             */
            diffInfo () {
                const diffVersion = Object.keys(this.choosedMap);
                return {
                    oldVersionId: parseInt(diffVersion[0], 10) || '',
                    newVersionId: parseInt(diffVersion[1], 10) || '',
                };
            },
            /**
             * @desc 当前脚本版本的 name
             * @returns { String }
             */
            currentVersionName () {
                if (this.dataMemo.length < 1) {
                    return '';
                }
                return this.dataMemo[0].name;
            },
            /**
             * @desc 展示的列表列
             * @returns { Object }
             */
            allColumnMap () {
                if (this.isListFlod) {
                    return {
                        version: true,
                        statusDesc: true,
                    };
                }
                return this.selectedTableColumn.reduce((result, item) => {
                    result[item.id] = true;
                    return result;
                }, {});
            },
        },
        watch: {
            displayCom (displayCom) {
                // 当右侧详情面板切换时，需要重置dataAppendList
                if (displayCom !== 'copyCreate') {
                    this.dataAppendList = [];
                }
            },
        },
        created () {
            // 缓存状态切换中的中间状态
            this.lastSelectScriptVersionId = '';
            // 缓存脚本版本的完整数据列表——用于脚本搜索
            this.dataMemo = [];

            this.isPublicScript = checkPublicScript(this.$route);
            this.serviceHandler = this.isPublicScript ? PublicScriptService : ScriptService;
            this.scriptId = this.$route.params.id;

            this.fetchData(true);
            
            this.searchSelect = [
                {
                    name: I18n.t('script.版本号.colHead'),
                    id: 'version',
                    default: true,
                },
                {
                    name: I18n.t('script.更新人.colHead'),
                    id: 'lastModifyUser',
                    remoteMethod: NotifyService.fetchUsersOfSearch,
                    inputInclude: true,
                },
            ];
            this.tableColumn = [
                {
                    id: 'scriptVersionId',
                    label: I18n.t('script.版本 ID'),
                },
                {
                    id: 'version',
                    label: I18n.t('script.版本号.colHead'),
                    disabled: true,
                },
                {
                    id: 'relatedTaskNum',
                    label: I18n.t('script.被引用.colHead'),
                    disabled: true,
                },
                {
                    id: 'lastModifyUser',
                    label: I18n.t('script.更新人.colHead'),
                },
                {
                    id: 'lastModifyTime',
                    label: I18n.t('script.更新时间'),
                },
                {
                    id: 'statusDesc',
                    label: I18n.t('script.状态'),
                    disabled: true,
                },
            ];
            const columnsCache = listColumnsCache.getItem(TABLE_COLUMN_CACHE);
            if (columnsCache) {
                this.selectedTableColumn = Object.freeze(columnsCache.columns);
                this.tableSize = columnsCache.size;
            } else {
                this.selectedTableColumn = Object.freeze([
                    { id: 'version' },
                    { id: 'relatedTaskNum' },
                    { id: 'lastModifyUser' },
                    { id: 'lastModifyTime' },
                    { id: 'statusDesc' },
                ]);
            }
        },
        mounted () {
            this.calcTableHeight();
            window.addEventListener('resize', this.calcTableHeight);
            this.$once('hook:beforeDestroy', () => {
                window.removeEventListener('resize', this.calcTableHeight);
            });
        },
        methods: {
            /**
             * @desc 获取脚本版本列表数据
             * @param {Boolean} isFirst 第一次调用
             *
             * 需要解析url参数
             */
            fetchData (isFirst) {
                this.isLoading = true;
                return this.serviceHandler.scriptVersionList({
                    id: this.scriptId,
                }, {
                    permission: 'page',
                }).then((data) => {
                    this.dataMemo = Object.freeze([
                        ...data,
                    ]);
                    this.data = Object.freeze(data);

                    if (isFirst) {
                        this.parseUrl();
                    }
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc URL链接指定了脚本版本
             *
             * 进入脚本详情模式
             */
            parseUrl () {
                const scriptVersionId = parseInt(this.$route.query.scriptVersionId, 10);
                if (scriptVersionId) {
                    this.handleShowDetail({
                        scriptVersionId,
                    });
                }
            },
            /**
             * @desc 计算表格高度
             */
            calcTableHeight () {
                const { top } = getOffset(this.$refs.list);
                const windowHeight = window.innerHeight;
                this.tableHeight = windowHeight - top - 20;
            },
            
            rowClassName ({ row }) {
                return row.scriptVersionId === this.selectVersionId ? 'active' : '';
            },
            /**
             * @desc 新建脚本版本
             */
            handlNewVersion () {
                if (!this.isCopyCreateDisabled) {
                    this.isShowNewVersion = true;
                    return;
                }
                // 已有未上线脚本给出提示
                if (!this.newVersionPopperInstance) {
                    this.newVersionPopperInstance = Tippy(this.$refs.newVersion.$el, {
                        arrow: true,
                        width: 192,
                        placement: 'bottom-start',
                        trigger: 'manual',
                        theme: 'light',
                        interactive: true,
                        animation: 'slide-toggle',
                        content: document.querySelector('#newVersionDisableActions'),
                        boundary: 'window',
                        distance: 20,
                        zIndex: window.__bk_zIndex_manager.nextZIndex(), // eslint-disable-line no-underscore-dangle
                    });
                    this.$once('hook:beforeDestroy', () => {
                        this.newVersionPopperInstance.hide();
                        this.newVersionPopperInstance.destroy();
                    });
                }
                this.newVersionPopperInstance.show();
            },
            /**
             * @desc 编辑最新的未上线脚本版本
             */
            handleEditDraftVersion () {
                const lastestDraftScriptVersion = _.find(this.dataMemo, scriptVersion => scriptVersion.isDraft);
                this.handleEdit(lastestDraftScriptVersion);
                this.newVersionPopperInstance.hide();
            },
            /**
             * @desc 取消新建版本
             */
            handleNewVersionClose () {
                this.newVersionPopperInstance && this.newVersionPopperInstance.hide();
                this.isShowNewVersion = false;
            },
            /**
             * @desc 列表搜索
             * @param {Object} payload 搜索字段
             */
            handleSearch (payload) {
                let list = this.dataMemo;
                Object.keys(payload).forEach((key) => {
                    const reg = new RegExp(encodeRegexp(payload[key]));
                    list = list.filter(item => reg.test(item[key]));
                });
                this.data = Object.freeze(list);
                this.handleLayoutFlod();
            },
            /**
             * @desc 列表排序
             * @param {Object} payload 排序字段
             */
            handleSortChange (payload) {
                if (payload.prop) {
                    const key = payload.prop;
                    const list = [
                        ...this.data,
                    ];
                    list.sort((preItem, nextItem) => {
                        if (payload.order === 'descending') {
                            return preItem[key] - nextItem[key];
                        }
                        return nextItem[key] - preItem[key];
                    });
                }
            },
            /**
             * @desc 自定义列表配置
             */
            handleSettingChange ({ fields, size }) {
                this.selectedTableColumn = Object.freeze(fields);
                this.tableSize = size;
                listColumnsCache.setItem(TABLE_COLUMN_CACHE, {
                    columns: fields,
                    size,
                });
            },
            /**
             * @desc 布局切换
             */
            handleLayoutFlod () {
                this.displayCom = '';
                this.isListFlod = false;
                if (this.selectVersionId === -1) {
                    // 还原复制并新建的占位数据
                    this.handleCreateCancel();
                }
                this.selectVersionId = '';
            },
            /**
             * @desc 选中一行
             * @param {Object} payload 脚本数据
             * @param {Boolean} checked 选择状态
             */
            handleSelectionChange (payload, checked) {
                if (checked) {
                    this.choosedMap[payload.scriptVersionId] = true;
                } else {
                    delete this.choosedMap[payload.scriptVersionId];
                }
                this.choosedMap = { ...this.choosedMap };
            },
            /**
             * @desc 点击版本名查看详情
             * @param {Object} row 脚本数据
             */
            handleShowDetail (row) {
                leaveConfirm()
                    .then(() => {
                        this.selectVersionId = row.scriptVersionId;
                        this.isListFlod = true;
                        this.displayCom = 'detail';
                    });
            },
            /**
             * @desc 鼠标选中一行
             * @param {Object} row 脚本数据
             */
            handleRowSelect (row) {
                if (this.isListFlod) {
                    this.handleShowDetail(row);
                }
            },
            /**
             * @desc 下线
             * @param {Number} id 脚本id
             * @param {String} versionId
             */
            handleOffline (id, versionId) {
                return this.serviceHandler.scriptVersionOffline({
                    id,
                    versionId,
                }).then(() => {
                    this.messageSuccess(I18n.t('script.操作成功'));
                    this.fetchData();
                });
            },
            /**
             * @desc 上线
             * @param {Number} id 脚本id
             * @param {String} versionId 脚本版本id
             */
            handleOnline (id, versionId) {
                return this.serviceHandler.scriptVersionOnline({
                    id,
                    versionId,
                }).then(() => {
                    this.messageSuccess(I18n.t('script.操作成功'));
                    this.fetchData();
                });
            },
            /**
             * @desc 删除
             * @param {String} versionId 脚本版本id
             */
            handleRemove (versionId) {
                return this.serviceHandler.scriptVersionRemove({
                    versionId,
                }).then(() => {
                    this.messageSuccess(I18n.t('script.删除成功'));
                    this.fetchData();
                });
            },
            /**
             * @desc 执行
             * @param {Object} payload 脚本数据
             */
            handleGoExce (payload) {
                this.$router.push({
                    name: 'fastExecuteScript',
                    params: {
                        taskInstanceId: 0,
                        scriptVersionId: payload.scriptVersionId,
                    },
                    query: {
                        from: 'scriptDetail',
                    },
                });
            },
            /**
             * @desc 同步
             * @param {Object} row 脚本数据
             */
            handleSync (row) {
                const routerName = this.isPublicScript ? 'scriptPublicSync' : 'scriptSync';
                
                this.$router.push({
                    name: routerName,
                    params: {
                        scriptId: row.id,
                        scriptVersionId: row.scriptVersionId,
                    },
                });
            },
            /**
             * @desc 编辑脚本版本
             * @param {Object} scriptInfo 脚本数据
             */
            handleEdit (scriptInfo) {
                this.selectVersionId = scriptInfo.scriptVersionId;
                this.displayCom = 'edit';
                this.isListFlod = true;
                this.isShowNewVersion = false;
            },
            /**
             * @desc 显示脚本引用列表
             * @param {String} mode 引用的模板、执行方案
             * @param {Object} payload 脚本数据
             */
            handleShowRelated (payload) {
                this.showRelated = true;
                this.relatedScriptInfo = payload;
            },
            /**
             * @desc 显示脚本差异弹层
             * @param {Object} payload
             */
            handlShowDiff () {
                this.showDiff = true;
            },
            /**
             * @desc 脚本对比版本切换
             * @param {Object} payload
             */
            handleDiffVersionChange (payload) {
                this.choosedMap = payload;
            },
            /**
             * @desc 关闭脚本对比弹层
             */
            handleDiffClose () {
                this.showDiff = false;
            },
            /**
             * @desc 编辑成功刷新列表
             */
            handleScriptChangeSubmit () {
                this.fetchData();
            },
            /**
             * @desc 删除成功
             *
             * 更新布局、刷新列表
             */
            handleDeleteSubmit () {
                this.handleLayoutFlod();
                this.fetchData();
            },
            /**
             * @desc 切换到复制并新建状态
             */
            handleToggleCopyCreate ({ scriptVersionId }) {
                this.selectVersionId = scriptVersionId;
                const currentScriptVersion = _.find(this.dataMemo, _ => _.scriptVersionId === scriptVersionId);
                const newScriptVersion = new ScriptModel({
                    ...currentScriptVersion,
                    scriptVersionId: -1,
                    status: -1,
                    version: genDefaultScriptVersion(),
                });
                this.isListFlod = true;
                this.lastSelectScriptVersionId = this.selectVersionId;
                this.selectVersionId = -1;
                this.displayCom = 'copyCreate';
                this.dataAppendList = Object.freeze([
                    newScriptVersion,
                ]);
            },
            /**
             * @desc 编辑脚本
             */
            handleToggleEdit () {
                this.displayCom = 'edit';
            },
            /**
             * @desc 取消新建脚本版本
             */
            handleCreateCancel () {
                this.displayCom = 'detail';
                this.selectVersionId = this.lastSelectScriptVersionId;
            },
            /**
             * @desc 新建成功
             *
             * 并选中新版本
             */
            handleCreateSubmit ({ scriptVersionId }) {
                this.fetchData()
                    .then(() => {
                        setTimeout(() => {
                            this.displayCom = 'detail';
                            this.selectVersionId = scriptVersionId;
                        });
                    });
            },
            /**
             * @desc 新建脚本过程中字段值改变
             * @param {Number} scriptVersionId 脚本版本id
             * @param {Object} payload 脚本数据
             */
            handleCreateValueChange (scriptVersionId, payload) {
                const data = [
                    ...this.dataAppendList,
                ];
                const currentScriptVersion = _.find(data, _ => _.scriptVersionId === scriptVersionId);
                if (currentScriptVersion) {
                    Object.assign(currentScriptVersion, payload);
                    this.dataAppendList = Object.freeze(data);
                }
            },
            /**
             * @desc 编辑成功
             *
             * 显示脚本详情
             * 刷新列表数据
             */
            handleEditSubmit () {
                this.displayCom = 'detail';
                this.fetchData();
            },
            /**
             * @desc 编辑取消
             */
            handleEditCancel () {
                this.displayCom = 'detail';
            },
            
            /**
             * @desc 判断是否可以选中
             * @param {Object} row 脚本数据
             */
            isRowSelectable (row) {
                if (this.disableDiff) {
                    return true;
                }
                return this.choosedMap[row.scriptVersionId];
            },
            /**
             * @desc 判断是否选中
             * @param {Object} row 脚本数据
             */
            isRowChecked (row) {
                return this.choosedMap[row.scriptVersionId];
            },
            /**
             * @desc 自定义表头
             */
            renderHeader (h, data) {
                return (
                    <span>
                        <span>{ data.column.label }</span>
                        <bk-popover>
                            <icon
                                type="circle-italics-info"
                                style="margin-left: 8px; font-size: 12px;" />
                            <div slot="content">
                                <div>{ I18n.t('script.显示被作业引用的次数') }</div>
                                <div>{ I18n.t('script.显示被执行方案引用的次数') }</div>
                            </div>
                        </bk-popover>
                    </span>
                );
            },
            /**
             * @desc 路由回退
             */
            routerBack () {
                if (this.isPublicScript) {
                    this.$router.push({
                        name: 'publicScriptList',
                    });
                    return;
                }
                this.$router.push({
                    name: 'scriptList',
                });
            },
        },
    };
</script>
<style lang='postcss'>
    .script-manage-version-page {
        .version-list-wraper {
            margin-top: 20px;
        }

        .script-version-list {
            .bk-table-row {
                &.active {
                    background: #eff5ff;
                }

                &.active,
                &.hover-row {
                    span[data-script-status] {
                        background: #e6e7eb !important;
                    }

                    span[data-script-status="1"] {
                        background: #daebde !important;
                    }

                    span[data-script-status="-1"] {
                        background: #ffe8c3 !important;
                    }
                }
            }

            .select-flag {
                float: right;
                margin-top: 3px;
                color: #a3c5fd;
            }
        }
    }
</style>

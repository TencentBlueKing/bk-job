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
    <div class="task-manage-plan-page">
        <list-action-layout>
            <bk-button
                v-test="{ type: 'button', value: 'createPlan' }"
                class="w120"
                theme="primary"
                @click="handleCreatePlan">
                {{ $t('template.新建') }}
            </bk-button>
            <span v-bk-tooltips="batchSyncDisableTips">
                <bk-button
                    v-test="{ type: 'button', value: 'batchSyncPlan' }"
                    :disabled="!!batchSyncDisableTips"
                    @click="handleSyncBatch">
                    {{ $t('template.批量同步') }}
                </bk-button>
            </span>
            <span v-bk-tooltips="batchEditGlobalVariableTips">
                <bk-button
                    v-test="{ type: 'button', value: 'batchEditPlanValue' }"
                    :disabled="!!batchEditGlobalVariableTips"
                    @click="handleBatchEditGlobalVariable">
                    {{ $t('template.批量编辑变量值') }}
                </bk-button>
            </span>
            <template #right>
                <jb-search-select
                    ref="search"
                    :append-value="searchValue"
                    :data="searchData"
                    :placeholder="$t('template.输入 ID、执行方案名、作业模板名、更新人 或 创建人 进行搜索...')"
                    style="width: 420px;"
                    @on-change="handleSearch" />
                <bk-button @click="handleMyPlan">
                    {{ $t('template.我的方案') }}
                </bk-button>
            </template>
        </list-action-layout>
        <layout
            :flod="isListFlod"
            @on-flod="handleLayoutFlod">
            <render-list
                ref="list"
                v-test="{ type: 'list', value: 'plan' }"
                :data-source="listDataSource"
                :outer-border="false"
                :pagination-small="paginationSmall"
                :row-class-name="caclRowClassName"
                :search-control="() => $refs.search"
                selectable
                :size="tableSize"
                @on-selection-change="handleSelection">
                <div
                    v-if="isCreatePlan"
                    slot="prepend"
                    class="create-plan-placeholder"
                    :class="{ active: selectPlanInfo.id === -1 }">
                    <div class="name-box">
                        {{ newPlanName || '--' }}
                    </div>
                    <Icon
                        style="font-size: 26px; color: #ff9c01;"
                        svg
                        type="new-3" />
                </div>
                <bk-table-column
                    v-if="allRenderColumnMap.id"
                    key="id"
                    align="left"
                    label="ID"
                    prop="id"
                    width="120" />
                <bk-table-column
                    v-if="allRenderColumnMap.name"
                    key="name"
                    align="left"
                    class-name="plan-name-column"
                    :label="$t('template.执行方案名称')"
                    min-width="300"
                    prop="name">
                    <template slot-scope="{ row }">
                        <auth-component
                            auth="job_plan/view"
                            :permission="row.canView"
                            :resource-id="row.id"
                            style="width: 100%;">
                            <div
                                class="plan-name-box"
                                :class="{
                                    active: selectPlanInfo.id === row.id,
                                }"
                                @click="handlePlanSelect(row)">
                                <div class="name-wraper">
                                    <div
                                        v-bk-overflow-tips
                                        class="name-text">
                                        {{ row.name }}
                                    </div>
                                    <router-link
                                        v-if="row.cronJobCount > 0"
                                        v-bk-tooltips.html="`
                                            <div>${$t('template.有')} ${row.cronJobCount} ${$t('template.个定时任务')}</div>
                                            <div>${$t('template.点击前往查看')}</div>
                                        `"
                                        class="cron-job-tag"
                                        target="_blank"
                                        :to="{
                                            name: 'cronList',
                                            query: {
                                                planId: row.id,
                                            },
                                        }">
                                        <Icon
                                            svg
                                            type="job-timing" />
                                        <span style="margin-left: 2px;">{{ row.cronJobCount }}</span>
                                    </router-link>
                                    <span
                                        v-if="row.needUpdate"
                                        class="update-flag">
                                        <Icon
                                            svg
                                            :tippy-tips="$t('template.未同步')"
                                            type="sync-8" />
                                    </span>
                                </div>
                                <Icon
                                    class="collection-flag"
                                    :class="{ favored: row.favored }"
                                    type="collection"
                                    @click.stop="handleCollection(row)" />
                                <Icon
                                    v-if="selectPlanInfo.id === row.id"
                                    class="select-flag"
                                    type="arrow-full-right" />
                            </div>
                            <div
                                slot="forbid"
                                class="plan-name-box"
                                :class="{
                                    active: selectPlanInfo.id === row.id,
                                }">
                                <div class="name-wraper">
                                    <div
                                        v-bk-overflow-tips
                                        class="name-text">
                                        {{ row.name }}
                                    </div>
                                    <span
                                        v-if="row.cronJobCount > 0"
                                        v-bk-tooltips.html="`
                                            <div>${$t('template.有')} ${row.cronJobCount} ${$t('template.个定时任务')}</div>
                                            <div>${$t('template.点击前往查看')}</div>
                                        `"
                                        class="cron-job-tag">
                                        <Icon
                                            svg
                                            type="job-timing" />
                                        <span style="margin-left: 2px;">{{ row.cronJobCount }}</span>
                                    </span>
                                    <span
                                        v-if="row.needUpdate"
                                        class="update-flag">
                                        <Icon
                                            svg
                                            :tippy-tips="$t('template.未同步')"
                                            type="sync-8" />
                                    </span>
                                </div>
                                <Icon
                                    class="collection-flag"
                                    :class="{ favored: row.favored }"
                                    type="collection"
                                    @click.stop="handleCollection(row)" />
                            </div>
                        </auth-component>
                    </template>
                </bk-table-column>
                <bk-table-column
                    v-if="allRenderColumnMap.templateName"
                    key="templateName"
                    align="left"
                    :label="$t('template.所属作业模板')"
                    min-width="200"
                    prop="templateName"
                    show-overflow-tooltip />
                <bk-table-column
                    v-if="allRenderColumnMap.lastModifyUser"
                    key="lastModifyUser"
                    align="left"
                    :label="$t('template.更新人.colHead')"
                    prop="lastModifyUser"
                    width="160" />
                <bk-table-column
                    v-if="allRenderColumnMap.lastModifyTime"
                    key="lastModifyTime"
                    align="left"
                    :label="$t('template.更新时间')"
                    prop="lastModifyTime"
                    width="180" />
                <bk-table-column
                    v-if="allRenderColumnMap.creator"
                    key="creator"
                    align="left"
                    :label="$t('template.创建人')"
                    prop="creator"
                    width="120" />
                <bk-table-column
                    v-if="allRenderColumnMap.createTime"
                    key="createTime"
                    align="left"
                    :label="$t('template.创建时间')"
                    prop="createTime"
                    width="180" />
                <bk-table-column
                    v-if="!isListFlod"
                    key="action"
                    align="left"
                    fixed="right"
                    :label="$t('template.操作')"
                    prop="statusText"
                    :resizable="false"
                    width="120">
                    <template slot-scope="{ row }">
                        <bk-button
                            v-test="{ type: 'button', value: 'execPlan' }"
                            class="mr10"
                            text
                            @click="handleExecute(row)">
                            {{ $t('template.去执行') }}
                        </bk-button>
                        <span :tippy-tips="row.needUpdate ? '' : $t('template.无需同步')">
                            <auth-button
                                v-test="{ type: 'button', value: 'syncPlan' }"
                                auth="job_plan/sync"
                                class="mr10"
                                :disabled="!row.needUpdate"
                                :permission="row.canEdit"
                                :resource-id="row.id"
                                text
                                @click="handleUpdate(row)">
                                {{ $t('template.去同步') }}
                            </auth-button>
                        </span>
                        <list-operation-extend>
                            <div
                                v-test="{ type: 'link', value: 'createCrontab' }"
                                class="action-item"
                                @click="handleGoCreateCronJob(row)">
                                {{ $t('template.定时执行') }}
                            </div>
                            <auth-component
                                auth="job_plan/edit"
                                :permission="row.canEdit"
                                :resource-id="row.id">
                                <div
                                    v-test="{ type: 'button', value: 'editPlan' }"
                                    class="action-item"
                                    @click="handleEdit(row)">
                                    {{ $t('template.编辑') }}
                                </div>
                                <div
                                    slot="forbid"
                                    v-test="{ type: 'button', value: 'editPlan' }"
                                    class="action-item">
                                    {{ $t('template.编辑') }}
                                </div>
                            </auth-component>
                            <jb-popover-confirm
                                class="action-del"
                                :confirm-handler="() => handleDelete(row)"
                                :content="$t('template.若已设置了定时任务，需要先删除才能操作')"
                                :title="$t('template.确定删除该执行方案？')">
                                <auth-component
                                    auth="job_plan/delete"
                                    :permission="row.canDelete"
                                    :resource-id="row.id">
                                    <div
                                        v-test="{ type: 'button', value: 'deletePlan' }"
                                        class="action-item">
                                        {{ $t('template.删除') }}
                                    </div>
                                    <div
                                        slot="forbid"
                                        v-test="{ type: 'button', value: 'deletePlan' }"
                                        class="action-item">
                                        {{ $t('template.删除') }}
                                    </div>
                                </auth-component>
                            </jb-popover-confirm>
                        </list-operation-extend>
                    </template>
                </bk-table-column>
                <bk-table-column
                    v-if="!isListFlod"
                    type="setting">
                    <bk-table-setting-content
                        :fields="tableColumn"
                        :selected="selectedTableColumn"
                        :size="tableSize"
                        @setting-change="handleSettingChange" />
                </bk-table-column>
            </render-list>
            <template slot="flod">
                <component
                    :is="planCom"
                    ref="planHandler"
                    v-bind="selectPlanInfo"
                    :bottom-offset="20"
                    :first-plan="isFirstTemplatePlan"
                    @on-create="handleCreateSubmit"
                    @on-delete="handlePlanDelete"
                    @on-edit="handleShowPlanEdit"
                    @on-edit-cancle="handleEditCancle"
                    @on-edit-success="handleEditSuccess"
                    @on-name-change="handleCreatePlanNameChange" />
            </template>
        </layout>
        <lower-component
            :custom="isShowTemplateSelect"
            level="custom">
            <template-select
                v-model="isShowTemplateSelect"
                @on-change="handleTemplateChange" />
        </lower-component>
        <lower-component
            :custom="isShowBatchGlobalVariable"
            level="custom">
            <batch-edit-global-variable
                v-model="isShowBatchGlobalVariable"
                :data="listSelect"
                @on-success="handleBatchEditGlobalVariableSuccess" />
        </lower-component>
    </div>
</template>
<script>
    /**
     * @desc 执行方案列表展示公用组件
     *
     * 用于作业模板详情展示指定作业模板的执行方案（固定搜索项作业模板名称）
     * 用执行方案列表展示所有执行方案列表
    */
    import NotifyService from '@service/notify';
    import TaskExecuteService from '@service/task-execute';
    import ExecPlanService from '@service/task-plan';
    import UserService from '@service/user';

    import { leaveConfirm } from '@utils/assist';
    import { listColumnsCache } from '@utils/cache-helper';

    import JbSearchSelect from '@components/jb-search-select';
    import ListActionLayout from '@components/list-action-layout';
    import ListOperationExtend from '@components/list-operation-extend';
    import RenderList from '@components/render-list';

    import PlanCreate from '../create';
    import PlanDetail from '../detail';
    import PlanEdit from '../edit';

    import BatchEditGlobalVariable from './components/batch-edit-gobal-variable';
    import Layout from './components/layout';
    import TemplateSelect from './components/template-select';

    import I18n from '@/i18n';

    const TABLE_COLUMN_CACHE = 'task_plan_list_columns';

    export default {
        name: '',
        components: {
            ListActionLayout,
            RenderList,
            JbSearchSelect,
            ListOperationExtend,
            Layout,
            PlanDetail,
            PlanEdit,
            TemplateSelect,
            BatchEditGlobalVariable,
        },
        data () {
            return {
                searchValue: [],
                listSelect: [],
                isShowTemplateSelect: false,
                isShowBatchGlobalVariable: false,
                planComType: '',
                newPlanName: '',
                isFirstTemplatePlan: false,
                selectPlanInfo: {
                    templateId: -1,
                    id: -1,
                },
                currentUser: {},
                selectedTableColumn: [],
                tableSize: 'small',
                paginationSmall: false,
            };
        },
        computed: {
            isLoading () {
                return this.$refs.list.isLoading;
            },
            allRenderColumnMap () {
                if (this.isListFlod) {
                    return {
                        name: true,
                    };
                }
                return this.selectedTableColumn.reduce((result, item) => {
                    result[item.id] = true;
                    return result;
                }, {});
            },
            planCom () {
                const planComMap = {
                    detail: PlanDetail,
                    edit: PlanEdit,
                    create: PlanCreate,
                };
                if (!planComMap[this.planComType]) {
                    return 'div';
                }
                return planComMap[this.planComType];
            },
            isCreatePlan () {
                return this.planComType === 'create';
            },
            isListFlod () {
                return Boolean(this.planComType);
            },
            /**
             * @desc 批量同步按钮禁用tips
             * 当所选执行方案中有执行方案中不需要同步或者有执行方案中没有权限同步，禁用批量同步操作
             */
            batchSyncDisableTips () {
                if (this.listSelect.length < 1) {
                    return I18n.t('template.请选择要同步的执行方案');
                }
                let needUpdate = true;
                let canEdit = true;
                this.listSelect.forEach((currentSelect) => {
                    if (!currentSelect.needUpdate) {
                        needUpdate = false;
                    }
                    if (!currentSelect.canEdit) {
                        canEdit = false;
                    }
                });
                if (!needUpdate) {
                    return I18n.t('template.已选结果中有执行方案中不需要同步');
                }
                if (!canEdit) {
                    return I18n.t('template.已选结果中有执行方案中没有权限同步');
                }
                return '';
            },
            /**
             * @desc 批量编辑全局变量按钮禁用tips
             * 当所选执行方案中有执行方案中没有权限编辑禁用批量编辑操作
             */
            batchEditGlobalVariableTips () {
                if (this.listSelect.length < 1) {
                    return I18n.t('template.请选择要编辑的执行方案');
                }
                let canEdit = true;
                // eslint-disable-next-line no-plusplus
                for (let i = 0; i < this.listSelect.length; i++) {
                    const currentSelect = this.listSelect[i];
                    if (!currentSelect.canEdit) {
                        canEdit = false;
                        break;
                    }
                }
                
                if (!canEdit) {
                    return I18n.t('template.已选结果中有执行方案中没有权限编辑');
                }
                return '';
            },
        },
        created () {
            this.parseUrl();

            this.fetchUserInfo();

            this.listDataSource = ExecPlanService.fetchAllPlan;
            
            // 查看指定作业模板的执行方案列表，不支持作业模板名称搜索
            this.searchData = [
                {
                    name: 'ID',
                    id: 'planId',
                    description: I18n.t('template.将覆盖其它条件'),
                    validate (values, item) {
                        const validate = (values || []).every(_ => /^(\d*)$/.test(_.name));
                        return !validate ? I18n.t('template.ID只支持数字') : true;
                    },
                },
                {
                    name: I18n.t('template.执行方案.colHead'),
                    id: 'planName',
                    default: true,
                },
                {
                    name: I18n.t('template.更新人.colHead'),
                    id: 'lastModifyUser',
                    remoteMethod: NotifyService.fetchUsersOfSearch,
                    inputInclude: true,
                },
                {
                    name: I18n.t('template.创建人'),
                    id: 'creator',
                    remoteMethod: NotifyService.fetchUsersOfSearch,
                    inputInclude: true,
                },
            ];
            if (!this.isViewTemplatePlanList) {
                this.searchData.splice(2, 0, {
                    name: I18n.t('template.作业模板名称'),
                    id: 'templateName',
                });
            }
            
            // 列表可显示列
            this.tableColumn = [
                {
                    id: 'id',
                    label: 'ID',
                },
                {
                    id: 'name',
                    label: I18n.t('template.执行方案.colHead'),
                    disabled: true,
                },
                {
                    id: 'templateName',
                    label: I18n.t('template.所属作业模板'),
                },
                {
                    id: 'lastModifyUser',
                    label: I18n.t('template.更新人.colHead'),
                },
                {
                    id: 'lastModifyTime',
                    label: I18n.t('template.更新时间'),
                },
                {
                    id: 'creator',
                    label: I18n.t('template.创建人'),
                },
                {
                    id: 'createTime',
                    label: I18n.t('template.创建时间'),
                },
            ];
            const columnsCache = listColumnsCache.getItem(TABLE_COLUMN_CACHE);
            if (columnsCache) {
                this.selectedTableColumn = Object.freeze(columnsCache.columns);
                this.tableSize = columnsCache.size;
            } else {
                this.selectedTableColumn = Object.freeze([
                    { id: 'id' },
                    { id: 'name' },
                    { id: 'lastModifyUser' },
                    { id: 'lastModifyTime' },
                ]);
            }
        },
        methods: {
            /**
             * @desc 执行方案列表
             *
             * 查看指定作业模板的执行方案列表
             *  - api请求固定作业模板id，url查询参数需要拼接viewPlanId表示当前正常查看的执行方案
             */
            fetchData () {
                const searchParams = {
                    ...this.searchParams,
                };
                if (this.templateId) {
                    searchParams.templateId = this.templateId;
                }
                this.$refs.list.$emit('onFetch', searchParams);
            },
            /**
             * @desc 登陆用户信息
             */
            fetchUserInfo () {
                UserService.fetchUserInfo()
                    .then((data) => {
                        this.currentUser = Object.freeze(data);
                    });
            },
            /**
             * @desc 解析 url 参数
             */
            parseUrl () {
                // 查看作业模板的执行方案
                this.isViewTemplatePlanList = this.$route.name === 'viewPlan';
                // 执行方案列表所属的作业模板
                this.templateId = '';
                
                if (this.isViewTemplatePlanList) {
                    // 查看指定作业模板的执行方案列表

                    // 解析路由查询参数viewPlanId，没有指定viewPlanId或者viewPlanId不存在列表数据中，默认赋值列表数据的第一个
                    let {
                        templateId,
                    } = this.$route.params;
                    templateId = Number(templateId);
                    // 记录 templateId
                    this.templateId = templateId;
                    
                    const {
                        viewPlanId,
                        mode,
                    } = this.$route.query;
                    // 默认显示新建执行方案
                    if (mode === 'create') {
                        setTimeout(() => {
                            this.handleCreatePlan();
                        });
                        
                        return;
                    }
                    // url 记录了指定查看那个执行方案
                    // 默认显示执行方案详情
                    const id = Number(viewPlanId) || 0;
                    if (id > 0) {
                        setTimeout(() => {
                            this.handlePlanSelect({
                                templateId,
                                id,
                            });
                        });
                    }
                } else {
                    // 查看所有执行方案列表
                    const {
                        viewTemplateId,
                        viewPlanId,
                    } = this.$route.query;
                    const templateId = Number(viewTemplateId) || 0;
                    const id = Number(viewPlanId) || 0;

                    if (templateId && id) {
                        setTimeout(() => {
                            this.handlePlanSelect({
                                templateId,
                                id,
                            });
                        });
                    }
                }
            },
            caclRowClassName ({ row }) {
                return row.id === this.selectPlanInfo.id ? 'active' : '';
            },
            /**
             * @desc 表格列表配置
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
             * @desc 创建执行方案
             *
             * 在查看模板执行方案页面，直接创建当前作业模板下的执行方案
             * 在查看所有执行方案列表页面，需要先选中作业模板然后才开始创建执行方案
             *
             */
            handleCreatePlan () {
                if (this.isViewTemplatePlanList) {
                    this.handleTemplateChange(this.templateId);
                } else {
                    this.isShowTemplateSelect = true;
                }
            },
            /**
             * @desc 选择作业模板后显示新建执行方案页面
             * @param {Number} templateId 作业模板id
             *
             * 需要判断将要新建的执行方案是否是改模板下面的第一个执行方案
             */
            handleTemplateChange (templateId) {
                this.selectPlanInfo = {
                    templateId,
                    id: -1,
                };
                this.planComType = 'create';
                this.paginationSmall = true;
                this.isFirstTemplatePlan = this.$refs.list.data.length < 1;
            },
            /**
             * @desc 新建执行方案时执行方案名更新
             * @param {String} planName 执行方案名
             */
            handleCreatePlanNameChange (planName) {
                this.newPlanName = planName;
            },
            /**
             * @desc 批量同步已选的执行方案
             */
            handleSyncBatch () {
                this.$router.push({
                    name: 'syncPlanBatch',
                    query: {
                        planIds: this.listSelect.map(_ => _.id).join(','),
                        from: this.$route.name,
                    },
                });
            },
            /**
             * @desc 批量更新变量值
             */
            handleBatchEditGlobalVariable () {
                this.isShowBatchGlobalVariable = true;
            },
            /**
             * @desc 列表搜索
             * @param {Object} params 搜索参数
             */
            handleSearch (params) {
                this.searchParams = params;
                this.fetchData();
            },
            /**
             * @desc 筛选我的执行方案
             */
            handleMyPlan () {
                const currentUserName = this.currentUser.username;
                const creator = {
                    name: I18n.t('template.创建人'),
                    id: 'creator',
                    values: [
                        {
                            id: currentUserName,
                            name: currentUserName,
                        },
                    ],
                };
                this.searchParams = {
                    ...this.searchParams,
                    creator: currentUserName,
                };
                this.searchValue = [creator];
                this.fetchData();
            },
            /**
             * @desc 列表选择
             * @param {Array} selectPlan 选中的执行方案
             */
            handleSelection (selectPlan) {
                this.listSelect = Object.freeze(selectPlan);
            },
            /**
             * @desc 列表布局收起
             *
             * 收起时需要更新 url 参数
             */
            handleLayoutFlod () {
                this.planComType = '';
                this.selectPlanInfo = {
                    templateId: -1,
                    id: -1,
                };
                this.paginationSmall = false;
                const searchParams = new URLSearchParams(window.location.search);
                searchParams.delete('viewTemplateId');
                searchParams.delete('viewPlanId');
                window.history.replaceState({}, '', `?${searchParams.toString()}`);
            },
            /**
             * @desc 选中执行方案查看详情
             * @param {Object} row 选中的行数据
             * @param {String} mode 执行方案的展示状态（detail、edit、create）
             *
             * 显示执行方案详情面板
             * url 查询参数拼接viewPlanId记录当前选中的执行方案id
             */
            handlePlanSelect (row, mode = 'detail') {
                const currentPlanId = row.id;
                if (currentPlanId === this.selectPlanInfo.id) {
                    return;
                }
                leaveConfirm()
                    .then(() => {
                        this.planComType = mode;
                        this.selectPlanInfo = {
                            templateId: row.templateId,
                            id: currentPlanId,
                        };
                        this.paginationSmall = true;
                        // url 缓存用户场景
                        setTimeout(() => {
                            const searchParams = new URLSearchParams(window.location.search);
                            searchParams.set('viewTemplateId', row.templateId);
                            searchParams.set('viewPlanId', row.id);
                            window.history.replaceState({}, '', `?${searchParams.toString()}`);
                        });
                    });
            },
            /**
             * @desc 收藏执行方案
             * @param {Object} plan 操作的执行方案数据
             */
            handleCollection (plan) {
                const requestHander = plan.favored ? ExecPlanService.deleteFavorite : ExecPlanService.updateFavorite;
                requestHander({
                    id: plan.id,
                    templateId: plan.templateId,
                }).then(() => {
                    plan.toggleFavored();
                    this.messageSuccess(plan.favored ? I18n.t('template.收藏成功') : I18n.t('template.取消收藏成功'));
                });
            },
            /**
             * @desc 跳转执行方案关联的定时任务列表
             * @param {Object} plan 操作的执行方案数据
             */
            handleGoCronJobList (plan) {
                const { href } = this.$router.resolve({
                    name: 'cronList',
                    query: {
                        planId: plan.id,
                    },
                });
                window.open(href);
            },
            /**
             * @desc 执行选中的执行方案
             * @param {Object} row 操作的执行方案数据
             */
            handleExecute (row) {
                // 获取作业详情
                ExecPlanService.fetchPlanDetailInfo({
                    id: row.id,
                    templateId: row.templateId,
                }).then((data) => {
                    // 没有变量——直接执行
                    if (data.variableList.length < 1) {
                        this.$bkInfo({
                            title: I18n.t('template.确认执行？'),
                            subTitle: I18n.t('template.未设置全局变量，点击确认将直接执行。'),
                            confirmFn: () => {
                                TaskExecuteService.taskExecution({
                                    taskId: row.id,
                                    taskVariables: [],
                                }).then(({ taskInstanceId }) => {
                                    this.$bkMessage({
                                        theme: 'success',
                                        message: I18n.t('template.操作成功'),
                                    });
                                    this.$router.push({
                                        name: 'historyTask',
                                        params: {
                                            id: taskInstanceId,
                                        },
                                        query: {
                                            from: this.$route.name,
                                        },
                                    });
                                });
                            },
                        });
                        return;
                    }
                    // 有变量——跳到设置变量页面
                    this.$router.push({
                        name: 'settingVariable',
                        params: {
                            id: row.id,
                            templateId: row.templateId,
                        },
                        query: {
                            from: this.$route.name,
                        },
                    });
                });
            },
            /**
             * @desc 编辑执行方案
             * @param {Object} row 操作的执行方案数据
             */
            handleEdit (row) {
                this.handlePlanSelect(row, 'edit');
            },
            /**
             * @desc 同步执行方案
             * @param {Object} row 操作的执行方案数据
             */
            handleUpdate (row) {
                this.$router.push({
                    name: 'syncPlan',
                    params: {
                        id: row.id,
                        templateId: row.templateId,
                    },
                    query: {
                        from: this.$route.name,
                    },
                });
            },

            /**
             * @desc 新建执行方案
             * @param {Object} row 操作的执行方案数据
             */
            handleGoCreateCronJob (row) {
                const { href } = this.$router.resolve({
                    name: 'cronList',
                    query: {
                        mode: 'create',
                        templateId: row.templateId,
                        planId: row.id,
                    },
                });
                window.open(href);
            },
            /**
             * @desc 删除执行方案
             * @param {Object} row 操作的执行方案数据
             */
            handleDelete (row) {
                return ExecPlanService.planDelete({
                    id: row.id,
                    templateId: row.templateId,
                }).then((data) => {
                    this.$bkMessage({
                        theme: 'success',
                        message: I18n.t('template.操作成功'),
                    });
                    this.fetchData();
                });
            },
            /**
             * @desc 创建执行方案成功
             * @param {number} planId 新创建的执行方案id
             *
             * 执行方案创建成功重新拉取列表数据，并切换到新执行方案详情的查看页面
             */
            handleCreateSubmit (planId) {
                this.fetchData();
                this.handlePlanSelect({
                    templateId: this.selectPlanInfo.templateId,
                    id: planId,
                });
            },
            /**
             * @desc 切换编辑执行方案面板
             */
            handleShowPlanEdit () {
                this.planComType = 'edit';
            },
            /**
             * @desc 删除执行方案成功
             *
             * 收起面板，刷新列表数据
             */
            handlePlanDelete () {
                this.handleLayoutFlod();
                this.fetchData();
            },
            /**
             * @desc 取消编辑执行方案
             */
            handleEditCancle () {
                this.planComType = 'detail';
            },
            /**
             * @desc 编辑执行方案成功，刷新列表数据
             */
            handleEditSuccess () {
                this.fetchData();
            },
            /**
             * @desc 批量编辑执行方案的全局变量
             *
             * 编辑成功
             *  - 刷新执行方案面板数据
             *  - 重置 table 行选中状态
             */
            handleBatchEditGlobalVariableSuccess () {
                this.$refs.list.resetSelect();
                if (this.$refs.planHandler) {
                    this.$refs.planHandler.refresh();
                }
            },
        },
    };
</script>
<style lang='postcss'>
    .task-manage-plan-page {
        .bk-table tr:hover {
            td.plan-name-column {
                .plan-name-box {
                    .collection-flag {
                        display: inline-block;
                    }
                }
            }
        }

        .bk-table-row {
            &.active {
                background: #eff5ff;
            }
        }

        .create-plan-placeholder {
            display: flex;
            align-items: center;
            height: 40px;
            padding-left: 85px;
            line-height: 40px;
            color: #3a84ff;
            border-bottom: 1px solid #dfe0e5;

            &.active {
                background: #eff5ff;
            }

            .name-box {
                max-width: 226px;
                margin-right: 8px;
                overflow: hidden;
                font-size: 13px;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
        }

        .plan-name-column {
            &:hover {
                .collection-flag {
                    display: block;
                }
            }

            .cell {
                overflow: unset;
            }

            .plan-name-box {
                position: relative;
                display: flex;
                flex: 1;
                align-items: center;
                height: 40px;
                cursor: pointer;
            }

            .name-wraper {
                display: flex;
                align-items: center;
                width: 100%;
                padding-right: 6px;
                overflow: hidden;
            }

            .name-text {
                overflow: hidden;
                color: #3a84ff;
                text-overflow: ellipsis;
                white-space: nowrap;
                cursor: pointer;
                flex: 0 1 auto;
                align-items: center;
            }

            .cron-job-tag {
                flex: 0 0 auto;
                display: inline-flex;
                height: 16px;
                padding: 0 4px;
                margin-left: 4px;
                font-size: 12px;
                color: #fff;
                cursor: pointer;
                background: #3a84ff;
                border-radius: 8px;
                user-select: none;
                justify-content: center;
                align-items: center;
            }

            .update-flag {
                flex: 0 0 auto;
                display: inline-flex;
                width: 16px;
                height: 16px;
                margin-left: 4px;
                font-size: 12px;
                color: #fff;
                background: #ea3636;
                border-radius: 8px;
                user-select: none;
                justify-content: center;
                align-items: center;
            }

            .collection-flag {
                position: absolute;
                left: -32px;
                display: none;
                padding: 10px;
                font-size: 14px;
                color: #c4c6cc;

                &.favored {
                    display: inline-block;
                    color: #ffd695;
                }
            }

            .select-flag {
                margin-left: auto;
                color: #a3c5fd;
            }
        }
    }
</style>

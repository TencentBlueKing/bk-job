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
    <div class="sync-plan-batch-page">
        <list-action-layout>
            <template #right>
                <bk-button
                    :disabled="planConfirmInfo.unconfirmed < 1 || isComfirmAllFinished"
                    :loading="isConfirmLoading || isCronJobLoading"
                    theme="primary"
                    @click="handleConfirmAll">
                    {{ $t('template.全部确认') }}
                </bk-button>
            </template>
        </list-action-layout>
        <div class="render-list-header">
            <div class="list-name">
                <span>{{ $t('template.同步执行方案') }}</span>
                <span class="total">（{{ $t('template.共') }} {{ data.length }} {{ $t('template.个.total') }}）</span>
            </div>
            <div class="item-status">
                <template v-if="isCronJobLoading">
                    <Icon
                        class="plan-cron-job-loading"
                        svg
                        type="sync-pending" />
                </template>
                <template v-else>
                    <span class="confirmed">{{ planConfirmInfo.confirmed }}</span>{{ $t('template.个已就绪') }} ，
                    <span class="unconfirmed">{{ planConfirmInfo.unconfirmed }}</span>{{ $t('template.个未就绪') }}
                </template>
            </div>
        </div>
        <div ref="list">
            <bk-table
                v-if="tableHeight"
                class="sync-plan-list"
                :data="data"
                :max-height="tableHeight"
                :row-class-name="calcRowClass"
                selectable>
                <bk-table-column
                    key="name"
                    align="left"
                    :label="$t('template.执行方案.colHead')"
                    prop="name">
                    <template slot-scope="{ row }">
                        <auth-router-link
                            auth="job_plan/view"
                            :permission="row.canView"
                            :resource-id="row.id"
                            target="_blank"
                            :to="{
                                name: 'viewPlan',
                                params: {
                                    templateId: row.templateId,
                                },
                                query: {
                                    viewPlanId: row.id,
                                },
                            }">
                            {{ row.name }}
                            <Icon
                                class="open-link-flag"
                                type="edit" />
                        </auth-router-link>
                    </template>
                </bk-table-column>
                <bk-table-column
                    key="templateName"
                    align="left"
                    :label="$t('template.所属作业模板')"
                    prop="templateName">
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
                            <Icon
                                class="open-link-flag"
                                type="edit" />
                        </router-link>
                    </template>
                </bk-table-column>
                <bk-table-column
                    key="statusText2"
                    align="left"
                    class-name="status-column"
                    :label="$t('template.状态')"
                    prop="statusText">
                    <template slot-scope="{ row }">
                        <div class="confirm-status-box">
                            <Icon
                                class="status-flag"
                                :class="row.statusIcon"
                                svg
                                :type="row.statusIcon" />
                            <span v-html="row.statusHtml" />
                            <bk-button
                                v-if="row.isRetryEnable"
                                class="ml10"
                                text
                                @click="handleSyncRetry(row)">
                                {{ $t('template.重试') }}
                            </bk-button>
                        </div>
                    </template>
                </bk-table-column>
                <bk-table-column
                    key="action"
                    align="left"
                    :label="$t('template.操作')"
                    :resizable="false"
                    width="280">
                    <template slot-scope="{ row }">
                        <span
                            class="mr10"
                            :tippy-tips="row.disableDiffTips">
                            <bk-button
                                :disabled="!!row.disableDiffTips"
                                text
                                @click="handleGoDiff(row)">
                                {{ $t('template.查看差异') }}
                            </bk-button>
                        </span>
                        <!-- 定时任务加载中 -->
                        <template v-if="row.isCronJobLoading">
                            <Icon
                                class="plan-cron-job-loading"
                                svg
                                type="sync-pending" />
                        </template>
                        <template v-else>
                            <span
                                class="mr10"
                                :tippy-tips="row.disableConfirmTips">
                                <bk-button
                                    :disabled="!!row.disableConfirmTips"
                                    :loading="row.isCronJobLoading"
                                    text
                                    @click="handleConfirmCron(row)">
                                    {{ $t('template.确认定时任务') }}
                                </bk-button>
                            </span>
                            <div
                                class="confirm-status"
                                :class="{ confirmed: row.isConfirmed }">
                                {{ row.confirmProcessText }}
                            </div>
                        </template>
                    </template>
                </bk-table-column>
            </bk-table>
        </div>
        <div class="sync-plan-action">
            <bk-button
                v-if="isFinished"
                class="w120"
                theme="primary"
                @click="handleFinish">
                {{ $t('template.完成') }}
            </bk-button>
            <template v-else>
                <bk-button
                    class="mr10"
                    @click="handleCancle">
                    {{ $t('template.取消') }}
                </bk-button>
                <span :tippy-tips="syncSubmitInvalid ? $t('template.所有方案均已同步至最新版') : ''">
                    <bk-button
                        class="w120"
                        :disabled="syncSubmitInvalid || planConfirmInfo.unconfirmed > 0"
                        :loading="isSyncLoading || isCronJobLoading"
                        theme="primary"
                        @click="handleSubmitSync">
                        {{ $t('template.立即同步') }}
                    </bk-button>
                </span>
            </template>
        </div>
        <confirm-cron
            :is-show="isShowConfirmCron"
            v-bind="selectPlanInfo"
            @on-change="handleSelectPlanConfirmChange"
            @on-close="hanndleSelectPlanConfirmClose" />
    </div>
</template>
<script>
    import _ from 'lodash';

    import TaskManageService from '@service/task-manage';
    import TaskPlanService from '@service/task-plan';
    import TimeTaskService from '@service/time-task';

    import {
        getOffset,
        leaveConfirm,
    } from '@utils/assist';

    import ListActionLayout from '@components/list-action-layout';

    import SyncPlanVO from '@domain/variable-object/sync-plan';

    import ConfirmCron from './components/confirm-cron';

    import I18n from '@/i18n';

    const runStepByStep = (data, callback, finishCallback = () => {}) => {
        let startIndex = 0;
        const next = () => {
            startIndex += 1;
            if (startIndex >= data.length) {
                finishCallback();
                return;
            }
            callback(data[startIndex], next);
        };
        callback(data[startIndex], next);
    };

    export default {
        name: '',
        components: {
            ListActionLayout,
            ConfirmCron,
        },
        data () {
            return {
                isLoading: true,
                data: [],
                tableHeight: '',
                isCronJobLoading: false,
                isConfirmLoading: false,
                isComfirmAllFinished: false,
                isSyncLoading: false,
                isShowConfirmCron: false,
                isFinished: false,
                syncValueMemoMap: {},
                selectPlanInfo: {
                    templateId: -1,
                    planId: -1,
                    cronJobInfoList: [],
                },
            };
        },
        computed: {
            isSkeletonLoading () {
                return this.isLoading;
            },
            planConfirmInfo () {
                let confirmed = 0;
                let unconfirmed = 0;
                
                this.data.forEach((currentPlan) => {
                    if (currentPlan.isConfirmed) {
                        confirmed += 1;
                    } else {
                        unconfirmed += 1;
                    }
                });
                return {
                    confirmed,
                    unconfirmed,
                };
            },
            syncSubmitInvalid () {
                // eslint-disable-next-line no-plusplus
                for (let i = 0; i < this.data.length; i++) {
                    const currentPlan = this.data[i];
                    // 需要确认定时任务
                    if (currentPlan.needUpdate) {
                        return false;
                    }
                }
                return true;
            },
        },
        created () {
            const { planIds = '' } = this.$route.query;
            this.planIds = planIds;
            // 如果是从作业模板的执行方案列表过来同步的
            // 所有执行方案的templateId相同，保留下来，路由回退时需要
            this.lastOnePlanTemplateId = '';
            this.fetchData();
        },
        mounted () {
            this.calcTableHeight();
        },
        methods: {
            /**
             * @desc 获取要同步的执行方案基本信息
             *
             * 初始化同步执行方案的状态
             */
            fetchData () {
                this.isLoading = true;
                TaskPlanService.fetchBatchPlan({
                    planIds: this.planIds,
                }).then((data) => {
                    const planData = [];
                    const needCheckCronJobStatusPlanMap = {};
                    
                    data.forEach((_) => {
                        const currentSyncPlan = new SyncPlanVO(_);
                        planData.push(currentSyncPlan);
                        this.lastOnePlanTemplateId = _.templateId;
                        // 执行方案有关联定时任务
                        // 执行方案的状态设置为加载定时任务中（SyncPlanVO.STATUS_CRON_JOB_LOADING）
                        // 缓存该执行方案id用于下一步获取关联定时任务
                        if (currentSyncPlan.cronJobCount > 0) {
                            currentSyncPlan.status = SyncPlanVO.STATUS_CRON_JOB_LOADING;
                            needCheckCronJobStatusPlanMap[currentSyncPlan.id] = currentSyncPlan;
                        }
                    });
                    this.data = planData;

                    // 有执行方案需要获取关联定时任务
                    // 页面操作状态设置定时任务任务获取中（isCronJobLoading），其它操作失效
                    const needCheckCronJobStatusPlanIds = Object.keys(needCheckCronJobStatusPlanMap);
                    if (needCheckCronJobStatusPlanIds.length > 0) {
                        this.isCronJobLoading = true;
                        TimeTaskService.fetchTaskOfPlanBatch({
                            planIds: needCheckCronJobStatusPlanIds.join(','),
                        }).then((data) => {
                            for (const planId in data) {
                                const currentCronJobList = data[planId];
                                if (currentCronJobList.some(_ => _.enable)) {
                                    // 执行方案关联的定时任务[有开启状态]——同步状态设置为未就绪
                                    needCheckCronJobStatusPlanMap[planId].status = SyncPlanVO.STATUS_DEFAULT;
                                } else {
                                    // 执行方案关联的定时任务[全部是关闭状态]——同步状态设置为已就绪
                                    needCheckCronJobStatusPlanMap[planId].status = SyncPlanVO.STATUS_CONFIRMED;
                                }
                            }
                        })
                            .finally(() => {
                                this.isCronJobLoading = false;
                            });
                    }
                    
                    if (this.data.length > 0) {
                        window.changeConfirm = true;
                    }
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },

            /**
             * @desc 计算页面高度，实现表格内部滚动
             */
            calcTableHeight () {
                const { top } = getOffset(this.$refs.list);
                const windowHeight = window.innerHeight;
                this.tableHeight = windowHeight - top - 77;
            },
            /**
             * @desc 计算表格行的样式
             */
            calcRowClass ({ row }) {
                let className = 'template-plan-sync-record';
                if (!row.canEdit) {
                    className = `${className} sync-permission`;
                }
                return className;
            },
            /**
             * @desc 全部确认操作
             *
             * 一条条数据串联同步
             */
            handleConfirmAll () {
                window.changeConfirm = true;
                this.isConfirmLoading = true;
                const syncValueMemoMap = { ...this.syncValueMemoMap };
                this.data.forEach((item) => {
                    if (syncValueMemoMap[item.id]) {
                        return;
                    }
                    item.status = SyncPlanVO.STATUS_CONFIRM_QUEUE;
                });
                let errorNums = 0;
                const confirmOnePlan = (plan, next) => {
                    const currentSyncPlan = plan;
                    const syncValue = {
                        planId: currentSyncPlan.id,
                        templateId: currentSyncPlan.templateId,
                        templateVersion: currentSyncPlan.templateVersion,
                        cronJobInfoList: [],
                    };
                    
                    // 执行方案没有查看和编辑权限——跳过
                    if (!currentSyncPlan.canView || !currentSyncPlan.canEdit) {
                        next();
                        return;
                    }
                    // 已确认——跳过
                    if (currentSyncPlan.isConfirmed) {
                        next();
                        return;
                    }
                    // 没有定时任务——跳过
                    if (currentSyncPlan.isPassConfirm) {
                        syncValueMemoMap[currentSyncPlan.id] = syncValue;
                        next();
                        return;
                    }
                    
                    // 定时任务确认中
                    currentSyncPlan.status = SyncPlanVO.STATUS_CONFIRM_PENDGING;
                    Promise.all([
                        TaskManageService.taskDetail({
                            id: currentSyncPlan.templateId,
                        }),
                        TimeTaskService.fetchTaskOfPlan({
                            id: currentSyncPlan.id,
                        }),
                    ]).then(([
                        template,
                        cronJobList,
                    ]) => {
                        // 作业模板中的变量
                        const currentTemplateVariableList = template.variables;

                        // 必填变量没有赋值
                        let isRequiredError = false;
                        let isPermissionError = false;

                        // 确认定时任务的变量值
                        // 1，将模板中的变量同步到定时任务中
                        // 2，作业模板和定时任务同名的变量保留定时任务中的变量值
                        // 3，作业模板中新增的变量为必填但值为空则同步失败
                        const cronJobInfoList = [];
                        // eslint-disable-next-line no-plusplus
                        for (let i = 0; i < cronJobList.length; i++) {
                            const currentCronJob = cronJobList[i];

                            const currentCronJobInfo = {
                                id: currentCronJob.id,
                                name: currentCronJob.name,
                                enable: currentCronJob.enable,
                                hasConfirm: false,
                                variableValue: [],
                            };
                            // 没有定时任务的管理权限——跳过处理
                            if (!currentCronJob.canManage) {
                                isPermissionError = true;
                                cronJobInfoList.push(currentCronJobInfo);
                                continue;
                            }
                            // 当前定时任务未被开启——跳过处理
                            if (!currentCronJob.enable) {
                                cronJobInfoList.push(currentCronJobInfo);
                                continue;
                            }

                            // 同步作业模板中变量到定时任务
                            // 作业模板和定时任务同名的变量——保留定时任务中的变量值
                            const currentCronJobVariableMap = currentCronJob.variableValue.reduce((result, item) => {
                                result[item.id] = item;
                                return result;
                            }, {});
                            const newCronJobVariableList = [];
                            // eslint-disable-next-line no-plusplus
                            for (let j = 0; j < currentTemplateVariableList.length; j++) {
                                const newVariableFromTemplate = _.cloneDeep(currentTemplateVariableList[j]);
                                
                                if (currentCronJobVariableMap[newVariableFromTemplate.id]) {
                                    const {
                                        value,
                                        targetValue,
                                    } = currentCronJobVariableMap[newVariableFromTemplate.id];
                                    newVariableFromTemplate.defaultValue = value;
                                    newVariableFromTemplate.defaultTargetValue = targetValue;
                                }
                                // 必填变量不能为空
                                if (newVariableFromTemplate.isRequired && newVariableFromTemplate.isEmpty) {
                                    isRequiredError = true;
                                }
                                const { id, name, type, defaultValue, defaultTargetValue } = newVariableFromTemplate;
                                // 定时任务中的变量需要赋值操作
                                newCronJobVariableList.push({
                                    id,
                                    name,
                                    type,
                                    defaultValue,
                                    defaultTargetValue,
                                    value: defaultValue,
                                    targetValue: defaultTargetValue,
                                });
                            }

                            // 有必填变量没有被赋值，确实失败
                            currentCronJobInfo.hasConfirm = !isRequiredError;
                            currentCronJobInfo.variableValue = newCronJobVariableList;
                            cronJobInfoList.push(currentCronJobInfo);
                        }
                        
                        // 手动确认过——继续使用手动确认的结果
                        if (!syncValueMemoMap[currentSyncPlan.id]) {
                            syncValue.cronJobInfoList = cronJobInfoList;
                            syncValueMemoMap[currentSyncPlan.id] = syncValue;
                        }

                        // 定时任务确认完成
                        currentSyncPlan.cronJobInfoList = cronJobInfoList;
                        if (isRequiredError) {
                            currentSyncPlan.status = SyncPlanVO.STATUS_CONFIRM_FAILED;
                            currentSyncPlan.error = I18n.t('template.定时任务中必填变量未赋值');
                            errorNums += 1;
                        } else if (isPermissionError) {
                            currentSyncPlan.status = SyncPlanVO.STATUS_CONFIRM_FAILED;
                            currentSyncPlan.error = I18n.t('template.没有定时任务管理权限，请手动确认');
                            errorNums += 1;
                        } else {
                            currentSyncPlan.status = SyncPlanVO.STATUS_CONFIRMED;
                        }
                    })
                        .catch(() => {
                            // 定时任务确认失败
                            currentSyncPlan.status = SyncPlanVO.STATUS_CONFIRM_FAILED;
                            currentSyncPlan.error = I18n.t('template.自动确认定时任务失败，请手动确认');
                            errorNums += 1;
                        })
                        .finally(() => {
                            next();
                        });
                };
                
                runStepByStep(this.data, confirmOnePlan, () => {
                    this.isConfirmLoading = false;
                    this.syncValueMemoMap = Object.freeze(syncValueMemoMap);
                    this.isComfirmAllFinished = true;
                    if (errorNums > 0) {
                        this.messageError(`${errorNums} ${I18n.t('template.项执行方案的确认出现问题，请逐个确认')}`);
                    }
                });
            },
            /**
             * @desc 查看同步差异
             */
            handleGoDiff (plan) {
                const router = this.$router.resolve({
                    name: 'syncPlan',
                    params: {
                        id: plan.id,
                        templateId: plan.templateId,
                    },
                    query: {
                        mode: 'view',
                    },
                });
                window.open(router.href);
            },
            /**
             * @desc 打开手动确认弹框
             * @param {Object} plan 要确认的执行方案
             */
            handleConfirmCron (plan) {
                let cronJobInfoList = [];
                if (this.syncValueMemoMap[plan.id]) {
                    /* eslint-disable prefer-destructuring */
                    cronJobInfoList = this.syncValueMemoMap[plan.id].cronJobInfoList;
                }
                this.isShowConfirmCron = true;
                this.selectPlanInfo = {
                    templateId: plan.templateId,
                    planId: plan.id,
                    cronJobInfoList,
                };
            },
            /**
             * @desc 关闭手动确认弹框
             */
            hanndleSelectPlanConfirmClose () {
                this.isShowConfirmCron = false;
                this.selectPlanInfo = {
                    templateId: -1,
                    planId: -1,
                    cronJobInfoList: [],
                };
            },
            /**
             * @desc 提交手动确认的定任务信息
             * @param {Array} cronJobInfoList 执行方案关联的定时任务变量信息
             */
            handleSelectPlanConfirmChange (cronJobInfoList) {
                window.changeConfirm = true;

                const syncValueMemoMap = { ...this.syncValueMemoMap };
                
                syncValueMemoMap[this.selectPlanInfo.planId] = {
                    templateId: this.selectPlanInfo.templateId,
                    id: this.selectPlanInfo.planId,
                    cronJobInfoList,
                };
                this.syncValueMemoMap = Object.freeze(syncValueMemoMap);

                const currentPlan = _.find(this.data, _ => _.id === this.selectPlanInfo.planId);
                // 定时任务全部确认完成
                if (cronJobInfoList.every(_ => _.hasConfirm || !_.enable)) {
                    currentPlan.status = SyncPlanVO.STATUS_CONFIRMED;
                }
                currentPlan.cronJobInfoList = cronJobInfoList;
                this.data = [
                    ...this.data,
                ];
            },
            /**
             * @desc 提交同步
             *
             * 一条一条数据串联同步
             */
            handleSubmitSync () {
                this.isSyncLoading = true;
                this.data.forEach((item) => {
                    // 进入同步队列
                    if (item.needUpdate) {
                        item.status = SyncPlanVO.STATUS_SYNC_QUEUE;
                    }
                });
                
                const syncOnePlan = (plan, next) => {
                    const currentSyncPlan = plan;
                    // 需要同步的执行方案才会变更同步状态
                    if (!currentSyncPlan.needUpdate) {
                        currentSyncPlan.status = SyncPlanVO.STATUS_SYNCED;
                        next();
                        return;
                    }
                    
                    // 同步中
                    currentSyncPlan.status = SyncPlanVO.STATUS_SYNC_PENDING;

                    TaskPlanService.planSyncInfo({
                        planId: currentSyncPlan.id,
                        templateId: currentSyncPlan.templateId,
                        templateVersion: currentSyncPlan.templateVersion,
                    }).then(() => {
                        // 有定时任务才会执行同步定时任务
                        if (this.syncValueMemoMap[currentSyncPlan.id]
                            && this.syncValueMemoMap[currentSyncPlan.id].cronJobInfoList.length > 0) {
                            return TimeTaskService.updatePlanTask({
                                cronJobInfoList: this.syncValueMemoMap[currentSyncPlan.id].cronJobInfoList,
                            });
                        }
                        // 不需要同步定时任务
                        return Promise.resolve();
                    })
                        .then(() => {
                            // 同步成功
                            currentSyncPlan.status = SyncPlanVO.STATUS_SYNCED;
                        })
                        .catch(() => {
                            // 同步失败
                            currentSyncPlan.status = SyncPlanVO.STATUS_SYNC_FAILED;
                        })
                        .finally(() => {
                            next();
                        });
                };

                runStepByStep(this.data, syncOnePlan, () => {
                    this.isSyncLoading = false;
                    this.isFinished = true;
                    window.changeConfirm = false;
                });
            },
            /**
             * @desc 同步失败重试
             * @param {Object} plan 重试的执行方案
             */
            handleSyncRetry (plan) {
                plan.status = SyncPlanVO.STATUS_SYNC_PENDING;
                TaskPlanService.planSyncInfo({
                    planId: plan.id,
                    templateId: plan.templateId,
                    templateVersion: plan.templateVersion,
                }).then(() => TimeTaskService.updatePlanTask({
                    cronJobInfoList: this.syncValueMemoMap[plan.id].cronJobList,
                }))
                    .then(() => {
                        // 同步成功
                        plan.status = SyncPlanVO.STATUS_SYNCED;
                    })
                    .catch(() => {
                        // 同步失败
                        plan.error = I18n.t('template.同步请求失败，请重试');
                        plan.status = SyncPlanVO.STATUS_SYNC_FAILED;
                    });
            },
            /**
             * @desc 取消批量同步
             *
             * 需要确认页面的编辑状态
             */
            handleCancle () {
                leaveConfirm()
                    .then(() => {
                        this.routerBack();
                    });
            },
            /**
             * @desc 完成批量同步
             */
            handleFinish () {
                window.changeConfirm = false;
                this.routerBack();
            },
            /**
             * @desc 路由回退
             */
            routerBack () {
                const { from } = this.$route.query;
                if (from === 'viewPlan') {
                    this.$router.push({
                        name: 'viewPlan',
                        params: {
                            templateId: this.lastOnePlanTemplateId,
                        },
                    });
                } else if (from === 'planList') {
                    this.$router.push({
                        name: 'planList',
                    });
                } else if (from === 'templateDetail') {
                    this.$router.push({
                        name: 'templateDetail',
                        params: {
                            id: this.lastOnePlanTemplateId,
                        },
                    });
                } else if (from === 'templateEdit') {
                    this.$router.push({
                        name: 'viewPlan',
                        params: {
                            templateId: this.lastOnePlanTemplateId,
                        },
                    });
                } else {
                    this.$router.push({
                        name: 'planList',
                    });
                }
            },
        },
    };
</script>
<style lang='postcss'>
    @keyframes sync-loading-ani {
        from {
            transform: rotateZ(0);
        }

        to {
            transform: rotateZ(360deg);
        }
    }

    .sync-plan-batch-page {
        .render-list-header {
            display: flex;
            height: 42px;
            padding: 0 15px;
            font-size: 12px;
            color: #63656e;
            background: #f0f1f5;
            border: 1px solid #dcdee5;
            border-bottom: none;
            align-items: center;

            .list-name {
                font-weight: bold;

                .total {
                    color: #979ba5;
                }
            }

            .item-status {
                margin-left: auto;

                .confirmed,
                .unconfirmed {
                    padding-right: 4px;
                    font-weight: bold;
                }

                .confirmed {
                    color: #3a84ff;
                }
            }
        }

        .sync-plan-list {
            &.bk-table {
                border-top-right-radius: 0;
                border-top-left-radius: 0;
            }

            .template-plan-sync-record {
                &:hover {
                    .open-link-flag {
                        opacity: 100%;
                    }
                }
            }

            .sync-permission {
                background: #fafbfd;
            }

            .open-link-flag {
                font-size: 12px;
                opacity: 0%;
            }

            .status-column {
                .cell {
                    overflow: unset;
                }
            }

            .confirm-status-box {
                display: flex;
                align-items: center;

                .status-flag {
                    margin-right: 4px;

                    &.sync-default {
                        color: #c4c6cc;
                    }

                    &.sync-pending {
                        color: #3a84ff;
                        animation: sync-loading-ani 1s linear infinite;
                    }

                    &.sync-success {
                        color: #3fc06d;
                    }

                    &.sync-failed {
                        color: #ea3636;
                    }
                }

                span[tippy-tips] {
                    padding-bottom: 2px;
                    cursor: pointer;
                    border-bottom: 1px dashed #c4c6cc;
                }
            }
        }

        .confirm-status {
            height: 16px;
            padding: 0 5px;
            font-size: 12px;
            line-height: 16px;
            color: #979ba5;
            background: #f0f1f5;
            border-radius: 8px;

            &.confirmed {
                color: #3a84ff;
                background: #e1ecff;
            }
        }

        .plan-cron-job-loading {
            color: #3a84ff;
            animation: sync-loading-ani 1s linear infinite;
        }

        .sync-plan-action {
            position: fixed;
            right: 0;
            bottom: 0;
            left: 0;
            display: flex;
            justify-content: flex-end;
            align-items: center;
            height: 52px;
            padding-right: 24px;
            background: #fff;
            box-shadow: 0 -2px 4px 0 rgb(0 0 0 / 6%);
        }
    }
</style>

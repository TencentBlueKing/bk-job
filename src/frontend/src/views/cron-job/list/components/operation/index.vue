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
        class="time-task-operation"
        :class="{ loading: isLoading }"
        v-bkloading="{ isLoading }">
        <jb-form
            v-if="!isLoading"
            :model="formData"
            :rules="rules"
            form-type="vertical"
            ref="timeTaskForm">
            <jb-form-item
                :label="$t('cron.任务名称.label')"
                required
                property="name">
                <jb-input
                    :placeholder="$t('cron.推荐按照该定时执行的实际场景来取名...')"
                    v-model="formData.name"
                    :maxlength="60" />
            </jb-form-item>
            <jb-form-item
                :label="$t('cron.执行策略.label')"
                required
                :property="strategyField">
                <bk-radio-group
                    :value="strategy"
                    @change="handleStrategyChange">
                    <bk-radio-button value="once">
                        {{ $t('cron.单次执行') }}
                    </bk-radio-button>
                    <bk-radio-button value="period">
                        {{ $t('cron.周期执行') }}
                    </bk-radio-button>
                </bk-radio-group>
                <div class="strategy-wraper">
                    <render-strategy
                        v-if="strategy === 'once'"
                        left="40">
                        <bk-date-picker
                            style="width: 100%;"
                            v-model="formData.executeTime"
                            :transfer="true"
                            :clearable="false"
                            :options="dateOptions"
                            type="datetime"
                            :placeholder="$t('cron.选择日期时间')" />
                    </render-strategy>
                    <render-strategy
                        v-else
                        left="135">
                        <cron-job
                            v-model="formData.cronExpression"
                            class="cron-task" />
                    </render-strategy>
                </div>
            </jb-form-item>
            <form-item-factory
                v-for="item in strategyFormItemList"
                :name="item"
                :key="item"
                :form-data="formData"
                @on-change="handleFormItemChange" />
            <jb-form-item
                :label="$t('cron.作业模板')"
                required
                property="taskTemplateId">
                <bk-select
                    v-model="formData.taskTemplateId"
                    :placeholder="$t('cron.选择作业模板')"
                    :clearable="false"
                    searchable
                    :loading="isTemplateLoading"
                    @selected="handleTemplateChange">
                    <auth-option
                        v-for="option in templateList"
                        :key="option.id"
                        :id="option.id"
                        :name="option.name"
                        :permission="option.canView"
                        :resource-id="option.id"
                        auth="job_template/view" />
                </bk-select>
            </jb-form-item>
            <jb-form-item
                :label="$t('cron.执行方案')"
                required
                property="taskPlanId">
                <div
                    class="plan-select"
                    :class="hasPlan ? 'new-width' : ''">
                    <bk-select
                        v-model="formData.taskPlanId"
                        :placeholder="$t('cron.选择执行方案')"
                        :clearable="false"
                        :disabled="!hasTemplate"
                        :loading="isPlanLoading"
                        searchable
                        @selected="handlePlanChange">
                        <auth-option
                            v-for="option in planList"
                            :key="option.id"
                            :id="option.id"
                            :name="option.name"
                            :permission="option.canView"
                            :resource-id="option.id"
                            auth="job_plan/view" />
                    </bk-select>
                </div>
                <div class="plan-icon" v-if="hasPlan">
                    <Icon
                        type="audit"
                        v-bk-tooltips="$t('cron.查看执行方案')"
                        @click="handleGoPlan" />
                </div>
            </jb-form-item>
            <div
                v-if="hasPlan"
                class="global-variable-content"
                v-bkloading="{ isLoading: isVariableLoading }">
                <render-strategy left="70">
                    <span
                        v-if="isVariabelEmpty"
                        class="plan-variable-empty">
                        {{ $t('cron.该执行方案无全局变量') }}
                    </span>
                    <global-variable-layout v-else type="vertical">
                        <global-variable
                            ref="variable"
                            v-for="variable in currentPlanVariableList"
                            value-width="100%"
                            :type="variable.type"
                            :key="`${currentRenderPlanId}_${variable.id}_${variable.name}`"
                            :data="variable" />
                    </global-variable-layout>
                </render-strategy>
            </div>
        </jb-form>
    </div>
</template>
<script>
    import I18n from '@/i18n';
    import TaskService from '@service/task-manage';
    import TaskPlanService from '@service/task-plan';
    import TimeTaskService from '@service/time-task';
    import {
        generatorDefaultCronTime,
    } from '@utils/assist';
    import {
        timeTaskNameRule,
    } from '@utils/validator';
    import GlobalVariableLayout from '@components/global-variable/layout';
    import GlobalVariable from '@components/global-variable/edit';
    import JbInput from '@components/jb-input';
    import RenderStrategy from '../render-strategy';
    import FormItemFactory from './form-item-strategy';
    import CronJob from './cron-job';

    const onceItemList = [
        'executeBeforeNotify',
    ];
    const periodItemList = [
        'endTime',
        'finishBeforeNotify',
    ];

    const getDefaultData = () => ({
        id: 0,
        executeBeforeNotify: false,
        cronExpression: '* * * * *', // 循环执行的定时表达式
        enable: false, // 是否启用
        endTime: '', // 周期执行的结束时间
        executeTime: generatorDefaultCronTime(), // 单次执行的指定执行时间
        isEnable: false,
        name: '', // 任务名称
        notifyChannel: [], // 通知渠道
        notifyOffset: 0, // 通知提前时间
        notifyUser: { // 用户角色列表
            roleList: [],
            userList: [],
        },
        taskPlanId: 0, // 关联的执行方案 ID
        taskTemplateId: 0,
        variableValue: [], // 变量信息
    });
 
    export default {
        name: '',
        components: {
            GlobalVariableLayout,
            GlobalVariable,
            RenderStrategy,
            JbInput,
            FormItemFactory,
            CronJob,
        },
        props: {
            data: {
                type: Object,
                default: () => ({}),
            },
            id: {
                type: [
                    String,
                    Number,
                ],
                default: '',
            },
        },
        data () {
            this.rules = {};
            return {
                isLoading: false,
                isTemplateLoading: false,
                isPlanLoading: false,
                strategy: 'period',
                formData: getDefaultData(),
                templateList: [],
                planList: [],
                isVariableLoading: false,
                currentPlanVariableList: [],
                currentRenderPlanId: 0, // 切换执行方案后更新全局变量（不同执行方案里面的变量id和name是相同的当value不同时页面不会更新）
                isRepeat: false,
                dateOptions: {
                    disabledDate (date) {
                        return date.valueOf() < Date.now() - 86400000;
                    },
                },
            };
        },
        computed: {
            strategyFormItemList () {
                if (this.strategy === 'once') {
                    return onceItemList;
                }
                return periodItemList;
            },
            strategyField () {
                return this.strategy === 'once' ? 'executeTime' : 'cronExpression';
            },
            hasTemplate () {
                return !!this.formData.taskTemplateId;
            },
            hasPlan () {
                return !!this.formData.taskPlanId;
            },
            hasVariabel () {
                return this.currentPlanVariableList.length < 1;
            },
            isVariabelEmpty () {
                return this.currentPlanVariableList.length < 1;
            },
        },
        watch: {
            strategy: {
                handler (value) {
                    if (value === 'once') {
                        delete this.rules.cronExpression;
                        this.rules.executeTime = [
                            { required: true, message: I18n.t('cron.单次执行时间必填'), trigger: 'blur' },
                            {
                                validator: value => new Date(value).getTime() > Date.now(),
                                message: I18n.t('cron.执行时间无效（早于当前时间）'),
                                trigger: 'blur',
                            },
                        ];
                    } else {
                        delete this.rules.executeTime;
                        this.rules.cronExpression = [
                            { required: true, message: I18n.t('cron.请输入正确时间表达式'), trigger: 'blur' },
                        ];
                    }
                },
                immediate: true,
            },
        },
        created () {
            this.rules = Object.assign({}, this.rules, {
                name: [
                    {
                        required: true,
                        message: I18n.t('cron.任务名称必填'),
                        trigger: 'blur',
                    },
                    {
                        validator: timeTaskNameRule.validator,
                        message: timeTaskNameRule.message,
                        trigger: 'blur',
                    },
                    {
                        validator: this.checkName,
                        message: I18n.t('cron.任务名称已存在，请重新输入'),
                        trigger: 'blur',
                    },
                ],
                taskTemplateId: [
                    {
                        required: true,
                        message: I18n.t('cron.作业模板必填'),
                        trigger: 'blur',
                    },
                ],
                taskPlanId: [
                    {
                        required: true,
                        message: I18n.t('cron.执行方案必填'),
                        trigger: 'blur',
                    },
                ],
            });
            
            // 作业模板列表
            this.fetchTemplateList();

            if (this.data.id) {
                // 编辑状态

                this.formData.id = this.data.id;
                this.formData.name = this.data.name;
                this.formData.taskTemplateId = this.data.taskTemplateId;
                this.formData.taskPlanId = this.data.taskPlanId;
                this.fetchData();
                this.fetchTemplatePlanList();
            } else {
                // 新建(指定指定执行方案)

                // 通过url指定作业模板和执行方案的定时任务任务
                // 执行方案id 必须是 templateId 同时存在时才有效
                const { templateId, planId } = this.$route.query;
                if (parseInt(templateId, 10) > 0) {
                    this.formData.taskTemplateId = parseInt(templateId, 10);
                    this.fetchTemplatePlanList();
                    if (parseInt(planId, 10) > 0) {
                        this.formData.taskPlanId = parseInt(planId, 10);
                        this.fetchPlanDetailInfo();
                    }
                }
            }
        },
        
        methods: {
            /**
             * @desc 定时任务详情
             */
            fetchData () {
                this.isLoading = true;
                Promise.all([
                    TimeTaskService.getDetail({
                        id: this.formData.id,
                    }),
                    TaskPlanService.fetchPlanDetailInfo({
                        templateId: this.formData.taskTemplateId,
                        id: this.formData.taskPlanId,
                    }),
                ]).then(([
                    cronJob,
                    planInfo,
                ]) => {
                    const {
                        cronExpression,
                        enable,
                        endTime,
                        executeTime,
                        name,
                        notifyChannel,
                        notifyOffset,
                        notifyUser,
                        scriptId,
                        scriptVersionId,
                        taskPlanId,
                        taskTemplateId,
                    } = cronJob;

                    if (executeTime) {
                        this.strategy = 'once';
                    }
                    this.formData = {
                        ...this.formData,
                        cronExpression,
                        enable,
                        endTime,
                        executeTime,
                        name,
                        notifyChannel,
                        notifyOffset,
                        notifyUser,
                        scriptId,
                        scriptVersionId,
                        taskPlanId,
                        taskTemplateId,
                        variableValue: [],
                    };

                    // 使用执行方案的变量
                    // 如果定时任务任务中存有变量变量值——拷贝过来
                    // 获取执行方案的接口是一个批量接口，取数组第一个
                    if (planInfo) {
                        const cronJobVariableList = [
                            ...cronJob.variableValue,
                        ];
                        // 当前定时任务变量
                        const cronJobVariableMap = cronJobVariableList.reduce((result, variableItem) => {
                            result[variableItem.id] = variableItem;
                            return result;
                        }, {});
                        const currentPlanVariableList = planInfo.variableList;
                        // 拷贝定时任务中的变量值到执行方案的相同变量中
                        // 定时任务中的变量是执行的赋值逻辑，分别使用的是value、server字段来存储
                        // 执行方案中的变量表示的是默认值，分别使用defaultValue、defaultTargetValue字段来存储
                        currentPlanVariableList.forEach((validVariableFromPlan) => {
                            if (cronJobVariableMap[validVariableFromPlan.id]) {
                                const { value, targetValue } = cronJobVariableMap[validVariableFromPlan.id];
                                validVariableFromPlan.defaultValue = value;
                                validVariableFromPlan.defaultTargetValue = targetValue;
                            }
                        });
                        this.currentPlanVariableList = Object.freeze(currentPlanVariableList);
                    }
                })
                    .finally(() => {
                        this.isLoading = false;
                    })
                    .catch((error) => {
                        if (error.code === 1238001) {
                            this.formData.taskTemplateId = '';
                            this.formData.taskPlanId = '';
                        }
                    });
            },
            /**
             * @desc 所有的作业模板列表
             */
            fetchTemplateList () {
                this.isTemplateLoading = true;
                TaskService.taskList({
                    pageSize: -1,
                    start: -1,
                }).then(({ data }) => {
                    this.templateList = Object.freeze(data);
                })
                    .finally(() => {
                        this.isTemplateLoading = false;
                    });
            },
            /**
             * @desc 指定作业模板关联的执行方案列白哦
             */
            fetchTemplatePlanList () {
                this.isPlanLoading = true;
                TaskPlanService.fetchTaskPlan({
                    id: this.formData.taskTemplateId,
                }).then((data) => {
                    this.planList = Object.freeze(data);
                })
                    .finally(() => {
                        this.isPlanLoading = false;
                    });
            },
            /**
             * @desc 执行方案详情
             */
            fetchPlanDetailInfo () {
                this.isVariableLoading = true;
                TaskPlanService.fetchPlanDetailInfo({
                    templateId: this.formData.taskTemplateId,
                    id: this.formData.taskPlanId,
                }).then((planInfo) => {
                    this.currentPlanVariableList = Object.freeze(planInfo.variableList);
                    this.currentRenderPlanId = this.formData.taskPlanId;
                })
                    .finally(() => {
                        this.isVariableLoading = false;
                    });
            },
            /**
             * @desc 检测定时任务是否重名
             * @param {String} name 定时任务名
             */
            checkName (name) {
                return TimeTaskService.timeTaskCheckName({
                    id: this.formData.id,
                    name,
                });
            },
            /**
             * @desc 切换执行策略
             * @param {String} strategy 执行策略
             */
            handleStrategyChange (strategy) {
                this.strategy = strategy;
                if (strategy === 'once') {
                    this.$refs.timeTaskForm.clearError('cronExpression');
                    this.formData.executeTime = generatorDefaultCronTime();
                } else {
                    this.formData.cronExpression = '* * * * *';
                }
                this.formData.endTime = '';
                this.formData.notifyChannel = [];
                this.formData.notifyOffset = 0; // 通知提前时间
                this.formData.notifyUser = { // 用户角色列表
                    roleList: [],
                    userList: [],
                };
            },
            /**
             * @desc 执行通知相关的字段值更新
             * @param {Object} payload 字段名和值
             */
            handleFormItemChange (payload) {
                this.formData = {
                    ...this.formData,
                    ...payload,
                };
            },
            /**
             * @desc 作业模板更新
             * @param {Id} templateId 作业模板id
             *
             * 作业模板改变时重新获取执行方案列表
             */
            handleTemplateChange (templateId) {
                this.formData.taskPlanId = '';
                if (templateId) {
                    this.fetchTemplatePlanList();
                }
            },
            /**
             * @desc 执行方案更新
             * @param {Id} planId 执行方案id
             *
             * 执行方案该表重新获取执行方案详情
             */
            handlePlanChange (planId) {
                if (planId) {
                    this.fetchPlanDetailInfo();
                }
            },
            /**
             * @desc 打开执行方案详情页
             */
            handleGoPlan () {
                const routerUrl = this.$router.resolve({
                    name: 'viewPlan',
                    params: {
                        templateId: this.formData.taskTemplateId,
                    },
                    query: {
                        from: 'cronJob',
                        viewPlanId: this.formData.taskPlanId,
                    },
                });
                window.open(routerUrl.href, '_blank');
            },
            /**
             * @desc 保存执行方案
             */
            submit () {
                return this.$refs.timeTaskForm.validate().then(() => {
                    if (this.currentPlanVariableList.length < 1) {
                        return Promise.resolve([]);
                    }
                    return Promise.all(this.$refs.variable.map(item => item.validate()));
                })
                    .then((variableValue) => {
                        const params = { ...this.formData };
                        if (this.strategy === 'period') {
                            params.executeTime = '';
                        } else {
                            params.cronExpression = '';
                            params.executeTime = new Date(params.executeTime).getTime() / 1000;
                        }
                        if (params.endTime) {
                            params.endTime = new Date(params.endTime).getTime() / 1000;
                        }
                        return TimeTaskService.timeTaskUpdate({
                            ...params,
                            variableValue,
                        }).then(() => {
                            if (params.id) {
                                // 编辑
                                this.messageSuccess(I18n.t('cron.定时任务编辑成功'));
                            } else {
                                // 新建
                                this.messageSuccess(I18n.t('cron.定时任务创建成功(默认关闭，请手动开启)'));
                            }
                            
                            this.$emit('on-change');
                        });
                    });
            },
        },
    };
</script>
<style lang='postcss' scoped>
    .time-task-operation {
        &.loading {
            height: calc(100vh - 100px);
        }

        .global-variable-content {
            position: relative;
            top: -10px;
        }

        .plan-select {
            display: inline-block;
            width: 100%;

            &.new-width {
                width: calc(100% - 22px);
            }
        }

        .plan-icon {
            display: inline-block;
            font-size: 16px;
            color: #979ba5;
            vertical-align: top;
            cursor: pointer;
        }

        .plan-variable-empty {
            color: #b2b5bd;
        }
    }
</style>

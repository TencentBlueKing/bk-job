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
        id="templateOperation"
        class="template-operation-page">
        <resize-layout
            ref="resizeLayout"
            class="push-file-content"
            :right-fixed="true"
            :right-width="366">
            <smart-action offset-target="bk-form-content">
                <jb-form
                    ref="templateOperateRef"
                    v-test="{ type: 'form', value: 'template' }"
                    class="template-operation-form"
                    :model="formData"
                    :rules="rules">
                    <bk-alert
                        class="info"
                        :title="$t('template.「对作业模板的修改不会立即自动更新执行方案，需要由用户手动触发」')" />
                    <jb-form-item
                        :label="$t('template.模板名称')"
                        property="name"
                        required>
                        <jb-input
                            v-model="formData.name"
                            class="input form-item-content"
                            :maxlength="60"
                            :placeholder="$t('template.输入作业模板名称')" />
                    </jb-form-item>
                    <toggle-display style="margin-bottom: 20px;">
                        <jb-form-item
                            :label="$t('template.场景标签.label')"
                            property="tags">
                            <jb-tag-select
                                v-model="formData.tags"
                                class="input form-item-content"
                                :placeholder="$t('template.标签对资源的分类管理有很大帮助')" />
                        </jb-form-item>
                        <jb-form-item :label="$t('template.模板描述')">
                            <bk-input
                                v-model="formData.description"
                                class="template-desc-textarea form-item-content"
                                :maxlength="500"
                                :placeholder="$t('template.填写该模板的功能介绍等详细描述...')"
                                type="textarea" />
                        </jb-form-item>
                    </toggle-display>
                    <jb-form-item
                        :label="$t('template.全局变量.label')"
                        style="margin-bottom: 30px;">
                        <render-global-var
                            :list="formData.variables"
                            mode="operate"
                            @on-change="handleGlobalVariableChange" />
                    </jb-form-item>
                    <jb-form-item
                        :label="$t('template.作业步骤.label')"
                        property="steps"
                        required
                        style="margin-bottom: 30px;">
                        <render-task-step
                            ref="step"
                            :list="formData.steps"
                            mode="operation"
                            :variable="formData.variables"
                            @on-change="handleTaskStepChange" />
                    </jb-form-item>
                </jb-form>
                <template #action>
                    <bk-button
                        v-test="{ type: 'button', value: 'operationTemplateSubmit' }"
                        class="w120 mr10"
                        :loading="isSubmiting"
                        theme="primary"
                        @click="handlerSubmit">
                        {{ submitText }}
                    </bk-button>
                    <bk-button
                        v-test="{ type: 'button', value: 'operationTemplateCancel' }"
                        @click="handleCancel">
                        {{ $t('template.取消') }}
                    </bk-button>
                </template>
            </smart-action>
            <div
                id="globalVariableGuide"
                slot="right" />
        </resize-layout>
        <back-top :target="getScrollParent" />
    </div>
</template>
<script>
    import _ from 'lodash';

    import TaskManageService from '@service/task-manage';
    import TaskPlanService from '@service/task-plan';

    import { taskTemplateName } from '@utils/validator';

    import BackTop from '@components/back-top';
    import JbInput from '@components/jb-input';
    import JbTagSelect from '@components/jb-tag-select';
    import ResizeLayout from '@components/resize-layout';

    import RenderGlobalVar from '../common/render-global-var';
    import RenderTaskStep from '../common/render-task-step';

    import ToggleDisplay from './components/toggle-display';

    import I18n from '@/i18n';

    export default {
        name: '',
        components: {
            JbTagSelect,
            JbInput,
            BackTop,
            ResizeLayout,
            ToggleDisplay,
            RenderGlobalVar,
            RenderTaskStep,
        },
        data () {
            return {
                isLoading: true,
                isPlanListLoading: true,
                formData: {
                    name: '',
                    tags: [],
                    variables: [],
                    steps: [],
                    description: '',
                },
                planList: [],
                isSubmiting: false,
                execLoading: false,
            };
        },
        computed: {
            isSkeletonLoading () {
                return this.isLoading;
            },
        },
        watch: {
            formData: {
                handler  () {
                    if (this.isLoading) {
                        return;
                    }
                    this.hasChange = true;
                },
                deep: true,
            },
        },
        created () {
            this.taskId = this.$route.params.id || 0;
            this.isEdit = this.$route.name === 'templateEdit';
            this.isClone = this.$route.name === 'templateClone';
            // 是否默认显示步骤编辑框
            this.initShowStepId = Number(this.$route.params.stepId);

            // 编辑和克隆作业模板时需要获取模板数据
            if (this.$route.name !== 'templateCreate') {
                this.fetchData(true);
            }
            // 编辑作业模板需要获取模板对应的执行方案列表
            if (this.isEdit) {
                this.fetchPlanList();
            }
            this.submitText = this.isEdit ? I18n.t('template.保存') : I18n.t('template.提交');
            this.rules = {
                name: [
                    {
                        required: true,
                        message: I18n.t('template.模板名称必填'),
                        trigger: 'blur',
                    },
                    {
                        validator: taskTemplateName.validator,
                        message: taskTemplateName.message,
                        trigger: 'blur',
                    },
                    {
                        validator: this.checkName,
                        message: I18n.t('template.模板名已存在，请重新输入'),
                        trigger: 'blur',
                    },
                ],
                steps: [
                    {
                        validator: value => value.length && value.some(item => !item.delete),
                        message: I18n.t('template.作业步骤必填'),
                        trigger: 'blur',
                    },
                ],
            };
        },
        methods: {
            /**
             * @desc 获取模板详情
             * @param {Boolean} isFirst 是否是第一次执行
             */
            fetchData (isFirst = false) {
                this.isLoading = true;
                const requestHandler = this.isEdit ? TaskManageService.taskDetail : TaskManageService.taskClone;
                requestHandler({
                    id: this.taskId,
                }, {
                    permission: 'page',
                }).then((data) => {
                    const { name, description, tags, variables, stepList } = data;
                    this.formData = {
                        name,
                        description,
                        tags,
                        variables,
                        steps: stepList,
                    };
                    // 克隆模板提示密文变量
                    if (this.isClone) {
                        this.searchCiphertextVariable();
                    }
                    // 编辑执行步骤
                    if (isFirst && this.isEdit && this.initShowStepId > 0) {
                        setTimeout(() => {
                            this.$refs.step.clickStepByIndex(_.findIndex(stepList, ({ id }) => id === this.initShowStepId));
                        });
                    }
                    // 再次编辑
                    // 拉取模板最新数据
                    if (!isFirst) {
                        setTimeout(() => {
                            window.changeConfirm = false;
                        }, 100);
                    }
                })
                    .catch((error) => {
                        if ([
                            1,
                            400,
                        ].includes(error.code)) {
                            setTimeout(() => {
                                this.$router.push({
                                    name: 'taskList',
                                });
                            }, 3000);
                        }
                    })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 获取模板关联的执行方案
             */
            fetchPlanList () {
                this.isPlanListLoading = true;
                TaskPlanService.fetchTaskPlan({
                    id: this.taskId,
                }).then((data) => {
                    this.planList = Object.freeze(data);
                })
                    .finally(() => {
                        this.isPlanListLoading = false;
                    });
            },
            /**
             * @desc 验证作业模板的名
             * @param {String} name 作业模板名
             *
             */
            checkName (name) {
                return TaskManageService.taskCheckName({
                    id: this.isEdit ? this.taskId : 0,
                    name,
                });
            },
            getScrollParent () {
                return this
                    .$refs
                    .resizeLayout
                    .$el
                    .querySelector('.jb-resize-layout-left')
                    .querySelector('.scroll-faker-content');
            },
            /**
             * @desc 克隆作业模板时提示密文变量
             */
            searchCiphertextVariable () {
                const stack = [];
                
                this.formData.variables.forEach((current) => {
                    if (current.isPassword) {
                        stack.push(current.name);
                    }
                });
                if (stack.length < 1) {
                    return;
                }
                this.$bkInfo({
                    title: I18n.t('template.模板中包含密文变量，请重新设置值'),
                    subTitle: I18n.t('template.“密文”类型的变量经过特殊加密处理，为避免信息泄露，克隆后初始值不会还原，需用户重新设置。'),
                    okText: I18n.t('template.我知道了'),
                    extCls: 'password-variable-info',
                });
            },
            
            /**
             * @desc 删除全局变量-删除步骤中的变量引用
             * @param {Array} stepList 步骤列表
             * @param {Object} deleteMap 删除的变量名
             * @returns {Array}
             *
             * 忽略已经删除的步骤
             */
            syncStepVarialbeDelete (stepList, deleteMap) {
                let changeFlag = false;
                // 所有主机全局变量
                stepList.forEach((currentStep) => {
                    // 步骤已经删除
                    if (currentStep.delete) {
                        return;
                    }
                    if (currentStep.isFile) {
                        // 分发文件步骤
                        const { fileDestination, fileSourceList } = currentStep.fileStepInfo;
                        // 执行目标使用变量
                        const { server } = fileDestination;
                        if (deleteMap[server.variable]) {
                            server.variable = '';
                            currentStep.localValidator = false;
                            changeFlag = true;
                        }
                        // 源文件使用变量
                        fileSourceList.forEach((currentFile) => {
                            if (currentFile.fileType === 1) {
                                if (deleteMap[currentFile.host.variable]) {
                                    currentFile.host.variable = '';
                                    // 步骤使用的全局变量被删除，步骤需要被标记为待补全状态
                                    currentStep.localValidator = false;
                                    changeFlag = true;
                                }
                            }
                        });
                        return;
                    }
                    if (currentStep.isScript) {
                        // 脚本步骤
                        const { executeTarget } = currentStep.scriptStepInfo;
                        if (deleteMap[executeTarget.variable]) {
                            executeTarget.variable = '';
                            // 步骤使用的全局变量被删除，步骤需要被标记为待补全状态
                            currentStep.localValidator = false;
                            changeFlag = true;
                        }
                    }
                });
                return changeFlag ? [...stepList] : stepList;
            },
            /**
             * @desc 修改全局变量名-同步步骤中引用的变量
             * @param {Array} stepList 步骤列表
             * @param {Object} renameMap 全局变量对应的最新名字
             * @returns {Array}
             *
             * 忽略已经删除的步骤
             */
            syncStepVariableRename (stepList, renameMap) {
                stepList.forEach((currentStep) => {
                    // 步骤已经删除
                    if (currentStep.delete) {
                        return;
                    }
                    // 分发文件步骤
                    if (currentStep.isFile) {
                        // 执行目标使用全局变量
                        const { fileDestination, fileSourceList } = currentStep.fileStepInfo;
                        const { server } = fileDestination;
                        if (renameMap[server.variable]) {
                            server.variable = renameMap[server.variable];
                        }
                        // 源文件使用全局变量
                        fileSourceList.forEach((currentFile) => {
                            if (currentFile.fileType === 1) {
                                // 服务器文件
                                if (renameMap[currentFile.host.variable]) {
                                    currentFile.host.variable = renameMap[currentFile.host.variable];
                                }
                            }
                        });
                        return;
                    }
                    // 脚本步骤
                    if (currentStep.isScript) {
                        // 执行目标使用全局变量
                        const { executeTarget } = currentStep.scriptStepInfo;
                        if (renameMap[executeTarget.variable]) {
                            executeTarget.variable = renameMap[executeTarget.variable];
                        }
                    }
                });
                return [...stepList];
            },
            /**
             * @desc 全局变量更新
             * @param {Array} variableList 最新的变量列表
             *
             * 更新主机变量名
             *  -自动替换步骤中已经引用的全局变量
             * 删除主机变量
             *  -将使用了该全局变量的步骤标记为待补全
             */
            handleGlobalVariableChange (variableList) {
                const newVariableMap = variableList.reduce((result, variable) => {
                    result[variable.id] = {
                        name: variable.name,
                        delete: variable.delete,
                    };
                    return result;
                }, {});
                // 主机变量修改变量名自动同步步骤里面的变量引用
                const renameMap = {};
                // 主机变量被删除同步删除步骤里面的引用记录
                const deleteMap = {};
                this.formData.variables.forEach((variable) => {
                    if (!variable.isHost) {
                        return;
                    }
                    const {
                        id,
                        name,
                    } = variable;
                    // 全局变量被删除
                    if (!newVariableMap[id] || newVariableMap[id].delete) {
                        deleteMap[name] = true;
                        return;
                    }
                    // 修改了变量
                    if (newVariableMap[id].name !== name) {
                        renameMap[name] = newVariableMap[id].name;
                    }
                });
                // 优先同步删除操作
                let stepList = this.syncStepVarialbeDelete(this.formData.steps, deleteMap);
                // 同步改名操作
                stepList = this.syncStepVariableRename(stepList, renameMap);
                this.formData.variables = variableList;
                this.formData.steps = stepList;
            },
            /**
             * @desc 步骤更新
             * @param {Array} steps 最新的步骤列表
             */
            handleTaskStepChange (steps) {
                this.formData.steps = steps;
                this.$refs.templateOperateRef.clearError();
            },
            /**
             * @desc 保存作业模板
             *
             * 需要对作模板数据做逻辑验证处理
             * - 步骤的基本数据是否完整
             */
            handlerSubmit () {
                // eslint-disable-next-line no-plusplus
                for (let i = 0; i < this.formData.steps.length; i++) {
                    if (this.formData.steps[i].delete !== 1
                        && this.formData.steps[i].localValidator === false) {
                        this.messageError(I18n.t('template.请将「待补全」的步骤信息完善后提交重试'));
                        return;
                    }
                }
                // 提交作业模板
                // 再主动拉取作业模板对应的执行方案列表，判断执行方案是否为空和是否需要同步
                this.isSubmiting = true;
                this.$refs.templateOperateRef.validate()
                    .then(() => TaskManageService.taskUpdate({
                        ...this.formData,
                        id: this.isEdit ? this.taskId : 0,
                    }).then((taskId) => {
                        window.changeConfirm = false;
                        return TaskPlanService.fetchTaskPlan({
                            id: taskId,
                        }).then((planList) => {
                            let planSync = false;
                            // eslint-disable-next-line no-plusplus
                            for (let i = 0; i < planList.length; i++) {
                                if (planList[i].needUpdate) {
                                    planSync = true;
                                    break;
                                }
                            }
                            this.planList = Object.freeze(planList);
                            if (this.isEdit) {
                                this.editSuccessCallback(taskId, planSync);
                            } else {
                                this.createSuccessCallback(taskId);
                            }
                        });
                    }))
                    .finally(() => {
                        this.isSubmiting = false;
                    });
            },
            /**
             * @desc 创建作业模板成功
             * @param {Number} taskId 作业模板id
             */
            createSuccessCallback (taskId) {
                let confirmInfo = '';
                let isClickClose = false;
                const handleGoTemplateEdit = () => {
                    this.$router.push({
                        name: 'templateEdit',
                        params: {
                            id: taskId,
                        },
                    });
                    isClickClose = true;
                    confirmInfo.close();
                };
                const handleGoTemplateDetail = () => {
                    this.$router.push({
                        name: 'templateDetail',
                        params: {
                            id: taskId,
                        },
                    });
                    isClickClose = true;
                    confirmInfo.close();
                };
                const handleGoPlan = () => {
                    this.$router.push({
                        name: 'viewPlan',
                        params: {
                            templateId: taskId,
                        },
                        query: {
                            mode: 'create',
                        },
                    });
                    isClickClose = true;
                    confirmInfo.close();
                };
                
                const subHeader = () => (
                <div>
                    <p style={{ marginBottom: '10px', color: '#979BA5' }}>
                        {I18n.t('template.还差一步「 设置执行方案」，即可执行作业')}
                    </p>
                    <p>
                        <bk-button
                            style={{ marginRight: '10px' }}
                            text
                            onClick={handleGoTemplateEdit}>
                            {I18n.t('template.继续编辑')}
                        </bk-button>
                        <bk-button
                            style={{ marginRight: '10px' }}
                            text
                            onClick={handleGoTemplateDetail}>
                            {I18n.t('template.立即查看')}
                        </bk-button>
                        <bk-button
                            text
                            onClick={handleGoPlan}>
                            {I18n.t('template.设置方案')}
                        </bk-button>
                    </p>
                </div>
                );

                confirmInfo = this.$bkInfo({
                    type: 'success',
                    title: I18n.t('template.作业创建成功'),
                    showFooter: false,
                    subHeader: subHeader(),
                    cancelFn: () => {
                        if (isClickClose) {
                            return;
                        }
                        this.$router.push({
                            name: 'templateEdit',
                            params: {
                                id: taskId,
                            },
                        });
                    },
                });
            },
            /**
             * @desc 编辑作业模板成功
             * @param {Number} taskId 作业模板id
             * @param {Boolean} planSync 执行方案是否需要同步
             */
            editSuccessCallback (taskId, planSync) {
                let confirmInfo = '';
                let isClickClose = false;
                const handleGoTemplateDetail = () => {
                    this.$router.push({
                        name: 'templateDetail',
                        params: {
                            id: taskId,
                        },
                    });
                    isClickClose = true;
                    confirmInfo.close();
                };
                const handleGoSync = () => {
                    this.$router.push({
                        name: 'syncPlanBatch',
                        query: {
                            planIds: this.planList.map(_ => _.id).join(','),
                            from: 'templateEdit',
                        },
                    });
                    isClickClose = true;
                    confirmInfo.close();
                };
                const handleGoPlan = () => {
                    this.$router.push({
                        name: 'viewPlan',
                        params: {
                            templateId: taskId,
                        },
                        query: {
                            from: 'templateDetail',
                        },
                    });
                    isClickClose = true;
                    confirmInfo.close();
                };
                const subHeader = () => (
                <div>
                    <p style={{ marginBottom: '10px', color: '#979BA5' }}>
                        {I18n.t('template.可以通过 “立即同步” 入口前往更新所有执行方案')}
                    </p>
                    <p>
                        <bk-button
                            class="mr10"
                            text
                            onClick={handleGoTemplateDetail}>
                            {I18n.t('template.返回查看')}
                        </bk-button>
                        <bk-button
                            class="mr10"
                            text
                            disabled={!planSync}
                            onClick={handleGoSync}>
                            {I18n.t('template.立即同步')}
                        </bk-button>
                        <bk-button
                            text
                            onClick={handleGoPlan}>
                            {I18n.t('template.查看方案')}
                        </bk-button>
                    </p>
                </div>
                );

                confirmInfo = this.$bkInfo({
                    type: 'success',
                    title: I18n.t('template.编辑保存成功'),
                    showFooter: false,
                    subHeader: subHeader(),
                    cancelFn: () => {
                        if (isClickClose) {
                            return;
                        }
                        this.fetchData(false);
                    },
                });
            },
            /**
             * @desc 取消
             */
            handleCancel () {
                this.routerBack();
            },
            /**
             * @desc 路由回退
             */
            routerBack () {
                const { from } = this.$route.query;
                if (from === 'templateDetail') {
                    this.$router.push({
                        name: 'templateDetail',
                        params: {
                            id: this.taskId,
                        },
                    });
                    return;
                }
                this.$router.push({
                    name: 'taskList',
                });
            },
        },
    };
</script>
<style lang='postcss'>
    @import "@/css/mixins/media";

    .template-operation-page {
        .push-file-content {
            height: calc(100vh - 104px);

            .jb-resize-layout-right {
                background: #fff;

                .variable-use-guide {
                    height: calc(100vh - 104px);
                    padding-bottom: 52px;
                }
            }
        }

        .template-operation-form {
            padding: 20px 24px 0;
        }

        .info {
            margin-bottom: 20px;
        }

        .template-desc-textarea {
            .bk-textarea-wrapper .bk-form-textarea.textarea-maxlength {
                margin-bottom: 0;
            }

            .bk-form-textarea {
                min-height: 86px;
            }
        }

        .form-item-content {
            @media (--small-viewports) {
                width: 500px;
            }

            @media (--medium-viewports) {
                width: 560px;
            }

            @media (--large-viewports) {
                width: 620px;
            }

            @media (--huge-viewports) {
                width: 680px;
            }
        }
    }

    .invalid-variable-info,
    .password-variable-info {
        .bk-dialog-content {
            width: 500px !important;
        }
    }

    .password-variable-info {
        .bk-dialog-header-inner {
            white-space: normal !important;
        }

        .bk-default {
            display: none;
        }
    }
</style>

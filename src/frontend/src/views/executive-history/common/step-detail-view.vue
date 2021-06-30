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
    <div v-bkloading="{ isLoading }">
        <task-step-view :variable="variableList" :data="stepInfo" />
    </div>
</template>
<script>
    import TaskExecuteService from '@service/task-execute';
    import TaskStepModel from '@model/task/task-step';
    import GlobalVariableModel from '@model/task/global-variable';
    import TaskStepView from '@views/task-manage/common/render-task-step/task-step-view';

    export default {
        name: '',
        components: {
            TaskStepView,
        },
        props: {
            taskId: {
                type: Number,
                required: true,
            },
            id: {
                type: [Number, String],
            },
        },
        data () {
            return {
                isLoading: true,
                stepInfo: {},
                variableList: [],
            };
        },
        created () {
            this.isLoading = true;
            Promise.all([
                this.fetchStep(),
                this.fetchTaskVariables(),
            ]).finally(() => {
                this.isLoading = false;
            });
        },
        methods: {
            //  步骤详情
            fetchStep () {
                return TaskExecuteService.fetchStepInstance({
                    id: this.id,
                }).then((data) => {
                    this.stepInfo = Object.freeze(new TaskStepModel(data));
                });
            },
            fetchTaskVariables () {
                return TaskExecuteService.fetchStepInstanceParam({
                    id: this.taskId,
                }).then((data) => {
                    this.variableList = Object.freeze(data.map(({ id, name, type, value, targetValue }) => new GlobalVariableModel({
                        id,
                        name,
                        type,
                        defaultValue: value,
                        defaultTargetValue: targetValue,
                    })));
                });
            },
        },
    };
</script>

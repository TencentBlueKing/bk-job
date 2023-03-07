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
    class="setting-variable">
    <h2 class="title">
      {{ $t('history.全局变量') }}
    </h2>
    <smart-action offset-target="variable-value">
      <global-variable-layout>
        <global-variable
          v-for="variable in taskVariables"
          :key="variable.id"
          ref="variable"
          :data="variable"
          :type="variable.type" />
      </global-variable-layout>
      <template #action>
        <bk-button
          class="w120 mr10"
          :loading="isSubmiting"
          theme="primary"
          @click="handleGoExec">
          {{ $t('history.执行') }}
        </bk-button>
        <bk-button @click="handleCancle">
          {{ $t('history.取消') }}
        </bk-button>
      </template>
    </smart-action>
    <element-teleport v-if="taskName">
      <div style="font-size: 12px; color: #63656e;">
        （{{ taskName }}）
      </div>
    </element-teleport>
  </div>
</template>
<script>
  import TaskExecuteService from '@service/task-execute';

  import TaskHostNodeModel from '@model/task-host-node';

  import GlobalVariable from '@components/global-variable/edit';
  import GlobalVariableLayout from '@components/global-variable/layout';

  import I18n from '@/i18n';

  export default {
    name: '',
    components: {
      GlobalVariableLayout,
      GlobalVariable,
    },
    data() {
      return {
        taskName: '',
        taskVariables: [],
        isLoading: true,
        isSubmiting: false,
        templateId: 0,
        taskId: 0,
      };
    },
    computed: {
      isSkeletonLoading() {
        return this.isLoading;
      },
    },
    created() {
      this.taskInstanceId = this.$route.params.taskInstanceId;
      this.fetchData();
    },
    methods: {
      /**
       * @desc 获取任务详情数据
       */
      fetchData() {
        TaskExecuteService.fetchTaskInstance({
          id: this.taskInstanceId,
        }).then((data) => {
          this.taskName = data.taskInstance.name;
          this.taskVariables = Object.freeze(data.variables.map(({
            id,
            name,
            type,
            value,
            targetValue,
            changeable,
          }) => ({
            id,
            name,
            type,
            defaultValue: value,
            defaultTargetValue: new TaskHostNodeModel(targetValue || {}),
            changeable,
          })));
        })
          .finally(() => {
            this.isLoading = false;
          });
      },
      /**
       * @desc 任务重做执行
       */
      handleGoExec() {
        if (!this.$refs.variable) {
          return;
        }
        Promise.all(this.$refs.variable.map(item => item.validate()))
          .then((taskVariables) => {
            this.isSubmiting = true;
            TaskExecuteService.redoTask({
              taskInstanceId: this.taskInstanceId,
              taskVariables: taskVariables.map(({ id, name, type, value, targetValue }) => ({
                id,
                name,
                type,
                value,
                targetValue,
              })),
            }).then(({ taskInstanceId }) => {
              this.$bkMessage({
                theme: 'success',
                message: I18n.t('history.操作成功'),
              });
              window.changeFlag = false;
              this.$router.push({
                name: 'historyTask',
                params: {
                  id: taskInstanceId,
                },
              });
            })
              .catch(() => {
                this.isSubmiting = false;
              });
          });
      },
      /**
       * @desc 取消重做
       */
      handleCancle() {
        this.routerBack();
      },
      /**
       * @desc 路由回退
       */
      routerBack() {
        this.$router.push({
          name: 'historyList',
        });
      },
    },
  };
</script>
<style lang='postcss'>
  .setting-variable {
    .title {
      margin-bottom: 20px;
      font-size: 14px;
      line-height: 1;
      color: #313238;
    }

    .variable-list {
      display: inline-block;
    }
  }

  .setting-wariable-operate {
    margin-left: 88px;
  }
</style>

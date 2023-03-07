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
    class="execute-global-variable">
    <template v-if="!isLoading">
      <global-variable-layout v-if="taskVariables.length > 0">
        <global-variable
          v-for="variable in taskVariables"
          :key="variable.id"
          ref="variable"
          :data="variable"
          :layout="variable.type === 3 ? 'vertical' : ''"
          :type="variable.type" />
      </global-variable-layout>
      <empty
        v-else
        class="empty" />
    </template>
  </div>
</template>
<script>
  import TaskExecuteService from '@service/task-execute';

  import TaskHostNodeModel from '@model/task-host-node';

  import Empty from '@components/empty';
  import GlobalVariableLayout from '@components/global-variable/layout';
  import GlobalVariable from '@components/global-variable/view';

  export default {
    name: '',
    components: {
      GlobalVariableLayout,
      GlobalVariable,
      Empty,
    },
    props: {
      id: {
        type: [Number, String],
        default: 0,
      },
    },
    data() {
      return {
        isLoading: false,
        taskVariables: [],
      };
    },
    computed: {
      isShowVar() {
        return this.taskVariables.length > 0;
      },
    },
    watch: {
      id: {
        handler(id) {
          if (!id) {
            return;
          }
          this.fetchTaskVariables(id);
        },
        immediate: true,
      },
    },
    methods: {
      fetchTaskVariables(id) {
        this.$request(TaskExecuteService.fetchStepInstanceParam({
          id,
        }), () => {
          this.isLoading = true;
        }).then((data) => {
          this.taskVariables = Object.freeze(data.map(({ id, name, type, value, targetValue }) => ({
            id,
            name,
            type,
            defaultValue: value,
            defaultTargetValue: new TaskHostNodeModel(targetValue || {}),
          })));
        })
          .finally(() => {
            this.isLoading = false;
          });
      },
    },
  };
</script>
<style lang='postcss' scoped>
  .execute-global-variable {
    min-height: calc(100vh - 120px);
  }

  .empty {
    padding-top: 80px;
  }
</style>

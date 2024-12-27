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
    class="history-step-detail-view">
    <task-step-view
      :data="stepInfo"
      :variable="variableList">
      <detail-item
        v-if="rollingConfigExpr"
        :label="$t('history.滚动策略：')">
        <span
          v-bk-tooltips.right="rollingExprParse(rollingConfigExpr)"
          class="tips">
          {{ rollingConfigExpr }}
        </span>
      </detail-item>
      <detail-item
        v-if="rollingModeText"
        :label="$t('history.滚动机制：')">
        {{ rollingModeText }}
      </detail-item>
    </task-step-view>
  </div>
</template>
<script>
  import TaskExecuteService from '@service/task-execute';

  import GlobalVariableModel from '@model/task/global-variable';
  import TaskStepModel from '@model/task/task-step';

  import rollingExprParse from '@utils/rolling-expr-parse';

  import TaskStepView from '@views/task-manage/common/render-task-step/task-step-view';

  import DetailItem from '@components/detail-layout/item';

  import I18n from '@/i18n';

  export default {
    name: '',
    components: {
      TaskStepView,
      DetailItem,
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
    data() {
      return {
        isLoading: true,
        stepInfo: {},
        rollingConfigExpr: '',
        rollingModeText: '',
        variableList: [],
      };
    },
    created() {
      this.isLoading = true;
      Promise.all([
        this.fetchStep(),
        this.fetchTaskVariables(),
      ]).finally(() => {
        this.isLoading = false;
      });
    },
    methods: {
      /**
       * @desc 解析滚动配置
       * @param { String } expr
       * @returns { String }
       */
      rollingExprParse(expr) {
        return rollingExprParse(expr);
      },
      //  步骤详情
      fetchStep() {
        return TaskExecuteService.fetchStepInstance({
          id: this.id,
        }).then((data) => {
          if (data.rollingEnabled) {
            this.rollingConfigExpr = data.rollingConfig.expr;
            const modeMap = {
              1: I18n.t('默认（执行失败则暂停）'),
              2: I18n.t('忽略失败，自动滚动下一批'),
              3: I18n.t('不自动，每批次都人工确认'),
            };
            this.rollingModeText = modeMap[data.rollingConfig.mode];
          }

          this.stepInfo = Object.freeze(new TaskStepModel(data));
        });
      },
      fetchTaskVariables() {
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

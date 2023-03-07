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
  <detail-layout
    class="detail-layout-wrapper"
    mode="see">
    <detail-item :label="$t('template.步骤类型：')">
      {{ stepTypeText }}
    </detail-item>
    <detail-item :label="$t('template.步骤名称：')">
      {{ data.name }}
    </detail-item>
    <component
      :is="stepCom"
      ref="stepCom"
      :data="data"
      :variable="variable">
      <slot />
    </component>
  </detail-layout>
</template>
<script>
  import DetailLayout from '@components/detail-layout';
  import DetailItem from '@components/detail-layout/item';

  import StepApproval from './approval';
  import StepDistroFile from './distro-file';
  import StepExecScript from './exec-script';

  import I18n from '@/i18n';

  const STEP_TYPE_LIST = {
    1: I18n.t('template.执行脚本'),
    2: I18n.t('template.分发文件'),
    3: I18n.t('template.人工确认'),
  };

  export default {
    components: {
      StepDistroFile,
      StepExecScript,
      StepApproval,
      DetailLayout,
      DetailItem,
    },
    props: {
      variable: {
        type: Array,
        default: () => [],
      },
      data: {
        type: Object,
        default: () => ({}),
      },
    },
    computed: {
      stepTypeText() {
        return STEP_TYPE_LIST[this.data.type];
      },
      stepCom() {
        const taskStepMap = {
          1: StepExecScript,
          2: StepDistroFile,
          3: StepApproval,
        };
        if (!Object.prototype.hasOwnProperty.call(taskStepMap, this.data.type)) {
          return 'div';
        }
        return taskStepMap[this.data.type];
      },
    },
  };
</script>
<style lang="postcss" scoped>
  .detail-layout-wrapper {
    .detail-item {
      margin-bottom: 0;
    }
  }
</style>

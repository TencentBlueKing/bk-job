<!--
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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
  <ip-detail
    :last-host="lastHost"
    :pre-host="preHost" />
</template>
<script>
  import {
    findParent,
  } from '@utils/vdom';

  import IpDetail from '../../common/ip-detail';
  import {
    findStep,
  } from '../../common/utils';

  export default {
    name: 'SyncPlanFileExecuteTarget',
    components: {
      IpDetail,
    },
    data() {
      return {
        preHost: {},
        lastHost: {},
      };
    },
    created() {
      const dataSourceParent = findParent(this, 'SyncPlanStep2');
      const currentStep = findParent(this, 'DiffTaskStep');
      const currentStepId = currentStep.data.realId;
      const currentPlanStep = findStep(dataSourceParent.planStepList, currentStepId);
      const currentTemplateStep = findStep(dataSourceParent.templateStepList, currentStepId);

      this.preHost = Object.freeze(currentPlanStep.fileStepInfo.fileDestination.server);
      this.lastHost = Object.freeze(currentTemplateStep.fileStepInfo.fileDestination.server);
    },
  };
</script>

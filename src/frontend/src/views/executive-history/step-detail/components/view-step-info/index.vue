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
  <lower-component>
    <div>
      <div
        v-bk-tooltips.bottom="$t('history.步骤内容')"
        class="action-btn"
        @click="handleShowStepDetail">
        <icon type="detail-line" />
      </div>
      <jb-sideslider
        :is-show.sync="isShow"
        quick-close
        :show-footer="false"
        :title="$t('history.查看步骤内容')"
        :width="960">
        <render-step
          :id="stepInstanceId"
          :task-id="taskInstanceId" />
      </jb-sideslider>
    </div>
  </lower-component>
</template>
<script setup>
  import { ref } from 'vue';

  import RenderStep from './components/render-step.vue';

  defineProps({
    taskInstanceId: {
      type: Number,
      required: true,
    },
    stepInstanceId: {
      type: Number,
      required: true,
    },
  });

  const isShow = ref(false);

  const handleShowStepDetail = () => {
    isShow.value = true;
  };
</script>

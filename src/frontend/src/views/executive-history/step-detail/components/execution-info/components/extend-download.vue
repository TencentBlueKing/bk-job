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
    v-if="activePanel === 'scriptLog'"
    v-bk-tooltips="$t('history.下载日志')"
    class="extend-item"
    @click="handleDownload">
    <icon type="download" />
  </div>
</template>
<script setup>
  import { getCurrentInstance } from 'vue';

  import TaskExecuteService from '@service/task-execute';

  import I18n from '@/i18n';

  const props = defineProps({
    activePanel: {
      type: String,
      required: true,
    },
    taskInstanceId: {
      type: Number,
      required: true,
    },
    stepInstanceId: {
      type: Number,
      required: true,
    },
    executeObject: {
      type: Object,
    },
  });

  const currentInstance = getCurrentInstance();

  const handleDownload = () => {
    TaskExecuteService.fetchStepExecutionLogFile({
      taskInstanceId: props.taskInstanceId,
      stepInstanceId: props.stepInstanceId,
      executeObjectType: props.executeObject.type,
      executeObjectResourceId: props.executeObject.executeObjectResourceId,
    }).then(() => {
      currentInstance.proxy.$bkMessage({
        theme: 'success',
        message: I18n.t('history.导出日志操作成功'),
      });
    });
  };
</script>

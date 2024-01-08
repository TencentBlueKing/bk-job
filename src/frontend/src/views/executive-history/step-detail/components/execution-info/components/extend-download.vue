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

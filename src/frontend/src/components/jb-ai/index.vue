<template>
  <div>
    <ai-blueking
      v-show="isBluekingShow"
      ref="aiRef"
      :url="apiUrl" />
  </div>
</template>
<script setup>
  import _ from 'lodash';
  import { ref } from 'vue';

  import AiService from '@service/ai';

  import eventBus from '@utils/event-bus';

  import AiBlueking from '@blueking/ai-blueking/vue2';

  import '@blueking/ai-blueking/dist/vue2/style.css';


  const aiRef = ref();
  const apiUrl = ref('');
  const isBluekingShow = ref(false);

  AiService.fetchConfig()
    .then((data) => {
      apiUrl.value = data.agentRootUrl;
    });

  const handleShowBlueking = async (commandName, params) => {
    const originalCommand = _.find((aiRef.value?.agentInfo?.conversationSettings?.commands || []), item => item.id === commandName);


    if (!originalCommand?.components) {
      console.error('AI 命令配置不完整', aiRef.value?.agentInfo);
    }

    // 深拷贝 command 对象，避免修改原对象
    const command = {
      ...originalCommand,
      components: originalCommand.components.map(item => ({
        ...item,
        default: params[item.key],
      })),
    };


    try {
      await aiRef.value?.handleShow(undefined, {  showFirst: true });
      aiRef.value?.handleShortcutClick({
        shortcut: command,
        source: 'popup',
      }, true);
    } catch (error) {
      console.error('AI 分析失败:', error);
    }
  };


  eventBus.$on('ai:generaChat', () => {
    aiRef.value?.handleShow();
  });

  eventBus.$on('ai:checkScript', (params) => {
    handleShowBlueking('checkScript', params);
  });

  eventBus.$on('ai:analyzeScriptTaskError', (params) => {
    handleShowBlueking('analyzeScriptTaskError', params);
  });

  eventBus.$on('ai:analyzeFileTaskError', (params) => {
    handleShowBlueking('analyzeFileTaskError', params);
  });

</script>
<style lang="postcss">
.ai-blueking-wrapper{
  .shortcuts-bar{
    display: none;
  }
}
</style>


<template>
  <ai-blueking
    v-if="apiUrl"
    ref="aiRef"
    :default-width="defaultWidth"
    :url="apiUrl" />
</template>
<script setup>
  import _ from 'lodash';
  import { onMounted, ref } from 'vue';

  import { useRoute } from '@router';

  import AiService from '@service/ai';

  import eventBus from '@utils/event-bus';

  import AiBlueking from '@blueking/ai-blueking/vue2';

  import '@blueking/ai-blueking/dist/vue2/style.css';


  const defaultWidth = window.innerWidth * 0.33;
  const route = useRoute();

  const aiRef = ref();
  const apiUrl = ref('');


  AiService.fetchConfig()
    .then((data) => {
      apiUrl.value = data.agentRootUrl;
      setTimeout(() => {
        console.log('AI 组件已挂载', aiRef.value);
      }, 1000);
    });

  const handleShowBlueking = async (commandName, params, sessionCode, showOptions = {}) => {
    const chatHelper = aiRef.value?.getChatHelper?.();
    const originalCommand = chatHelper?.agent.info.value?.conversationSettings?.commands;

    console.log('原始命令配置', aiRef.value.getChatHelper(), originalCommand);

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

    const sessionList = await aiRef.value?.getSessionList();
    if (!_.find(sessionList, item => item.sessionCode === sessionCode)) {
      // 存在会话，无需创建
      await aiRef.value?.addNewSession();
    }


    await aiRef.value?.handleShow(sessionCode, {  showFirst: true, ...showOptions });
    aiRef.value?.handleShortcutClick({
      shortcut: command,
      source: 'popup',
    }, true);
  };


  eventBus.$on('ai:generaChat', async () => {
    const curentScope =  `${window.PROJECT_CONFIG.SCOPE_TYPE}/${window.PROJECT_CONFIG.SCOPE_ID}`;
    const sceneType = 3;
    const sessionMemo = await AiService.fetchChatSession({
      sceneType,
      sceneResourceId: curentScope,
    });
    aiRef.value?.handleShow(sessionMemo?.aiSessionId);
    if (!sessionMemo?.aiSessionId) {
      const sessionList = await aiRef.value?.getSessionList();
      AiService.updateChatSession({
        sceneType,
        sceneResourceId: curentScope,
        aiSessionId: sessionList[0]?.sessionCode,
        sessionName: sessionList[0]?.sessionName,
      });
    }
  });

  eventBus.$on('ai:checkScript', (params) => {
    handleShowBlueking('checkScript', params, undefined, {
      isTemporary: false,
    });
  });

  eventBus.$on('ai:checkScriptVersion', async (params) => {
    const currentScriptVersionId = route.value.params.id;
    const sceneType = 2;
    const sessionMemo = await AiService.fetchChatSession({
      sceneType,
      sceneResourceId: currentScriptVersionId,
    });
    await handleShowBlueking('checkScript', params, sessionMemo?.aiSessionId);
    if (!sessionMemo?.aiSessionId) {
      const sessionList = await aiRef.value?.getSessionList();
      AiService.updateChatSession({
        sceneType,
        sceneResourceId: currentScriptVersionId,
        aiSessionId: sessionList[0]?.sessionCode,
        sessionName: sessionList[0]?.sessionName,
      });
    }
  });

  eventBus.$on('ai:analyzeScriptTaskError', async (params) => {
    const currrentStepInstanceId = route.value.query.stepInstanceId;
    const sessionMemo = await AiService.fetchChatSession({
      sceneType: 1,
      sceneResourceId: currrentStepInstanceId,
    });
    await handleShowBlueking('analyzeScriptTaskError', params, sessionMemo?.aiSessionId);
    if (!sessionMemo?.aiSessionId) {
      const sessionList = await aiRef.value?.getSessionList();
      AiService.updateChatSession({
        sceneType: 1,
        sceneResourceId: currrentStepInstanceId,
        aiSessionId: sessionList[0]?.sessionCode,
        sessionName: sessionList[0]?.sessionName,
      });
    }
  });

  eventBus.$on('ai:analyzeFileTaskError', async (params) => {
    const currrentStepInstanceId = route.value.query.stepInstanceId;
    const sessionMemo = await AiService.fetchChatSession({
      sceneType: 1,
      sceneResourceId: currrentStepInstanceId,
    });
    await handleShowBlueking('analyzeFileTaskError', params, sessionMemo?.aiSessionId);
    if (!sessionMemo?.aiSessionId) {
      const sessionList = await aiRef.value?.getSessionList();
      AiService.updateChatSession({
        sceneType: 1,
        sceneResourceId: currrentStepInstanceId,
        aiSessionId: sessionList[0]?.sessionCode,
        sessionName: sessionList[0]?.sessionName,
      });
    }
  });

  onMounted(() => {
    console.log('AI 组件已挂载', aiRef.value);
  });
</script>
<style lang="postcss">
.ai-blueking-wrapper{
  .shortcuts-bar{
    display: none;
  }
}
</style>


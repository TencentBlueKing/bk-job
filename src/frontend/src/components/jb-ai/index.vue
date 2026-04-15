<template>
  <ai-blueking
    v-if="apiUrl"
    ref="aiRef"
    :before-nimbus-click="handleBeforeNimbusClick"
    :default-width="defaultWidth"
    :url="apiUrl" />
</template>
<script setup>
  import _ from 'lodash';
  import { ref } from 'vue';

  import { useRoute } from '@router';

  import AiService from '@service/ai';

  import eventBus from '@utils/event-bus';

  import AiBlueking from '@blueking/ai-blueking/vue2';

  import '@blueking/ai-blueking/dist/vue2/style.css';


  const defaultWidth = window.innerWidth * 0.33;
  const route = useRoute();

  const curentScope =  `${window.PROJECT_CONFIG.SCOPE_TYPE}__${window.PROJECT_CONFIG.SCOPE_ID}`;

  const getSearchParams = () => {
    const curSearchParams = new URLSearchParams(window.location.search);
    return Array.from(curSearchParams.keys()).reduce(
      (result, key) => ({
        ...result,
        [key]: curSearchParams.get(key) || '',
      }),
      {},
    );
  };

  const aiRef = ref();
  const apiUrl = ref('');


  AiService.fetchConfig()
    .then((data) => {
      apiUrl.value = data.agentRootUrl;
    });

  const handleBeforeNimbusClick = async () => {
    const sceneType = 3;
    const sessionMemo = await AiService.fetchChatSession({
      sceneType,
      sceneResourceId: curentScope,
    });

    aiRef.value?.show();
    const chatHelper = aiRef.value.getChatHelper();
    if (sessionMemo?.aiSessionId && _.find(chatHelper.session.list.value, item => item.sessionCode === sessionMemo?.aiSessionId)) {
      // 切换到目标会话
      await chatHelper.session.chooseSession(sessionMemo?.aiSessionId);
    } else {
      await chatHelper.session.createSession({
        sessionCode: `${Date.now()}_${_.random(1000, 9999)}`,
        sessionName: '新会话',
      });
    }
    if (!sessionMemo?.aiSessionId) {
      const currentSession = aiRef.value.getChatHelper().session.current.value;
      AiService.updateChatSession({
        sceneType,
        sceneResourceId: curentScope,
        aiSessionId: currentSession.sessionCode,
        sessionName: currentSession.sessionName,
      });
    }
    return false;
  };

  const handleShowBlueking = async (commandName, params, sessionCode, createOptions = {}) => {
    const chatHelper = aiRef.value.getChatHelper();
    const originalCommand = _.find(chatHelper?.agent.info.value?.conversationSettings?.commands, item => item.id === commandName);

    aiRef.value?.show();

    if (chatHelper.session.current.value?.sessionCode !== sessionCode && _.find(chatHelper.session.list.value, item => item.sessionCode
      === sessionCode)) {
      // 切换到目标会话
      await chatHelper.session.chooseSession(sessionCode);
    } else {
      await chatHelper.session.createSession({
        sessionCode: `${Date.now()}_${_.random(1000, 9999)}`,
        sessionName: '新会话',
        ...createOptions,
      });
    }

    aiRef.value?.sendShortcut({
      ...originalCommand,
      components: originalCommand.components.map(item => ({
        ...item,
        default: params[item.key],
      })),
    });
  };


  eventBus.$on('ai:generaChat', async () => {
    const sceneType = 3;
    const sessionMemo = await AiService.fetchChatSession({
      sceneType,
      sceneResourceId: curentScope,
    });
    aiRef.value?.handleShow(sessionMemo?.aiSessionId);
    if (!sessionMemo?.aiSessionId) {
      const currentSession = aiRef.value.getChatHelper().session.current.value;
      AiService.updateChatSession({
        sceneType,
        sceneResourceId: curentScope,
        aiSessionId: currentSession.sessionCode,
        sessionName: currentSession.sessionName,
      });
    }
  });

  eventBus.$on('ai:checkScript', async (params) => {
    handleShowBlueking('checkScript', params, undefined, {
      isTemporary: true,
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
      const currentSession = aiRef.value.getChatHelper().session.current.value;
      AiService.updateChatSession({
        sceneType,
        sceneResourceId: currentScriptVersionId,
        aiSessionId: currentSession.sessionCode,
        sessionName: currentSession.sessionName,
      });
    }
  });

  eventBus.$on('ai:analyzeScriptTaskError', async (params) => {
    const currrentStepInstanceId = getSearchParams().stepInstanceId;

    const sessionMemo = await AiService.fetchChatSession({
      sceneType: 1,
      sceneResourceId: currrentStepInstanceId,
    });
    await handleShowBlueking('analyzeScriptTaskError', params, sessionMemo?.aiSessionId);
    if (!sessionMemo?.aiSessionId) {
      const currentSession = aiRef.value.getChatHelper().session.current.value;
      AiService.updateChatSession({
        sceneType: 1,
        sceneResourceId: currrentStepInstanceId,
        aiSessionId: currentSession.sessionCode,
        sessionName: currentSession.sessionName,
      });
    }
  });

  eventBus.$on('ai:analyzeFileTaskError', async (params) => {
    const currrentStepInstanceId = getSearchParams().stepInstanceId;
    const sessionMemo = await AiService.fetchChatSession({
      sceneType: 1,
      sceneResourceId: currrentStepInstanceId,
    });
    await handleShowBlueking('analyzeFileTaskError', params, sessionMemo?.aiSessionId);
    if (!sessionMemo?.aiSessionId) {
      const currentSession = aiRef.value.getChatHelper().session.current.value;
      AiService.updateChatSession({
        sceneType: 1,
        sceneResourceId: currrentStepInstanceId,
        aiSessionId: currentSession.sessionCode,
        sessionName: currentSession.sessionName,
      });
    }
  });

</script>
<style lang="postcss">
.ai-blueking-panel{
  .shortcut-btns{
    display: none;
  }
}
</style>


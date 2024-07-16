<template>
  <div class="jb-ai">
    <img
      v-show="!isBluekingShow"
      ref="handleRef"
      class="hander-btn"
      :class="{ active: !isBluekingShow && isHandleShow }"
      src="/static/images/blueking.png"
      style="width: 64px; height: 64px"
      @click="handleShow">
    <ai-blueking
      v-show="isBluekingShow"
      ref="aiRef"
      :loading="isLoading"
      :messages="messages"
      @clear="handleClear"
      @close="handleClose"
      @send="handleSend" />
  </div>
</template>
<script setup>
  import { ref, shallowRef } from 'vue';

  import AiService from '@service/ai';

  import eventBus from '@utils/event-bus';

  import AiBlueking from '@blueking/ai-blueking/dist/vue2/index.es.min.js';

  import useCloseAnimate from './use-close-animate';

  import '@blueking/ai-blueking/dist/vue2/style.css';

  let promptParamsMemo = {};

  const messages = shallowRef([]);

  const handleRef = ref();
  const aiRef = ref();
  const isLoading = ref(false);
  const promptType = ref('');
  const isBluekingShow = ref(false);
  const isHandleShow = ref(true);

  const runCloseAnimate = useCloseAnimate(aiRef, handleRef, () => {
    isHandleShow.value = true;
  });

  const fetchLatestChatHistoryList = () => {
    isLoading.value = false;
    AiService.fetchLatestChatHistoryList()
      .then((result) => {
        messages.value = result;
      })
      .finally(() => {
        isLoading.value = false;
      });
  };
  fetchLatestChatHistoryList();

  const handleShow = () => {
    promptType.value = 'generaChat';
    isBluekingShow.value = true;
    isHandleShow.value = false;
  };


  const handleCheckScript = () => {
    isLoading.value = true;
    AiService.fetchCheckScript(promptParamsMemo)
      .then(() => fetchLatestChatHistoryList());
  };

  const handlerAnalyzeError = () => {
    isLoading.value = true;
    AiService.fetchAnalyzeError(promptParamsMemo)
      .then(() => fetchLatestChatHistoryList());
  };

  const handleGeneraChat = (params) => {
    isLoading.value = true;
    AiService.fetchGeneraChat(params)
      .then(() => {
        fetchLatestChatHistoryList();
      });
  };

  const handleSend = (content) => {
    if (promptType.value === 'checkScript') {
      handleCheckScript({
        ...promptParamsMemo,
        content,
      });
    } if (promptType.value === 'analyzeError') {
      handlerAnalyzeError({
        ...promptParamsMemo,
        content,
      });
    }
    handleGeneraChat({
      content,
    });
  };

  const handleClear = () => {
    AiService.fetchGeneraChat();
    messages.value = [];
  };

  const handleClose = () => {
    isBluekingShow.value = false;
    runCloseAnimate();
  };


  eventBus.$on('ai:generaChat', () => {
    handleShow();
  });

  eventBus.$on('ai:checkScript', (params) => {
    handleShow();
    promptParamsMemo = params;
    promptType.value = 'checkScript';
    handleCheckScript(params);
  });

  eventBus.$on('ai:analyzeError', (params) => {
    handleShow();
    console.log(params);
    promptParamsMemo = params;
    promptType.value = 'analyzeError';
    handlerAnalyzeError(params);
  });
</script>
<style lang="postcss">
  .jb-ai {
    .hander-btn{
      position: fixed;
      right: 0;
      bottom: 20px;
      z-index: 999999999;
      cursor: pointer;
      transform: translateX(100%);
      transition: all .1s ease-in-out;

      &.active{
        transform: translateX(29px);
      }

      &:hover{
        transform: translateX(20px);
      }
    }
  }
</style>

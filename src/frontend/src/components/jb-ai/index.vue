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
      :messages="messageList"
      :scroll-loading="isLoadingMoore"
      :scroll-loading-end="isScrollLoadingEnd"
      @clear="handleClear"
      @close="handleClose"
      @scroll-load="handleLoadingMore"
      @send="handleGeneraChat"
      @stop="handleStop" />
  </div>
</template>
<script setup>
  import dayjs from 'dayjs';
  import { nextTick, ref } from 'vue';

  import AiService from '@service/ai';

  import eventBus from '@utils/event-bus';

  import AiBlueking, { MessageStatus } from '@blueking/ai-blueking/vue2';

  import useCloseAnimate from './use-close-animate';
  import useStreamContent from './use-stream-content';

  import '@blueking/ai-blueking/dist/vue2/style.css';

  console.log('MessageStatus = ', MessageStatus);

  let currentRecordId = 0;

  const handleRef = ref();
  const aiRef = ref();
  const promptType = ref('');
  const isBluekingShow = ref(false);
  const isHandleShow = ref(true);
  const isLoadingMoore = ref(false);
  const isScrollLoadingEnd = ref(false);

  const {
    messageList,
    loading: isLoading,
    fetchContent,
  } = useStreamContent();
  const runCloseAnimate = useCloseAnimate(aiRef, handleRef, () => {
    isHandleShow.value = true;
  });


  const handleShow = () => {
    promptType.value = 'generaChat';
    isBluekingShow.value = true;
    isHandleShow.value = false;
    nextTick(() => {
      aiRef.value.$el.querySelector('.ai-messages').scrollTop = Number.MAX_SAFE_INTEGER;
    });
  };

  const handleCheckScript = (params) => {
    isLoading.value = true;
    AiService.fetchCheckScript(params)
      .then((data) => {
        messageList.value.push({
          role: 'user',
          content: data.userInput.content,
          status: '',
          time: data.userInput.time,
        });
        currentRecordId = data.id;
        fetchContent(currentRecordId);
      });
  };

  const handlerAnalyzeError = (params) => {
    isLoading.value = true;
    AiService.fetchAnalyzeError(params)
      .then((data) => {
        messageList.value.push({
          role: 'user',
          content: data.userInput.content,
          status: '',
          time: data.userInput.time,
        });
        currentRecordId = data.id;
        fetchContent(currentRecordId);
      });
  };

  const handleGeneraChat = (content) => {
    isLoading.value = true;
    messageList.value.push({
      role: 'user',
      content,
      status: '',
      time: dayjs().format('YYYY-MM-DD HH:mm:ss'),
    });
    AiService.fetchGeneraChat({
      content,
    })
      .then((data) => {
        currentRecordId = data.id;
        fetchContent(currentRecordId);
      });
  };

  const handleClear = () => {
    AiService.deleteChatHistory()
      .then(() => {
        messageList.value = [];
      });
  };

  const handleStop = () => {
    AiService.terminateChat({
      recordId: currentRecordId,
    });
  };

  const handleClose = () => {
    isBluekingShow.value = false;
    handleStop();
    runCloseAnimate();
  };

  const handleLoadingMore = () => {
    isLoadingMoore.value = true;
    console.log('asdasdasd');
  };


  eventBus.$on('ai:generaChat', () => {
    handleShow();
  });

  eventBus.$on('ai:checkScript', (params) => {
    handleShow();
    handleCheckScript(params);
  });

  eventBus.$on('ai:analyzeError', (params) => {
    handleShow();
    handlerAnalyzeError(params);
  });
</script>
<style lang="postcss">
  .jb-ai {
    position: relative;
    z-index: 999999;

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

    .ai-modal-header{
      background: none !important;
      background-image: linear-gradient(-83deg, #162737 0%, #375B97 100%) !important;
    }

    .ai-content{
      background: #E5ECF8 !important;

      .message-content.user{
        background: #F5F8FF !important;

        &::before{
          background: #F5F8FF !important;
        }
      }
    }
  }
</style>

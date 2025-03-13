<template>
  <div
    class="jb-ai">
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
      :alert="$t('注意！为了回答更准确，大模型可能将你提供的信息作为训练材料，如果内容带有敏感信息，请酌情使用！')"
      :loading="isChatHistoryLoading || isContentLoading"
      :messages="messageList"
      :name="$t('小鲸')"
      :placeholder="$t('在这里输入你的问题，试试我是否可以帮助到你...')"
      :scroll-loading="isLoadingMore"
      :scroll-loading-end="!hasMore"
      :start-position="startPosition"
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

  import AiBlueking from '@blueking/ai-blueking/vue2';

  import useChatHistory from './use-chat-history';
  import useCloseAnimate from './use-close-animate';
  import useStreamContent from './use-stream-content';

  import '@blueking/ai-blueking/dist/vue2/style.css';

  let currentRecordId = 0;

  const handleRef = ref();
  const aiRef = ref();
  const promptType = ref('');
  const isBluekingShow = ref(false);
  const isHandleShow = ref(true);

  const startPosition = {
    top: Math.max(window.innerHeight - 600, 0),
    bottom: 0,
    left: window.innerWidth - window.innerWidth * 0.75,
    right: 0,
  };

  const {
    messageList,
    loading: isChatHistoryLoading,
    loadingMore: isLoadingMore,
    hasMore: hasMore,
    loadMore: handleLoadingMore,
  } = useChatHistory();

  const {
    loading: isContentLoading,
    fetchContent,
  } = useStreamContent(messageList);

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
    isContentLoading.value = true;
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
    isContentLoading.value = true;
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
    isContentLoading.value = true;
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


  const handleStop = () => {
    AiService.terminateChat({
      recordId: currentRecordId,
    });
  };

  const handleClear = () => {
    if (isContentLoading.value) {
      handleStop();
    }
    AiService.deleteChatHistory()
      .then(() => {
        messageList.value = [];
      });
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
        transform: translateX(39px);
      }

      &:hover{
        transform: translateX(20px);
      }
    }
  }

  .ai-modal{
    box-shadow: 0 0 30px 8px #0000001a !important;

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

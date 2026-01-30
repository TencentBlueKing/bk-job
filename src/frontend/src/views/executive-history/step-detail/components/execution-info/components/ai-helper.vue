<template>
  <div
    v-if="!isConfigLoading && isAiEnable"
    class="execute-history-ai-helper extend-item">
    <span
      ref="referRef"
      style="cursor: pointer;"
      @click="handleClick">
      <img
        :src="aiImage"
        style="width: 16px">
    </span>
    <div
      ref="popoverContentRef"
      style="position: relative; width: 300px; padding: 12px 14px 12px 22px; font-size: 14px; line-height: 22px; color: #63656E;">
      <div>{{ $t('history.日志文本过长，超出 AI 解析范围，请框选部分日志，再次出发点小鲸分析。') }}</div>
      <div style="margin-top: 16px;text-align: right;">
        <bk-button
          size="small"
          theme="primary"
          @click="handleHide">
          {{ $t('history.确定') }}
        </bk-button>
      </div>
      <icon
        style="position: absolute; top: 15px; left: 1px; font-size: 16px; color: #EA3636;"
        type="info" />
    </div>
    <div
      v-if="isShowGuide"
      class="guide-tips">
      <div class="wrapper">
        <img
          :src="bluekingImage"
          style="width: 36px">
        <span class="ml-8">{{ $t('history.报错日志看不懂？来提问小鲸吧～') }}</span>
      </div>
    </div>
  </div>
</template>
<script setup>
  import Tippy from 'bk-magic-vue/lib/utils/tippy';
  import { onBeforeUnmount, onMounted, ref } from 'vue';

  import AiService from '@service/ai';

  const props = defineProps({
    getLog: {
      type: Function,
      default: () => {},
    },
    taskInstanceId: {
      type: Number,
    },
  });

  const aiImage = window.__loadAssetsUrl__('/static/images/ai.png');
  const bluekingImage = window.__loadAssetsUrl__('/static/images/blueking.png');
  const referRef = ref();
  const popoverContentRef = ref();

  const isShowGuide = ref(false);
  const isConfigLoading = ref(true);
  const analyzeErrorLogMaxLength = ref(-1);
  const isAiEnable = ref(false);

  let instance;

  const initPop = () => {
    instance = Tippy(referRef.value, {
      theme: 'light',
      interactive: true,
      placement: 'bottom-start',
      content: popoverContentRef.value,
      trigger: 'manual',
      arrow: true,
    });
  };

  AiService.fetchConfig()
    .then((data) => {
      analyzeErrorLogMaxLength.value = data.analyzeErrorLogMaxLength;
      isAiEnable.value = data.enabled;
      if (data.enabled) {
        setTimeout(() => {
          initPop();
        });
      }
    })
    .finally(() => {
      isConfigLoading.value = false;
    });

  const handleHide = () => {
    instance.hide();
  };

  const handleClick = () => {
    isShowGuide.value = false;
    props.getLog(analyzeErrorLogMaxLength.value)
      .catch(() => {
        instance.show();
      });
  };

  onMounted(() => {
    setTimeout(() => {
      isShowGuide.value = true;
    }, 1000);
  });

  onBeforeUnmount(() => {
    if (instance) {
      instance.hide();
      instance.destroy();
    }
  });
</script>
<style lang="postcss">
  @keyframes ai-guide-tips {
    0% {
      opacity: 0%;
      transform: translateY(-30px) scale(.8);
    }

    30%{
      opacity: 100%;
    }

    45%{
      transform: translateY(0) scale(1);
    }

    75% {
      transform: translateY(-12px) scale(.92);
    }

    100%{
      opacity: 100%;
      transform: translateY(0);
    }
  }

  .execute-history-ai-helper {
    position: relative;

    .guide-tips{
      position: absolute;
      top: -44px;
      right: -40px;
      padding: 0 6px;
      font-size: 12px;
      color: #63656E;
      white-space: nowrap;
      background: #fff;
      border-radius: 18px;
      opacity: 0%;
      box-shadow: 0 2px 6px 0 #0000001f;
      animation: 1.5s ai-guide-tips ease-in-out forwards;

      .wrapper{
        position: relative;
        z-index: 1;
        display: flex;
        background: inherit;
        border-radius: inherit;
        align-items: center;
        justify-content: center;
      }

      &::after{
        position: absolute;
        right: 52px;
        bottom: 3px;
        border: 0 solid transparent;
        border-bottom: 10px solid #fff;
        border-right-width: 16px;
        border-left-width: 16px;
        content: '';
        transform: rotateZ(45deg);
        box-shadow: 0 2px 6px 0 #0000001f;
      }
    }
  }
</style>

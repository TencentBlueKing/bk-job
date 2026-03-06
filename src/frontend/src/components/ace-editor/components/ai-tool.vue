<template>
  <div
    class="ace-editor-ai-tool"
    :class="{'is-active': isActive}"
    @click="handleShow"
    @mouseenter="handleShow">
    <span ref="referRef">
      <img
        :src="aiImage"
        style="width: 16px">
    </span>
    <div
      ref="popoverContentRef"
      class="ace-editor-ai-tool-menu">
      <div
        class="item"
        @click="handleCheckScript">
        {{ $t('脚本检查') }}
      </div>
      <div
        class="item"
        @click="handleChat">
        {{ $t('其它') }}
      </div>
    </div>
    <div
      v-if="showAi"
      class="tool-tips">
      <div class="wrapper">
        <img
          :src="bluekingImage"
          style="width: 36px">
        <span class="ml-8">{{ $t('AI 脚本检查已上线！') }}</span>
      </div>
    </div>
  </div>
</template>
<script setup>
  import Tippy from 'bk-magic-vue/lib/utils/tippy';
  import { onBeforeUnmount, onMounted, ref } from 'vue';

  import eventBus from '@/utils/event-bus';

  const emits = defineEmits(['checkScript']);

  const editorAiHelperCacheKey = 'editor_ai_helper';
  const aiImage = window.__loadAssetsUrl__('/static/images/ai.png');
  const bluekingImage = window.__loadAssetsUrl__('/static/images/blueking.png');

  const referRef = ref();
  const popoverContentRef = ref();

  const isActive = ref(false);
  const showAi = ref(!localStorage.getItem(editorAiHelperCacheKey));

  let instance;
  const handleShow = () => {
    instance && instance.show();
  };

  const handleCheckScript = () => {
    emits('checkScript');
    localStorage.setItem(editorAiHelperCacheKey, true);
    showAi.value = false;
  };

  const handleChat = () => {
    eventBus.$emit('ai:generaChat');
    localStorage.setItem(editorAiHelperCacheKey, true);
    showAi.value = false;
  };

  onMounted(() => {
    instance = Tippy(referRef.value, {
      theme: 'dark ace-editor-ai-tool',
      interactive: true,
      placement: 'bottom-start',
      content: popoverContentRef.value,
      trigger: 'manual',
      arrow: false,
      onShown() {
        isActive.value = true;
      },
      onHidden() {
        isActive.value = false;
      },
    });

    onBeforeUnmount(() => {
      instance.hide();
      instance.destroy();
    });
  });
</script>
<style lang="postcss">
@keyframes ai-tool-tips {
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

.ace-editor-ai-tool {
  position: relative;
  display: flex;
  width: 24px;
  height: 24px;
  border-radius: 2px;
  align-items: center;
  justify-content: center;

  &.is-active,
  &:hover{
    background: #4D4D4D;
  }

  .tool-tips{
    position: absolute;
    top: -44px;
    left: -145px;
    padding: 0 6px;
    font-size: 12px;
    color: #63656E;
    white-space: nowrap;
    background: #fff;
    border-radius: 18px;
    opacity: 0%;
    box-shadow: 0 2px 6px 0 #0000001f;
    animation: 1.5s ai-tool-tips .5s ease-in-out forwards;
    transform-origin: right bottom;

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
      bottom: 3px;
      left: 133px;
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

.ace-editor-ai-tool-theme{
  width: 80px;
  padding: 8px 0;
  border: 1px solid #3D3D3D;

  .item{
    display: flex;
    height: 32px;
    padding: 0 10px;
    font-size: 12px;
    color: #DCDEE5;
    cursor: pointer;
    align-items: center;

    &:hover{
      background: #474747;
    }
  }
}
</style>

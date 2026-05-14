<template>
  <div class="script-content-alert">
    <div
      class="script-content-alert-header"
      @click="isExpanded = !isExpanded">
      <div class="script-content-alert-icon">
        <svg
          fill="currentColor"
          viewBox="0 0 16 16"><path d="M8 1a7 7 0 100 14A7 7 0 008 1zm-.5 3h1v1h-1V4zm0 2.5h1v5h-1v-5z" /></svg>
      </div>
      <i18n
        class="script-content-alert-title"
        path="Agent 执行环境提示标题"
        tag="div">
        <strong slot="strongText">{{ $t('Agent 执行环境可能与 SSH 登录不一致') }}</strong>
        <code slot="code1">ulimit</code>
        <code slot="code2">/etc/security/limits.conf</code>
        <code slot="code3">source</code>
      </i18n>
      <div class="script-content-alert-action">
        <span>{{ isExpanded ? $t('收起') : $t('了解详情') }}</span>
        <icon
          class="arrow"
          :class="{ expanded: isExpanded }"
          type="arrow-full-right" />
      </div>
    </div>
    <div
      v-show="isExpanded"
      class="script-content-alert-detail">
      <div class="content">
        <div class="reason-title">
          {{ $t('原因说明：') }}
        </div>
        <i18n
          path="Agent 执行环境原因说明"
          tag="div">
          <code slot="code1">/etc/profile</code>
          <code slot="code2">~/.bashrc</code>
        </i18n>
      </div>
      <i18n
        class="content"
        path="Agent 执行环境配置说明"
        tag="div">
        <code slot="code1">ulimit</code>
      </i18n>
      <div class="content">
        <div class="reason-title">
          {{ $t('推荐写法：') }}
        </div>
        {{ $t('在脚本开头显式加载所需配置，例如：') }}
      </div>
      <div class="code-block">
        #!/bin/bash<br>
        source /etc/profile<br>
        source ~/.bashrc<br>
        <span class="comment">{{ $t('# 如需特定的 ulimit 设置，也请在脚本中显式声明') }}</span><br>
        ulimit -n 65535<br>
        <br>
        <span class="comment">{{ $t('# 您的业务脚本内容...') }}</span>
      </div>
    </div>
  </div>
</template>
<script setup>
  import { ref } from 'vue';

  const isExpanded = ref(false);
</script>

<style lang="scss" scoped>
  .script-content-alert {
    background: #fff4e2;
    border: 1px solid #ffdfac;
    border-radius: 2px;
    margin-bottom: 10px;
    color: #63656e;
    line-height: 20px;

    .script-content-alert-header {
      min-height: 36px;
      display: flex;
      align-items: flex-start;
      padding: 8px 12px;
      cursor: pointer;
      user-select: none;
    }

    .script-content-alert-icon {
      width: 16px;
      height: 16px;
      margin: 2px 8px 0 0;
      color: #ff9c01;
      flex-shrink: 0;

      svg {
        width: 16px;
        height: 16px;
        display: block;
      }
    }

    .script-content-alert-title {
      flex: 1;
      color: #63656e;

      :deep(strong) {
        color: #313238;
        font-weight: 600;
      }

      :deep(code) {
        padding: 1px 5px;
        border-radius: 2px;
        background: rgba(255, 156, 1, 0.14);
        color: #b56a00;
        font-family: "SFMono-Regular", Consolas, monospace;
      }
    }

    .script-content-alert-action {
      color: #3a84ff;
      margin-left: 16px;
      white-space: nowrap;
      display: flex;
      align-items: center;
      gap: 4px;

      &:hover {
        color: #1768ef;
      }

      .arrow {
        transition: transform 0.2s;
        &.expanded {
          transform: rotate(90deg);
        }
      }
    }

    .script-content-alert-detail {
      padding: 2px 12px 12px 36px;

      .content {
        margin-bottom: 8px;
        color: #63656e;
      }

      .reason-title {
        color: #313238;
        font-weight: 600;
      }

      :deep(code) {
        padding: 1px 5px;
        border-radius: 2px;
        background: rgba(255, 156, 1, 0.14);
        color: #b56a00;
        font-family: "SFMono-Regular", Consolas, monospace;
      }

      .code-block {
        background: #f5f7fa;
        border: 1px solid #dcdee5;
        border-radius: 2px;
        padding: 8px 12px;
        margin-top: 8px;
        color: #313238;
        line-height: 20px;
        font-family: "SFMono-Regular", Consolas, monospace;
      }

      .comment {
        color: #699d00;
      }
    }
  }
</style>

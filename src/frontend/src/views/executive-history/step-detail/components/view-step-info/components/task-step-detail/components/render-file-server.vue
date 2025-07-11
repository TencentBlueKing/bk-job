<!--
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
-->

<template>
  <div class="execute-history-file-server">
    <div
      class="server-agent-text"
      @click="handlerView"
      v-html="data.serverDesc" />
    <jb-dialog
      v-model="isShowDetail"
      class="execute-distory-step-view-server-detail-dialog"
      :ok-text="$t('template.关闭')"
      :width="1020">
      <template #header>
        <div class="title">
          <span>{{ $t('template.服务器文件-服务器列表') }}</span>
          <i
            class="dialog-close-btn bk-icon icon-close"
            @click="handleClose" />
        </div>
      </template>
      <div class="content-wraper">
        <scroll-faker>
          <ip-selector
            :complete-container-list="containerDetail(executeObjectsInfo.containerList)"
            :complete-host-list="hostsDetails(executeObjectsInfo.hostList)"
            readonly
            show-view />
        </scroll-faker>
      </div>
      <template #footer>
        <bk-button @click="handleClose">
          {{ $t('关闭') }}
        </bk-button>
      </template>
    </jb-dialog>
  </div>
</template>
<script setup>
  import {
    ref,
    shallowRef,
  } from 'vue';

  import ScrollFaker from '@components/scroll-faker';

  import {
    containerDetail,
    hostsDetails,
  } from '@blueking/ip-selector/dist/adapter';

  const props = defineProps({
    data: {
      type: Object,
      required: true,
    },
  });

  const isShowDetail = ref(false);
  const executeObjectsInfo = shallowRef({});

  const handlerView = () => {
    executeObjectsInfo.value = props.data.host.executeObjectsInfo;
    isShowDetail.value = true;
  };
  const handleClose = () =>  {
    isShowDetail.value = false;
  };
</script>
<style lang='postcss'>
  .execute-history-file-server {
    min-height: 30px;
    padding: 5px;
    margin-left: -10px;
    cursor: pointer;

    &:hover {
      background: #f0f1f5;
    }

    .server-agent-text {
      white-space: pre;

      .sep-location {
        &::before {
          content: "";
        }
      }
    }
  }

  .execute-distory-step-view-server-detail-dialog {
    .ip-selector-view-host{
      margin-top: 0 !important;
    }

    .bk-dialog-tool {
      display: none;
    }

    .bk-dialog-header,
    .bk-dialog-footer {
      position: relative;
      z-index: 99999;
      background: #fff;
    }

    .bk-dialog-header {
      padding: 0;
    }

    .bk-dialog-wrapper .bk-dialog-header .bk-dialog-header-inner {
      font-size: 20px;
      color: #000;
      text-align: left;
    }

    .bk-dialog-wrapper .bk-dialog-body {
      padding: 0;

      .server-panel {
        height: 100%;

        &.show-detail {
          overflow: hidden;
        }

        .host-detail.show {
          padding-left: 20%;
        }
      }
    }

    .content-wraper {
      height: 550px;
      margin-top: -1px;
    }

    button[name="cancel"] {
      display: none;
    }

    .title {
      position: relative;
      height: 68px;
      padding-top: 0;
      padding-bottom: 0;
      padding-left: 25px;
      font-size: 20px;
      line-height: 68px;
      color: #000;
      text-align: left;
      border-bottom: 1px solid #dcdee5;
    }

    .dialog-close-btn {
      position: absolute;
      top: 5px;
      right: 5px;
      z-index: 1;
      width: 26px;
      height: 26px;
      font-size: 22px;
      font-weight: 700;
      line-height: 26px;
      color: #979ba5;
      text-align: center;
      cursor: pointer;
      border-radius: 50%;

      &:hover {
        background-color: #f0f1f5;
      }
    }
  }
</style>

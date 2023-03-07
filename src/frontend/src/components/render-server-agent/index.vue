<!--
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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
  <div class="render-server-agent">
    <icon
      v-if="isLoading"
      class="rotate-loading"
      svg
      type="sync-pending" />
    <div
      v-else
      class="agent-text"
      @click="handleShowDetail">
      <div v-if="data.normalNum > 0">
        {{ $t('正常') }}:<span class="success number">{{ data.normalNum }}</span>
      </div>
      <div v-if="data.abnormalNum > 0">
        <span
          v-if="data.normalNum > 0"
          class="splite" />
        {{ $t('异常') }}:<span class="error number">{{ data.abnormalNum }}</span>
      </div>
      <span v-if="isEmpty">--</span>
    </div>
    <lower-component
      :custom="isShowDetail"
      level="custom">
      <jb-dialog
        v-model="isShowDetail"
        class="render-server-detail-dialog"
        :ok-text="$t('关闭')"
        :width="1020">
        <template #header>
          <div class="variable-title">
            <span>{{ title }}</span>
            <i
              class="global-variable-dialog-close bk-icon icon-close"
              @click="handleClose" />
          </div>
        </template>
        <div class="content-wraper">
          <scroll-faker>
            <!-- <server-panel
                            detail-mode="dialog"
                            :host-node-info="hostNodeInfo" /> -->
            <ip-selector
              readonly
              show-view
              :value="hostNodeInfo" />
          </scroll-faker>
        </div>
      </jb-dialog>
    </lower-component>
  </div>
</template>
<script>
  import HostManageService from '@service/host-manage';

  import TaskHostNodeModel from '@model/task-host-node';
  // import ServerPanel from '@components/choose-ip/server-panel';

  export default {
    name: '',
    components: {
      // ServerPanel,
    },
    props: {
      title: {
        type: String,
        required: true,
      },
      hostNodeInfo: {
        type: Object,
        required: true,
      },
      separator: {
        type: String,
        default: '，',
      },
    },
    data() {
      return {
        isLoading: false,
        isShowDetail: false,
        data: {
          normalNum: 0,
          abnormalNum: 0,
        },
      };
    },
    computed: {
      isEmpty() {
        return TaskHostNodeModel.isHostNodeInfoEmpty(this.hostNodeInfo);
      },
    },
    watch: {
      hostNodeInfo() {
        this.fetchData();
      },
    },
    created() {
      this.fetchData();
    },
    methods: {
      fetchData() {
        if (this.isEmpty || this.isLoading) {
          this.isLoading = false;
          return;
        }
        this.isLoading = true;
        const { dynamicGroupList, hostList, nodeList } = this.hostNodeInfo;
        HostManageService.fetchHostStatistics({
          nodeList,
          dynamicGroupList,
          hostList,
        }).then((data) => {
          this.data = data;
        })
          .finally(() => {
            this.isLoading = false;
          });
      },
      handleShowDetail() {
        this.isShowDetail = true;
      },
      handleClose() {
        this.isShowDetail = false;
      },
    },
  };
</script>
<style lang="postcss">
  .render-server-agent {
    .agent-text {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      cursor: pointer;

      .strong {
        color: #3a84ff;
      }

      .error {
        color: #ea3636;
      }

      .success {
        color: #3fc06d;
      }

      .number {
        padding: 0 4px;
        font-weight: bold;
      }

      .splite {
        padding-left: 16px;
      }
    }
  }

  .render-server-detail-dialog {
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
      height: 450px;
      max-height: 450px;
      min-height: 450px;
      margin-top: -1px;
    }

    button[name="cancel"] {
      display: none;
    }

    .variable-title {
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

    .global-variable-dialog-close {
      position: absolute;
      top: 0;
      right: 0;
      font-size: 32px;
      color: #c4c6cc;
      cursor: pointer;
      transition: all 0.15s;

      &:hover {
        transform: rotateZ(90deg);
      }
    }
  }
</style>

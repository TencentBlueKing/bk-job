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
  <div
    v-if="allHostList.length > 0"
    class="server-panel-action-extend"
    @click.stop=""
    @mouseleave="handleHide">
    <div v-bk-tooltips="$t('复制所有')">
      <icon type="more" />
    </div>
    <div
      ref="popoverContent"
      class="server-action-extend-content"
      @click="handleWraperClick"
      @mouseleave="handleClose"
      @mouseover="handleShow">
      <div
        class="action-item"
        @click="() => handleCopyIPv4()">
        IPV4
      </div>
      <div
        class="action-item"
        @click="() => handleCopyIPv4(true)">
        {{ $t('管控区域 ID:IPv4') }}
      </div>
      <div
        class="action-item"
        @click="() => handleCopyIPv6()">
        IPV6
      </div>
      <div
        class="action-item"
        @click="() => handleCopyIPv6(true)">
        {{ $t('管控区域 ID:IPv6') }}
      </div>
      <div
        class="action-item"
        @click="handleCopyHostId">
        {{ $t('主机 ID') }}
      </div>
    </div>
  </div>
</template>
<script>

  import _ from 'lodash';

  import HomeService from '@service/home';

  import {
    execCopy,
  } from '@utils/assist';

  import I18n from '@/i18n';

  const instanceMap = {};

  export default {
    name: 'ChooseIpExtendAction',
    props: {
      list: {
        type: Array,
        default: () => [],
      },
      agentStatus: {
        type: Number,
      },
    },
    data() {
      return {
        allHostList: [],
      };
    },
    watch: {
      agentStatus: {
        handler() {
          if (this.agentStatus > -1) {
            this.fetchData();
          }
        },
        immediate: true,
      },
    },
    created() {
      this.id = `action_extend_${Math.random()}_${Math.random()}`;
    },
    beforeDestroy() {
      instanceMap[this.id].hide();
      delete instanceMap[this.id];
    },
    methods: {
      fetchData() {
        HomeService.fetchAgentStatus({
          agentStatus: this.agentStatus,
        }).then((data) => {
          this.allHostList = data.data;
          if (this.allHostList.length > 0) {
            setTimeout(() => {
              this.init();
            });
          }
        });
      },
      /**
       * @desc 弹层面板初始化
       */
      init() {
        instanceMap[this.id] = this.$bkPopover(this.$el, {
          theme: 'server-panel-action-extend-popover',
          interactive: true,
          placement: 'bottom',
          content: this.$refs.popoverContent,
          trigger: 'mouseover',
          arrow: true,
          onShow: () => {
            Object.keys(instanceMap).forEach((key) => {
              if (key !== this.id) {
                instanceMap[key].hide();
              }
            });
          },
        });
      },
      /**
       * @desc 隐藏弹层面板
       */
      handleWraperClick() {
        this.handleClose();
      },
      /**
       * @desc 鼠标操作隐藏弹层面板
       */
      handleHide() {
        this.leaveTimer = setTimeout(() => {
          this.handleClose();
        }, 3000);
      },

      handleCopyIPv4(withNet = false) {
        const allIP = _.filter(this.allHostList.map(host => (withNet ? `${host.cloudArea.id}:${host.ip}` : host.ip)), item => !!item);

        if (allIP.length < 1) {
          this.messageWarn(I18n.t('home.没有可复制的 IPv4'));
          return;
        }
        execCopy(allIP.join('\n'), `${I18n.t('home.复制成功')}（${allIP.length}${I18n.t('home.个IP')}）`);
      },
      handleCopyIPv6(withNet = false) {
        const ipv6HostList = _.filter(this.allHostList, host => !!host.ipv6);
        if (ipv6HostList.length < 1) {
          this.messageWarn(I18n.t('home.没有可复制的 IPv6'));
          return;
        }
        const allIP = _.filter(ipv6HostList.map(host => (withNet ? `${host.cloudArea.id}:${host.ipv6}` : host.ipv6)), item => !!item);

        execCopy(allIP.join('\n'), `${I18n.t('home.复制成功')}（${allIP.length}${I18n.t('home.个IP')}）`);
      },
      /**
       * @desc 复制主机ID
       */
      handleCopyHostId() {
        const allHostId = this.allHostList.map(host => host.hostId);
        if (allHostId.length < 1) {
          this.messageWarn(I18n.t('home.没有可复制的主机ID'));
          return;
        }
        execCopy(allHostId.join('\n'), `${I18n.t('home.复制成功')}（${allHostId.length}${I18n.t('home.个主机ID')}）`);
      },
      handleShow() {
        clearTimeout(this.leaveTimer);
      },
      handleClose() {
        instanceMap[this.id] && instanceMap[this.id].hide();
      },
    },
  };
</script>
<style lang="postcss">
  .server-panel-action-extend {
    position: absolute;
    top: 50%;
    right: 16px;
    z-index: 9;
    display: flex;
    width: 30px;
    height: 30px;
    font-size: 16px;
    font-weight: normal;
    color: #979ba5;
    cursor: pointer;
    border-radius: 50%;
    transform: translateY(-50%);
    user-select: none;
    align-items: center;
    justify-content: center;

    &:hover,
    &.tippy-active {
      z-index: 10;
      color: #3a84ff;
      background: #dcdee5;
    }
  }

  .server-panel-action-extend-popover-theme {
    padding: 0 !important;

    .tippy-arrow {
      display: none;
    }

    .server-action-extend-content {
      font-size: 12px;
      line-height: 32px;
      color: #63656e;
      background: #fff;
      border: 1px solid #f0f1f5;
      box-shadow: 0 2px 1px 0 rgb(185 203 222 / 50%);

      .action-item {
        padding: 0 15px;
        cursor: pointer;


        &:hover {
          color: #3a84ff;
          background: #e5efff;
        }
      }
    }
  }

</style>

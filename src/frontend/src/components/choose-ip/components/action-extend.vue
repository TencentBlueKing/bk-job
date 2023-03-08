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
  <div
    class="server-panel-action-extend"
    @click.stop=""
    @mouseleave="handleHide">
    <icon type="more" />
    <div
      ref="popoverContent"
      class="server-action-extend-content"
      @click="handleWraperClick"
      @mouseleave="handleClose"
      @mouseover="handleShow">
      <template v-if="copyable">
        <div
          class="action-item"
          @click="handleCopyAll">
          {{ $t('复制所有 IP') }}
        </div>
        <div
          class="action-item"
          @click="handleCopyFail">
          {{ $t('复制异常 IP') }}
        </div>
      </template>
      <slot />
    </div>
  </div>
</template>
<script>
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
      invalidList: {
        type: Array,
        default: () => [],
      },
      copyable: {
        type: Boolean,
        default: false,
      },
    },
    created() {
      this.id = `action_extend_${Math.random()}_${Math.random()}`;
    },
    mounted() {
      this.init();
    },
    beforeDestroy() {
      instanceMap[this.id].hide();
      delete instanceMap[this.id];
    },
    methods: {
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
      /**
       * @desc 复制所有主机
       */
      handleCopyAll() {
        if (this.list.length < 1 && this.invalidList.length < 1) {
          this.messageWarn(I18n.t('你还未选择主机'));
          return;
        }
        let allIP = this.list.map(host => host.ip);
        const allInvalidList = this.invalidList.map(host => host.ip);
        allIP = [
          ...allIP, ...allInvalidList,
        ];
        execCopy(allIP.join('\n'), `${I18n.t('复制成功')}（${allIP.length}${I18n.t('个IP')}）`);
      },
      /**
       * @desc 复制异常主机
       */
      handleCopyFail() {
        if (this.list.length < 1 && this.invalidList.length < 1) {
          this.messageWarn(I18n.t('你还未选择主机'));
          return;
        }
        let allFailIp = [];
        this.list.forEach((currentHost) => {
          if (!currentHost.alive) {
            allFailIp.push(currentHost.ip);
          }
        });
        if (allFailIp.length < 1 && this.invalidList.length < 1) {
          this.messageWarn(I18n.t('暂无异常主机'));
          return;
        }
        const allInvalidList = this.invalidList.map(host => host.ip);
        allFailIp = [
          ...allFailIp, ...allInvalidList,
        ];
        execCopy(allFailIp.join('\n'), `${I18n.t('复制成功')}（${allFailIp.length}${I18n.t('个异常IP')}）`);
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
  html[lang="en-US"] {
    .server-action-extend-content {
      width: 154px;
    }
  }

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
      width: 93px;
      font-size: 14px;
      line-height: 32px;
      color: #63656e;
      background: #fff;
      border: 1px solid #f0f1f5;
      box-shadow: 0 2px 1px 0 rgb(185 203 222 / 50%);

      .action-item {
        padding-left: 15px;
        cursor: pointer;

        &:hover {
          color: #3a84ff;
          background: #e5efff;
        }
      }
    }
  }

</style>

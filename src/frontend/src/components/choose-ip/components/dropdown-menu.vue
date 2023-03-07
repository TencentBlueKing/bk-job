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
    class="server-panel-dropdown-menu"
    @click.stop=""
    @mouseleave="handleHide">
    <slot />
    <div
      ref="popoverContent"
      class="server-dropdown-menu-content"
      @mouseleave="handleClose"
      @mouseover="handleShow">
      <slot name="menu" />
    </div>
  </div>
</template>
<script>
  const instanceMap = {};

  export default {
    name: 'JbPopoverConfirm',
    created() {
      this.id = `dropdown_menu_${Math.random()}_${Math.random()}`;
    },
    mounted() {
      this.init();
    },
    beforeDestroy() {
      instanceMap[this.id].hide();
      delete instanceMap[this.id];
    },
    methods: {
      init() {
        instanceMap[this.id] = this.$bkPopover(this.$el, {
          theme: 'server-panel-dropdown-menu-popover',
          interactive: true,
          placement: 'bottom-start',
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
      handleHide() {
        this.leaveTimer = setTimeout(() => {
          instanceMap[this.id].hide();
        }, 2000);
      },
      handleShow() {
        clearTimeout(this.leaveTimer);
      },
      handleClose() {
        instanceMap[this.id].hide();
      },
    },
  };
</script>
<style lang="postcss">
  .server-panel-dropdown-menu {
    display: inline-flex;
    align-items: center;
    font-size: 20px;
    cursor: pointer;
  }

  .server-panel-dropdown-menu-popover-theme {
    padding: 0 !important;

    .tippy-arrow {
      display: none;
    }

    .server-dropdown-menu-content {
      width: 93px;
      padding: 10px 0;
      font-size: 14px;
      line-height: 32px;
      color: #63656e;
      text-align: center;
      background: #fff;
      border: 1px solid #f0f1f5;
      box-shadow: 0 1px 1px 0 rgb(185 203 222 / 50%);

      .dropdown-menu-item {
        cursor: pointer;

        &:hover,
        &.active {
          color: #3a84ff;
          background: #e5efff;
        }
      }
    }
  }

</style>

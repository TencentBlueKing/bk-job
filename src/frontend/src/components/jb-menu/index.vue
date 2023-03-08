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
  <div class="jb-menu">
    <slot />
  </div>
</template>
<script>
  export default {
    name: 'JbMenu',
    provide() {
      return {
        jbMenu: this,
      };
    },
    props: {
      defaultActive: String,
      active: String,
      flod: {
        type: Boolean,
        default: false,
      },
    },
    data() {
      return {
        activeIndex: this.defaultActive,
      };
    },
    watch: {
      defaultActive(defaultActive) {
        if (!this.items[defaultActive]) {
          this.activeIndex = null;
        }
      },
      active(active) {
        if (this.items[active]) {
          this.activeIndex = active;
        }
      },
    },
    created() {
      this.items = {};
      this.$on('item-click', this.handleItemClick);
    },
    mounted() {

    },
    methods: {
      addItem(item) {
        this.items[item.index] = item;
      },
      handleItemClick(item) {
        this.activeIndex = item.index;
        this.$emit('select', item.index);
      },
    },
  };
</script>
<style lang='postcss'>
  .jb-menu {
    font-size: 14px;
    white-space: nowrap;
  }

  .jb-menu-item-group {
    .title {
      height: 40px;
      padding-left: 18px;
      font-size: 12px;
      line-height: 40px;
      color: #66748f;
    }

    &:nth-child(n+2) {
      margin-top: 16px;
    }
  }

  .jb-menu-item {
    height: 38px;
    padding-left: 22px;
    line-height: 38px;
    color: #acb9d1;
    cursor: pointer;

    &:hover {
      color: #acb9d1;
      background: #253047;
    }

    &.active {
      color: #fff;
      background: #3a84ff;

      .job-icon {
        color: #fff;
      }
    }

    &.flod {
      width: 100%;
      overflow: hidden;
    }

    .job-icon {
      margin-right: 19px;
      font-size: 16px;
      color: #b0bdd5;
    }
  }
</style>

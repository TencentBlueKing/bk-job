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
  <span
    ref="source"
    style="display: none;">
    <slot />
  </span>
</template>
<script>
  import _ from 'lodash';

  export default {
    name: 'ElementTeleport',
    props: {
      target: {
        type: String,
        default: '#siteHeaderStatusBar',
      },
    },
    created() {
      this.childNodeSet = new Set();
      // eslint-disable-next-line no-underscore-dangle
      this.move = _.debounce(this._move, 30);
    },
    updated() {
      this.move();
    },
    mounted() {
      this.move();
    },
    beforeDestroy() {
      this.remove();
      this.childNodeSet.clear();
    },
    methods: {
      /**
       * @desc 组件销毁时同步删除 slot
       */
      remove() {
        const $targetParent = document.querySelector(this.target);
        if (!$targetParent) {
          return;
        }

        this.childNodeSet.forEach((item) => {
          if ($targetParent.contains(item)) {
            if (this.$refs.source) {
              this.$refs.source.appendChild(item);
            } else {
              $targetParent.removeChild(item);
            }

            this.childNodeSet.delete(item);
          }
        });
      },
      /**
       * @desc 移动 slot 到指定的 target
       */
      _move() {
        this.remove();
        if (!this.$refs.source) {
          return;
        }
        const $targetParent = document.querySelector(this.target);
        if (!$targetParent) {
          console.error(`element-teleport: target 指定的 DOM 元素 ${this.target} 不存则`);
          return;
        }
        this.$refs.source.childNodes.forEach((childNode) => {
          this.childNodeSet.add(childNode);
          $targetParent.appendChild(childNode);
        });
      },
    },
  };
</script>

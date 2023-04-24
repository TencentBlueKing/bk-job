/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 *
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
*/

const STRATEGY_MAP = {
  'async'(callback) {
    setTimeout(() => callback(), 300);
  },
  'viewport'(callback) {
    const windowHeight = window.innerHeight;
    const check = () => {
      const { top } = this.$refs.lowerComponent.getBoundingClientRect();
      if (windowHeight + 30 >= top) {
        callback();
        clearTimeout(this.timer);
      }
      this.timer = setTimeout(() => {
        check();
      }, 100);
    };
    check();
    this.$once('hook:beforeDestroy', () => {
      clearTimeout(this.timer);
    });
  },
};
export default {
  name: 'lower-component',
  props: {
    level: {
      type: String,
      default: 'async',
    },
    custom: {
      type: [
        Function, Boolean,
      ],
      default: () => Promise.resolve(),
    },
  },
  data() {
    return {
      isRender: false,
    };
  },
  mounted() {
    if (this.level === 'custom') {
      if (typeof this.custom === 'function') {
        const customPromise = this.custom();
        if (process.env.NODE_ENV !== 'production') {
          if (typeof customPromise.then !== 'function') {
            throw new Error('当custom是函数类型时，custom必须返回一Promise');
          }
        }
        customPromise.then(() => {
          this.updateLevel();
        });
        return;
      }
      const unwatch = this.$watch(() => this.custom, (newVal) => {
        if (newVal) {
          this.updateLevel();
          this.$nextTick(() => {
            unwatch();
          });
        }
      }, {
        immediate: true,
      });
      return;
    }
    if (process.env.NODE_ENV !== 'production') {
      if (!Object.prototype.hasOwnProperty.call(STRATEGY_MAP, this.level)) {
        throw new Error('不支持的升级策略');
      }
    }
    STRATEGY_MAP[this.level].call(this, this.updateLevel);
  },
  methods: {
    updateLevel() {
      this.isRender = true;
    },
  },
  render(h) {
    if (this.isRender) {
      if (this.$slots.default) {
        return this.$slots.default[0];
      }
      return this._e(); // eslint-disable-line no-underscore-dangle
    }
    if (this.level === 'viewport') {
      return h('div', {
        ref: 'lowerComponent',
      });
    }
    return this._e(); // eslint-disable-line no-underscore-dangle
  },
};

/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

const postcssImportWebpackResolver = require('postcss-import-webpack-resolver');
const webpack = require('./webpack.config.js');

const webpackConfig = webpack({
  development: false,
});

module.exports = {
  plugins: {
    'postcss-import': {
      resolve: postcssImportWebpackResolver({
        alias: webpackConfig.resolve.alias,
        modules: ['src', 'node_modules'],
      }),
    },
    'postcss-mixins': {
    },
    // 用于在 URL ( )上重新定位、内嵌或复制。
    'postcss-url': {
      url: 'rebase',
    },
    // cssnext 已经不再维护，推荐使用 postcss-preset-env
    'postcss-preset-env': {
      stage: 0,
      autoprefixer: {
        grid: true,
      },
    },
    // 这个插件可以在写 nested 样式时省略开头的 &
    'postcss-nested': {},

    // 将 @at-root 里的规则放入到根节点
    'postcss-atroot': {},
    // 提供 @extend 语法
    'postcss-extend-rule': {},

    // 变量相关
    'postcss-advanced-variables': {
      // variables 属性内的变量为全局变量
    },
    // 类似于 stylus，直接引用属性而不需要变量定义
    'postcss-property-lookup': {},
  },
};

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

const globals = require('globals');
const js = require('@eslint/js');
const vuePlugin = require('eslint-plugin-vue');
const simpleImportSort = require('eslint-plugin-simple-import-sort');

module.exports = [
  // 全局忽略配置
  {
    ignores: [
      'mock/**',
      'dist/**',
      'static/**',
      'webpack_cache/**',
      'css/**',
      'node_modules/**',
      'lib/**',
      '**/iconcool.js',
      '**/bk-icon/**',
      'index.html',
    ],
  },
  // eslint:recommended 规则
  js.configs.recommended,
  ...vuePlugin.configs['flat/vue2-recommended'],
  // 全局变量配置
  {
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.node,
        NODE_ENV: 'readonly',
        LOCAL_DEV_URL: 'readonly',
        LOCAL_DEV_PORT: 'readonly',
        AJAX_URL_PREFIX: 'readonly',
        AJAX_MOCK_PARAM: 'readonly',
        USER_INFO_URL: 'readonly',
      },
      ecmaVersion: 'latest',
      sourceType: 'module',
      // parserOptions: {
      //   ecmaFeatures: {
      //     jsx: true,
      //   },
      // },
    },
    linterOptions: {
      reportUnusedDisableDirectives: 'off', // 关闭报告未使用的 disable 指令
    },
  },
  // 基础配置
  {
    plugins: {
      'simple-import-sort': simpleImportSort,
    },
    rules: {
      // 禁用 ESLint v10 新增的规则
      'no-useless-assignment': 'off',
      'simple-import-sort/imports': ['error', {
        groups: [
          ['^[a-zA-Z]'],
          ['^@lib'],
          ['^@router'],
          ['^@service'],
          ['^@model'],
          ['^@utils'],
          ['^@views'],
          ['^@components'],
          ['^@\\w'],
          ['^@/'],
          ['^\\.\\.'],
          ['^\\.'],
        ],
      }],
      'no-param-reassign': 'off',
      'max-len': 'off',
      'no-unused-vars': 'error',
      'no-underscore-dangle': ['error', {
        allow: ['__loadAssetsUrl__'],
      }],
      'vue/multi-word-component-names': 'off',
      // 禁用 Vue 3 专属规则
      'vue/no-v-model-argument': 'off',
    },
  },
  // .vue 文件的特殊规则
  {
    files: ['**/*.vue'],
    rules: {
      indent: 'off',
      'import/first': 'off',
      'vue/html-closing-bracket-newline': ['error', {
        singleline: 'never',
        multiline: 'never',
      }],
      'vue/attributes-order': ['error', {
        order: [
          'DEFINITION',
          'LIST_RENDERING',
          'CONDITIONALS',
          'RENDER_MODIFIERS',
          'GLOBAL',
          ['UNIQUE', 'SLOT'],
          'TWO_WAY_BINDING',
          'OTHER_DIRECTIVES',
          'OTHER_ATTR',
          'EVENTS',
          'CONTENT',
        ],
        alphabetical: true,
      }],
      'vue/no-useless-mustaches': ['error', {
        ignoreIncludesComment: false,
        ignoreStringEscape: false,
      }],
      'vue/no-useless-v-bind': ['error', {
        ignoreIncludesComment: false,
        ignoreStringEscape: false,
      }],
      'vue/prefer-separate-static-class': 'error',
      'vue/prefer-true-attribute-shorthand': 'error',
      'vue/script-indent': ['error', 2, {
        baseIndent: 1,
      }],
      'vue/component-name-in-template-casing': ['error', 'kebab-case', {
        registeredComponentsOnly: false,
        ignores: [],
      }],
      'vue/require-default-prop': 'off',
      'vue/no-v-html': 'off',
      'no-restricted-syntax': 'off',
    },
  },
];

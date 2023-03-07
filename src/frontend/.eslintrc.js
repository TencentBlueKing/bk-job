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

module.exports = {
  parser: 'vue-eslint-parser',
  parserOptions: {
    parser: '@babel/eslint-parser',
    ecmaVersion: 12,
    sourceType: 'module',
  },
  env: {
    browser: true,
  },
  extends: [
    'eslint-config-tencent',
    'eslint:recommended',
    'plugin:vue/recommended',
  ],
  // required to lint *.vue files
  plugins: [
    'vue',
    'simple-import-sort',
  ],
  globals: {
    // value 为 true 允许被重写，为 false 不允许被重写
    NODE_ENV: false,
    LOCAL_DEV_URL: false,
    LOCAL_DEV_PORT: false,
    AJAX_URL_PREFIX: false,
    AJAX_MOCK_PARAM: false,
    USER_INFO_URL: false,
  },
  // add your custom rules hered
  rules: {
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
        ['^\\.\\.'],
        ['^\\.'],
      ],
    }],
    'no-param-reassign': 'off',
    'max-len': 'off',
    'vue/multi-word-component-names': 'off',
    'no-unused-vars': 'off',
  },
  overrides: [
    {
      files: ['*.vue'],
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
  ],
};

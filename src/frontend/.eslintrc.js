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
        'standard',
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
        // 要求或禁止末尾逗号
        'comma-dangle': ['error', 'always-multiline'],
        // 强制在代码块中使用一致的大括号风格
        // https://eslint.org/docs/rules/brace-style
        'brace-style': ['error', '1tbs', { allowSingleLine: false }],
        // 强制在 parseInt() 使用基数参数
        radix: 'error',
        'no-restricted-properties': [2],
        'no-case-declarations': 'error',
        'no-else-return': 'error',
        'no-loop-func': 'error',
        'no-useless-escape': 'error',
        'no-param-reassign': 'error',
        'no-plusplus': 'error',
        'no-nested-ternary': 'error',
        'prefer-template': 'error',
        'arrow-parens': ['error', 'as-needed', { requireForBlockBody: true }],
        'arrow-body-style': ['error', 'as-needed'],
        'prefer-destructuring': ['error'],
        'prefer-arrow-callback': ['error'],
        'function-paren-newline': ['error'],
        'func-style': ['error'],
        'object-shorthand': 'error',
        'quote-props': ['error', 'as-needed'],
        'no-restricted-syntax': ['error', 'BinaryExpression[operator="in"]'],
        'no-underscore-dangle': [
            'error',
            {
                allowAfterThis: false,
                allowAfterSuper: false,
            },
        ],
        'max-len': [
            'error',
            {
                code: 120,
            },
        ],
        'implicit-arrow-linebreak': ['error'],
        'newline-per-chained-call': ['error'],
        'no-whitespace-before-property': 'error',

        // https://eslint.org/docs/rules/camelcase
        camelcase: ['error', { properties: 'never', ignoreDestructuring: true }],

        // 缩进使用 4 个空格，并且 switch 语句中的 Case 需要缩进
        // https://eslint.org/docs/rules/indent
        indent: [
            'error',
            4,
            {
                SwitchCase: 1,
                flatTernaryExpressions: true,
            },
        ],

        // 数组的括号内的前后禁止有空格
        // https://eslint.org/docs/rules/array-bracket-spacing
        'array-bracket-spacing': ['error', 'never'],

        // https://eslint.org/docs/rules/operator-linebreak
        'operator-linebreak': ['error', 'before'],

        // 在开发阶段打开调试
        // https://eslint.org/docs/rules/no-debugger
        'no-debugger': 'off',

        // 禁止空语句（可在空语句写注释避免），允许空的 catch 语句
        // https://eslint.org/docs/rules/no-empty
        'no-empty': ['error', { allowEmptyCatch: true }],

        // 禁止在语句末尾使用分号
        // https://eslint.org/docs/rules/semi
        semi: ['error', 'always'],

        // 禁用不必要的分号
        // https://eslint.org/docs/rules/no-extra-semi
        'no-extra-semi': 'error',

        // generator 的 * 前面禁止有空格，后面必须有空格
        // https://eslint.org/docs/rules/generator-star-spacing
        'generator-star-spacing': [
            'error',
            {
                before: false,
                after: true,
            },
        ],

        // 函数圆括号之前有一个空格
        // https://eslint.org/docs/rules/space-before-function-paren
        'space-before-function-paren': [
            'error',
            {
                anonymous: 'always', // 匿名函数表达式
                named: 'always', // 命名的函数表达式
                asyncArrow: 'always', // 异步的箭头函数表达式
            },
        ],

        // 禁止行尾有空格
        // https://eslint.org/docs/rules/no-trailing-spaces
        'no-trailing-spaces': [
            'error',
            {
                skipBlankLines: true, // 允许在空行使用空白符
            },
        ],

        // 注释的斜线或 * 后必须有空格
        // https://eslint.org/docs/rules/spaced-comment
        'spaced-comment': [
            'error',
            'always',
            {
                line: {
                    markers: ['*package', '!', '/', ',', '='],
                },
                block: {
                    // 前后空格是否平衡
                    balanced: false,
                    markers: ['*package', '!', ',', ':', '::', 'flow-include'],
                    exceptions: ['*'],
                },
            },
        ],

        // https://eslint.org/docs/rules/no-template-curly-in-string
        // 禁止在字符串中使用字符串模板。不限制
        'no-template-curly-in-string': 'off',

        // https://eslint.org/docs/rules/no-var
        // 禁止使用 var
        'no-var': 'error',

        // https://eslint.org/docs/rules/prefer-const
        // 如果一个变量不会被重新赋值，必须使用 `const` 进行声明。
        'prefer-const': 'error',
        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/array-bracket-spacing.md
        // 'vue/array-bracket-spacing': ['error', 'never'],

        'vue/multi-word-component-names': 'off',

        'vue/script-setup-uses-vars': 'off',
        'vue/html-closing-bracket-newline': ['error', {
            singleline: 'never',
            multiline: 'never',
        }],
        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/html-indent.md
        'vue/html-indent': ['error', 4, {
            attribute: 1,
            baseIndent: 1,
            closeBracket: 0,
            alignAttributesVertically: false,
            ignores: [],
        }],

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/no-v-html.md
        // 禁止使用 v-html，防止 xss
        'vue/no-v-html': 'off',

    },
    overrides: [
        {
            files: ['*.vue'],
            rules: {
                indent: 'off',
                'max-len': 'off',
                'import/first': 'off',
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
                'vue/require-default-prop': 'off',
            },
        },
    ],
};

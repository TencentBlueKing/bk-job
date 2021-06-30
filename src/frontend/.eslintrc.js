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
        'plugin:vue/essential',
    ],
    // required to lint *.vue files
    plugins: ['vue'],
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
        'comma-dangle': ['error', 'always-multiline'],
        // https://eslint.org/docs/rules/brace-style
        'brace-style': ['error', '1tbs', { allowSingleLine: false }],
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

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/arrow-spacing.md
        'vue/arrow-spacing': ['error', { before: true, after: true }],

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/attribute-hyphenation.md
        'vue/attribute-hyphenation': ['error', 'always'],

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/attributes-order.md
        // 属性顺序，不限制
        'vue/attributes-order': 'off',

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/block-spacing.md
        'vue/block-spacing': ['error', 'always'],

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/brace-style.md
        'vue/brace-style': ['error', '1tbs', { allowSingleLine: false }],

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/camelcase.md
        // 后端数据字段经常不是驼峰，所以不限制 properties，也不限制解构
        'vue/camelcase': ['error', { properties: 'never', ignoreDestructuring: true }],

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/comma-dangle.md
        // 禁止使用拖尾逗号，如 {demo: 'test',}
        'vue/comma-dangle': ['error', 'always-multiline'],

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/comment-directive.md
        // vue 文件 template 中允许 eslint-disable eslint-enable eslint-disable-line eslint-disable-next-line
        // 行内注释启用/禁用某些规则，配置为 1 即允许
        'vue/comment-directive': 1,

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/component-name-in-template-casing.md
        // 组件 html 标签的形式，连字符形式，所有 html 标签均会检测，如引入第三方不可避免，可通过 ignores 配置，支持正则，不限制
        'vue/component-name-in-template-casing': 'off',

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/dot-location.md
        // 等待 https://github.com/vuejs/eslint-plugin-vue/pull/794 合入
        // 'vue/dot-location': ['error', 'property'],

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/eqeqeq.md
        'vue/eqeqeq': ['error', 'always', { null: 'ignore' }],

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/html-closing-bracket-newline.md
        // 单行写法不需要换行，多行需要，不限制
        'vue/html-closing-bracket-newline': 'off',

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/html-closing-bracket-spacing.md
        'vue/html-closing-bracket-spacing': ['error', {
            startTag: 'never',
            endTag: 'never',
            selfClosingTag: 'always',
        }],

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/html-end-tags.md
        'vue/html-end-tags': 'error',

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/html-indent.md
        'vue/html-indent': ['error', 4, {
            attribute: 1,
            baseIndent: 1,
            closeBracket: 0,
            alignAttributesVertically: false,
            ignores: [],
        }],

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/html-quotes.md
        'vue/html-quotes': ['error', 'double'],

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/html-self-closing.md
        // html tag 是否自闭和，不限制
        'vue/html-self-closing': 'error',

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/jsx-uses-vars.md
        // 当变量在 `JSX` 中被使用了，那么 eslint 就不会报出 `no-unused-vars` 的错误。需要开启 eslint no-unused-vars 规则才适用
        'vue/jsx-uses-vars': 1,

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/key-spacing.md
        'vue/key-spacing': ['error', { beforeColon: false, afterColon: true }],

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/multiline-html-element-content-newline.md
        // 在多行元素的内容前后需要换行符，不限制
        'vue/multiline-html-element-content-newline': 'off',

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/mustache-interpolation-spacing.md
        // template 中 {{var}}，不限制
        'vue/mustache-interpolation-spacing': 'error',

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/no-confusing-v-for-v-if.md
        'vue/no-confusing-v-for-v-if': 'error',

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/no-multi-spaces.md
        // 删除 html 标签中连续多个不用于缩进的空格
        'vue/no-multi-spaces': 'error',

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/no-restricted-syntax.md
        // 禁止使用特定的语法
        'vue/no-restricted-syntax': ['error', 'BinaryExpression[operator="in"]'],

        // eslint-disable-next-line max-len
        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/no-spaces-around-equal-signs-in-attribute.md
        'vue/no-spaces-around-equal-signs-in-attribute': 'error',

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/no-template-shadow.md
        'vue/no-template-shadow': 'error',

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/no-v-html.md
        // 禁止使用 v-html，防止 xss
        'vue/no-v-html': 'off',

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/object-curly-spacing.md
        // 对象写在一行时，大括号里需要空格
        'vue/object-curly-spacing': ['error', 'always'],

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/order-in-components.md
        // 官方推荐顺序
        'vue/order-in-components': ['error', {
            order: [
                'el',
                'name',
                'parent',
                'functional',
                ['delimiters', 'comments'],
                ['components', 'directives', 'filters'],
                'extends',
                'mixins',
                'inheritAttrs',
                'model',
                ['props', 'propsData'],
                'data',
                'computed',
                'watch',
                'LIFECYCLE_HOOKS',
                'methods',
                ['template', 'render'],
                'renderError',
            ],
        }],

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/prop-name-casing.md
        // 组件 props 属性名驼峰命名
        'vue/prop-name-casing': ['error', 'camelCase'],

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/require-direct-export.md
        // 组件必须要直接被 export。不限制
        'vue/require-direct-export': 'error',

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/require-prop-types.md
        // props 必须要有 type。
        'vue/require-prop-types': 'error',

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/require-valid-default-prop.md
        // props 默认值必须有效。不限制
        'vue/require-valid-default-prop': 'off',

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/script-indent.md
        'vue/script-indent': ['error', 4, {
            baseIndent: 1,
            switchCase: 1,
        }],

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/singleline-html-element-content-newline.md
        // 单行 html 元素后面必须换行。不限制
        'vue/singleline-html-element-content-newline': 'off',

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/space-unary-ops.md
        // new, delete, typeof, void, yield 等后面必须有空格，一元操作符 -, +, --, ++, !, !! 禁止有空格
        'vue/space-unary-ops': ['error', { words: true, nonwords: false }],

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/this-in-template.md
        // 不允许在 template 中使用 this
        'vue/this-in-template': ['error', 'never'],

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/use-v-on-exact.md
        // 强制使用精确修饰词。不限制
        'vue/use-v-on-exact': 'off',

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/v-on-function-call.md
        // 强制或禁止在 v-on 指令中不带参数的方法调用后使用括号。不限制
        'vue/v-on-function-call': 'error',

        // https://github.com/vuejs/eslint-plugin-vue/blob/master/docs/rules/v-on-style.md
        // v-on 指令的写法。限制简写
        'vue/v-on-style': ['error', 'shorthand'],

    },
    overrides: [
        {
            files: ['*.vue'],
            rules: {
                indent: 'off',
                'max-len': 'off',
            },
        },
    ],
};

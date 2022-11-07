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
        ref="layout"
        :class="classes">
        <slot />
    </div>
</template>
<script>
    export default {
        name: 'GlobalVariableLayout',

        props: {
            type: {
                type: String,
                default: 'horizontal', // 水平：horizontal；垂直：vertical
            },
        },
        computed: {
            classes () {
                const classes = {
                    'global-variable-layout': true,
                };
                if (this.type !== 'horizontal') {
                    classes.vertical = true;
                }
                return classes;
            },
        },
        updated () {
            const childrenNum = this.$slots.default;
            if (this.childrenNum !== childrenNum) {
                this.childrenNum = childrenNum;
                this.init();
            }
        },
        mounted  () {
            if (this.type === 'horizontal') {
                this.init();
            }
        },
        methods: {
            init () {
                const isShowLayout = this.$refs.layout.getBoundingClientRect().width > 0;
                if (!isShowLayout) {
                    return;
                }
                const $formEle = this.$refs.layout;
                let max = 0;
                const $labelEleList = $formEle.querySelectorAll('.variable-name');
                $labelEleList.forEach((item) => {
                    const { width } = item.querySelector('span').getBoundingClientRect();
                    max = Math.max(max, width);
                });
                $labelEleList.forEach((item) => {
                    item.style.width = `${max}px`;
                });
            },
        },
    };
</script>
<style lang='postcss'>
    .global-variable-layout {
        display: flex;
        flex-direction: column;

        &.vertical {
            .global-variable-edit-box {
                flex-direction: column;
            }

            .variable-name {
                text-align: left;
            }
        }
    }
</style>

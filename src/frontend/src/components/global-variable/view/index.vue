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
        class="global-variable-detail"
        :class="classes">
        <div class="variable-name">
            <span>{{ data.name }}：</span>
        </div>
        <div class="variable-value">
            <component
                :is="typeCom"
                ref="value"
                :data="data"
                v-bind="$attrs"
                v-on="$listeners" />
        </div>
    </div>
</template>
<script>
    import TypeHost from './host';
    import TypeText from './text';

    export default {
        props: {
            type: {
                type: Number,
                required: true,
            },
            valueWidth: {
                type: String,
                default: '500px',
            },
            data: {
                type: Object,
                required: true,
            },
            layout: {
                type: String,
                default: 'horizontal', // 水平：horizontal；垂直：vertical
            },
        },
        data () {
            return {
                isError: false,
                isEmpty: false,
            };
        },
        computed: {
            typeCom () {
                const comMap = {
                    1: TypeText,
                    2: TypeText,
                    3: TypeHost,
                    4: TypeText,
                    5: TypeText,
                    6: TypeText,
                };
                if (!Object.prototype.hasOwnProperty.call(comMap, this.type)) {
                    return 'div';
                }
                
                return comMap[this.type];
            },
            classes () {
                const classes = {};
                if (this.isEmpty) {
                    return classes;
                }
                if (this.layout === 'vertical') {
                    classes.vertical = true;
                }
                return classes;
            },
        },
        mounted () {
            const unWatch = this.$watch(() => this.$refs.value.isEmpty, (value) => {
                this.isEmpty = Boolean(value);
            }, {
                immediate: true,
            });
            this.$once('hook:beforeDestroy', () => {
                unWatch();
            });
        },
    };
</script>
<style lang='postcss'>
    .global-variable-detail {
        display: flex;
        flex: 1;
        font-size: 14px;
        line-height: 36px;

        &.vertical {
            flex-direction: column;
        }

        .variable-name {
            padding-right: 2px;
            color: #b2b5bd;
            text-align: right;
            white-space: normal;
            box-sizing: content-box;
        }

        .variable-value {
            flex: 1 0;
            color: #63656e;
        }
    }
</style>

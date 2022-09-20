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
        v-test="{ type: 'navigation', value: index }"
        :class="classes"
        @click="handleClick">
        <slot />
    </div>
</template>
<script>
    export default {
        name: 'JbMenuItem',
        inject: ['jbMenu'],
        props: {
            index: {
                type: String,
                required: true,
            },
        },
        data () {
            return {};
        },
        computed: {
            active () {
                return this.index === this.jbMenu.activeIndex;
            },
            classes () {
                return {
                    'jb-menu-item': true,
                    active: this.index === this.jbMenu.activeIndex,
                    flod: this.jbMenu.flod,
                };
            },
        },
        watch: {
            $route: {
                handler  (route) {
                    route.matched.forEach((currentMatch) => {
                        if (currentMatch.name === this.index) {
                            this.jbMenu.activeIndex = this.index;
                        }
                    });
                },
                immediate: true,
            },
        },
        mounted () {
            this.jbMenu.addItem(this);
        },
        methods: {
            handleClick () {
                this.jbMenu.$emit('item-click', this);
                this.$emit('click', this);
            },
        },
    };
</script>

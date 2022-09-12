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
    <div class="notify-collapse-item">
        <bk-collapse-item
            ref="collapseItem"
            v-bind="$attrs"
            hide-arrow
            v-on="$listeners">
            <template #default>
                <Icon
                    style="color: #979ba5;"
                    :type="iconType" />
                <span style="display: none;">{{ iconType }}</span>
                <slot />
            </template>
            <template #content>
                <slot name="content" />
            </template>
        </bk-collapse-item>
    </div>
</template>
<script>
    export default {
        inject: ['collapse'],
        data () {
            return {
                iconType: 'arrow-full-right',
            };
        },
        mounted () {
            const unwatch = this.$watch(() => this.$refs.collapseItem.isActive, (newValue) => {
                this.iconType = newValue ? 'arrow-full-down' : 'arrow-full-right';
            }, {
                immediate: true,
            });
            this.$once('hook:beforeDestroy', () => {
                unwatch();
            });
        },
    };
</script>
<style lang='postcss'>
    .notify-collapse-item {
        .bk-collapse-item-header {
            font-size: 14px;
            font-weight: 600;
            color: #313238;
            border-bottom: 1px solid #dcdee5;
        }

        .bk-collapse-item .bk-collapse-item-content {
            min-height: 42px;
            padding: 40px 0 0 84px;
        }
    }
</style>

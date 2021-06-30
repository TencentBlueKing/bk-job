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
    <table class="ip-table">
        <colgroup>
            <template v-for="item in columns">
                <col
                    v-if="showColumns.includes(item.name)"
                    :key="item.name"
                    :name="item.name"
                    :width="item.width">
            </template>
            <col key="setting" name="setting" width="40">
        </colgroup>
        <tbody>
            <tr
                v-for="(item, index) in data"
                :key="item.key"
                :class="{
                    active: selectRow === item.key,
                }"
                @click="handleSelect(item, index)">
                <template v-for="(showKey, colIndex) in showColumns">
                    <td :key="showKey" :class="colIndex === 0 && item.result">
                        {{ item[showKey] }}
                    </td>
                </template>
                <td class="active-flag">
                    <Icon v-if="selectRow === item.key" type="arrow-full-right" />
                </td>
            </tr>
        </tbody>
    </table>
</template>
<script>
    import _ from 'lodash';

    export default {
        name: '',
        props: {
            data: {
                type: Array,
                required: true,
            },
            columns: {
                type: Array,
                required: true,
            },
            showColumns: {
                type: Array,
                required: true,
            },
        },
        data () {
            return {
                list: [],
                selectRow: '',
            };
        },
        watch: {
            data: {
                handler (data) {
                    if (data.length < 1) {
                        this.handleSelect({});
                        return;
                    }
                    
                    if (!this.selectRow || !_.find(this.data, _ => this.selectRow === _.key)) {
                        this.handleSelect(this.data[0]);
                    }
                },
                immediate: true,
            },
        },
        methods: {
            handleSelect (payload) {
                this.selectRow = payload.key;
                this.$emit('on-row-select', payload);
            },
        },
    };
</script>

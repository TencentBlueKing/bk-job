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
    <render-list
        ref="list"
        :data-source="fetchUnlaunchHistory"
        ignore-url>
        <bk-table-column
            key="scheduledTime"
            align="left"
            :label="$t('cron.唤起时间')"
            prop="scheduledTime"
            width="180" />
        <bk-table-column
            key="executor"
            align="left"
            :label="$t('cron.执行人.colHead')"
            prop="executor"
            width="180" />
        <bk-table-column
            key="errorMsg"
            align="left"
            :label="$t('cron.原因')"
            prop="errorMsg">
            <template slot-scope="{ row }">
                {{ row.errorMsg || '--' }}
            </template>
        </bk-table-column>
    </render-list>
</template>
<script>
    import TimeTaskService from '@service/time-task';

    import RenderList from '@components/render-list';

    export default {
        name: '',
        components: {
            RenderList,
        },
        props: {
            data: {
                type: Object,
                required: true,
            },
        },
        data () {
            return {
                searchParams: {},
            };
        },
        created () {
            this.searchParams.cronTaskId = this.data.id;
            this.fetchUnlaunchHistory = TimeTaskService.fetchUnlaunchHistory;
        },
        mounted () {
            this.fetchData();
        },
        methods: {
            fetchData () {
                this.$refs.list.$emit('onFetch', this.searchParams);
            },
        },
    };
</script>

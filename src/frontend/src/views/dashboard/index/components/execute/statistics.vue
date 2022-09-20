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
    <div class="execute-statistics-dashboard">
        <div class="header-action">
            <div class="type-tab">
                <template v-if="!isDrillDown">
                    <div
                        class="tab-item"
                        :class="{
                            active: dimension === 'TASK_STARTUP_MODE',
                        }"
                        @click="handleDimensionChange('TASK_STARTUP_MODE')">
                        {{ $t('dashboard.按渠道统计') }}
                    </div>
                    <div
                        class="tab-item"
                        :class="{
                            active: dimension === 'TASK_TYPE',
                        }"
                        @click="handleDimensionChange('TASK_TYPE')">
                        {{ $t('dashboard.按类型统计') }}
                    </div>
                    <div
                        class="tab-item"
                        :class="{
                            active: dimension === 'TASK_TIME_CONSUMING',
                        }"
                        @click="handleDimensionChange('TASK_TIME_CONSUMING')">
                        {{ $t('dashboard.按执行耗时统计') }}
                    </div>
                </template>
                <div
                    v-if="isDrillDown"
                    class="tab-back">
                    <span
                        class="tab-back-action"
                        @click="handleDimensionBack">
                        <Icon type="back1" />
                        {{ $t('dashboard.返回') }}
                    </span>
                    <span>{{ $t('dashboard.类型统计') }}</span>
                </div>
            </div>
            <div class="time-tab">
                <div
                    class="tab-item"
                    :class="{ active: days === 7 }"
                    @click="handleDaysChange(7)">
                    {{ $t('dashboard.7 天内') }}
                </div>
                <div
                    class="tab-item"
                    :class="{ active: days === 14 }"
                    @click="handleDaysChange(14)">
                    {{ $t('dashboard.14 天内') }}
                </div>
                <div
                    class="tab-item"
                    :class="{ active: days === 30 }"
                    @click="handleDaysChange(30)">
                    {{ $t('dashboard.30 天内') }}
                </div>
            </div>
        </div>
        <div v-bkloading="{ isLoading, opacity: 0.8 }">
            <div
                ref="dashboard"
                style="width: 100%; height: 325px;" />
        </div>
    </div>
</template>
<script>
/* eslint-disable max-len */
    import echarts from 'lib/echarts.min.js';
    import _ from 'lodash';

    import StatisticsService from '@service/statistics';

    import {
        formatNumber,
        prettyDateFormat,
    } from '@utils/assist';

    import {
        chartsOptionsBase,
    } from '../common/assist';

    import I18n from '@/i18n';

    const tooltipFormatter = (params) => {
        const generatorHtml = (data) => {
            const {
                seriesType,
                seriesName,
                color,
                value,
            } = data;
            
            if (seriesType === 'bar') {
                return `
                    <tr>
                        <td style="padding-right: 24px; vertical-align: middle;">
                            <span style="display: inline-block; width: 8px; height: 8px; background: ${color}"></span>
                            <span>${seriesName}</span>
                        </td>
                        <td style="text-align: right">${value}</td>
                    </tr>
                `;
            } else if (seriesType === 'line') {
                return `
                    <tr>
                        <td style="padding-right: 24px; vertical-align: middle;">
                            <span style="display: inline-block; width: 8px; height: 8px; border-radius: 50%; background: ${color}"></span>
                            <span>${seriesName}</span>
                        </td>
                        <td style="text-align: right">${value}</td>
                    </tr>
                `;
            }
        };
        let paramsArr = [];
        if (Object.prototype.toString.call(params) === '[object Object]') {
            paramsArr = [
                params,
            ];
        } else {
            paramsArr = params;
        }
        
        return `<table>${paramsArr.map(generatorHtml).join('')}</table>`;
    };

    export default {
        name: '',
        props: {
            date: {
                type: String,
                required: true,
            },
        },
        data () {
            return {
                isLoading: false,
                dimension: '',
                days: 7,
                isDrillDown: false,
            };
        },
        watch: {
            date () {
                this.handleDaysChange(this.days);
            },
        },
        mounted () {
            // 下钻的作业类型
            this.drillDowntaskType = '';

            this.myChart = echarts.init(this.$refs.dashboard);
            this.myChart.on('click', (target) => {
                if ([
                    'EXECUTED_FAST_SCRIPT',
                    'EXECUTED_FAST_FILE',
                ].includes(target.seriesId)) {
                    this.handleTaskTypeChange(target.seriesId);
                }
            });
            
            const resize = _.throttle(() => {
                this.myChart.resize();
            }, 300);
            window.addEventListener('resize', resize);
            this.$once('hook:beforeDestroy', () => {
                window.removeEventListener('resize', resize);
            });
            this.handleDimensionChange('TASK_STARTUP_MODE');
        },
        methods: {
            /**
             * @desc 按渠道统计
             * @param {Array} data 数据
             */
            initTaskStartupMode (data) {
                const dateList = [];
                const normalList = [];
                const cronList = [];
                const apiList = [];
                const failList = [];
                const totalList = [];
                data.forEach((item) => {
                    const {
                        date,
                        distribution,
                        failCount,
                    } = item;
                    dateList.push(date);
                    const {
                        API,
                        CRON,
                        NORMAL,
                    } = distribution.labelAmountMap;
                    
                    apiList.push(API);
                    cronList.push(CRON);
                    normalList.push(NORMAL);
                    failList.push(failCount);
                    totalList.push(API + CRON + NORMAL);
                });

                this.myChart.clear();
                this.myChart.setOption({
                    ...chartsOptionsBase,
                    legend: {
                        data: [
                            I18n.t('dashboard.页面执行'),
                            I18n.t('dashboard.API 调用'),
                            I18n.t('dashboard.定时执行'),
                            {
                                name: I18n.t('dashboard.执行次数'),
                                icon: 'circle',
                            },
                            {
                                name: I18n.t('dashboard.执行失败次数'),
                                icon: 'circle',
                            },
                        ],
                        bottom: '0',
                        icon: 'rect',
                        itemWidth: 8,
                        itemHeight: 8,
                        itemGap: 40,
                    },
                    tooltip: {
                        trigger: 'axis',
                        axisPointer: {
                            type: 'line',
                            lineStyle: {
                                width: 30,
                                color: '#63656E',
                                opacity: 0.16,
                            },
                        },
                        formatter: tooltipFormatter,
                        backgroundColor: 'rgba(0,0,0,0.8)',
                    },
                    grid: {
                        top: 45,
                        left: 40,
                        right: 0,
                        bottom: 48,
                        containLabel: true,
                    },
                    xAxis: [
                        {
                            type: 'category',
                            axisLine: {
                                lineStyle: {
                                    color: '#DCDEE5',
                                },
                            },
                            axisTick: {
                                show: false,
                            },
                            axisLabel: {
                                color: '#979BA5',
                                margin: 18,
                            },
                            data: dateList,
                        },
                    ],
                    yAxis: {
                        type: 'value',
                        name: I18n.t('dashboard.执行数'),
                        nameLocation: 'middle',
                        nameTextStyle: {
                            color: '#63656E',
                        },
                        nameGap: 50,
                        axisLine: {
                            show: false,
                        },
                        axisTick: {
                            show: false,
                        },
                        axisLabel: {
                            color: '#979BA5',
                            formatter: value => formatNumber(value, true),
                        },
                        splitLine: {
                            lineStyle: {
                                color: '#F0F1F5',
                            },
                        },
                    },
                    series: [
                        {
                            name: I18n.t('dashboard.页面执行'),
                            type: 'bar',
                            stack: 'bar',
                            barWidth: 20,
                            data: normalList,
                            itemStyle: {
                                color: '#D4E6C1',
                            },
                        },
                        {
                            name: I18n.t('dashboard.API 调用'),
                            type: 'bar',
                            stack: 'bar',
                            barWidth: 20,
                            data: apiList,
                            itemStyle: {
                                color: '#85CCA8',
                            },
                        },
                        {
                            name: I18n.t('dashboard.定时执行'),
                            type: 'bar',
                            stack: 'bar',
                            barWidth: 20,
                            data: cronList,
                            itemStyle: {
                                color: '#3786AD',
                            },
                        },
                        {
                            name: I18n.t('dashboard.执行次数'),
                            type: 'line',
                            data: totalList,
                            itemStyle: {
                                color: '#FFD695',
                            },
                        },
                        {
                            name: I18n.t('dashboard.执行失败次数'),
                            type: 'line',
                            data: failList,
                            itemStyle: {
                                color: '#FF5656',
                            },
                        },
                    ],
                });
            },
            /**
             * @desc 按任务类型统计
             * @param {Array} data 数据
             */
            initTaskType (data) {
                const dateList = [];
                const fastPushFileList = [];
                const fastExecuteScriptList = [];
                const executeTaskList = [];
                const failList = [];
                data.forEach((item) => {
                    const {
                        date,
                        distribution,
                        failCount,
                    } = item;
                    dateList.push(date);
                    const {
                        EXECUTE_TASK,
                        FAST_EXECUTE_SCRIPT,
                        FAST_PUSH_FILE,
                    } = distribution.labelAmountMap;
                    
                    executeTaskList.push(EXECUTE_TASK);
                    fastExecuteScriptList.push(FAST_EXECUTE_SCRIPT);
                    fastPushFileList.push(FAST_PUSH_FILE);
                    failList.push(failCount);
                });

                this.myChart.clear();
                this.myChart.setOption({
                    ...chartsOptionsBase,
                    legend: {
                        data: [
                            I18n.t('dashboard.快速执行脚本'),
                            I18n.t('dashboard.快速分发文件'),
                            I18n.t('dashboard.作业执行'),
                            {
                                name: I18n.t('dashboard.执行失败次数'),
                                icon: 'circle',
                            },
                        ],
                        bottom: '0',
                        icon: 'rect',
                        itemWidth: 8,
                        itemHeight: 8,
                        itemGap: 40,
                    },
                    tooltip: {
                        trigger: 'item',
                        backgroundColor: 'rgba(0,0,0,0.8)',
                        formatter: tooltipFormatter,
                    },
                    grid: {
                        top: 45,
                        left: 40,
                        right: 0,
                        bottom: 48,
                        containLabel: true,
                    },
                    xAxis: [
                        {
                            type: 'category',
                            axisLine: {
                                lineStyle: {
                                    color: '#DCDEE5',
                                },
                            },
                            axisTick: {
                                show: false,
                            },
                            axisLabel: {
                                color: '#979BA5',
                                margin: 18,
                            },
                            data: dateList,
                        },
                    ],
                    yAxis: {
                        type: 'value',
                        name: I18n.t('dashboard.执行数'),
                        nameLocation: 'middle',
                        nameTextStyle: {
                            color: '#63656E',
                        },
                        nameGap: 50,
                        axisLine: {
                            show: false,
                        },
                        axisTick: {
                            show: false,
                        },
                        axisLabel: {
                            color: '#979BA5',
                            formatter: value => formatNumber(value, true),
                        },
                        splitLine: {
                            lineStyle: {
                                color: '#F0F1F5',
                            },
                        },
                    },
                    series: [
                        {
                            id: 'EXECUTED_FAST_SCRIPT',
                            name: I18n.t('dashboard.快速执行脚本'),
                            type: 'bar',
                            stack: 'bar',
                            barWidth: 20,
                            data: fastExecuteScriptList,
                            itemStyle: {
                                color: '#D4E6C1',
                            },
                        },
                        {
                            id: 'EXECUTED_FAST_FILE',
                            name: I18n.t('dashboard.快速分发文件'),
                            type: 'bar',
                            stack: 'bar',
                            barWidth: 20,
                            data: fastPushFileList,
                            itemStyle: {
                                color: '#85CCA8',
                            },
                        },
                        {
                            id: 'EXECUTE_TASK',
                            name: I18n.t('dashboard.作业执行'),
                            type: 'bar',
                            stack: 'bar',
                            barWidth: 20,
                            data: executeTaskList,
                            itemStyle: {
                                color: '#3786AD',
                            },
                        },
                        {
                            name: I18n.t('dashboard.执行失败次数'),
                            type: 'line',
                            data: failList,
                            itemStyle: {
                                color: '#FF5656',
                            },
                        },
                    ],
                });
            },
            /**
             * @desc 按执行耗时统计
             * @param {Array} data 数据
             */
            initTaskTimeConsuming (data) {
                const dateList = [];
                const overTenMinList = [];
                const oneMinToTenMinList = [];
                const lessThanOneMinList = [];
                const failList = [];
                data.forEach((item) => {
                    const {
                        date,
                        distribution,
                        failCount,
                    } = item;
                    dateList.push(date);
                    const {
                        LESS_THAN_ONE_MIN,
                        ONE_MIN_TO_TEN_MIN,
                        OVER_TEN_MIN,
                    } = distribution.labelAmountMap;

                    lessThanOneMinList.push(LESS_THAN_ONE_MIN);
                    oneMinToTenMinList.push(ONE_MIN_TO_TEN_MIN);
                    overTenMinList.push(OVER_TEN_MIN);
                    failList.push(failCount);
                });

                this.myChart.clear();
                this.myChart.setOption({
                    ...chartsOptionsBase,
                    legend: {
                        data: [
                            I18n.t('dashboard.≥ 10分钟'),
                            I18n.t('dashboard.1~10分钟以内（包含10分钟）'),
                            I18n.t('dashboard.1分钟以内（包含1分钟）'),
                            {
                                name: I18n.t('dashboard.执行失败次数'),
                                icon: 'circle',
                            },
                        ],
                        bottom: '0',
                        icon: 'rect',
                        itemWidth: 8,
                        itemHeight: 8,
                        itemGap: 40,
                    },
                    tooltip: {
                        trigger: 'axis',
                        axisPointer: {
                            type: 'line',
                            lineStyle: {
                                width: 30,
                                color: '#63656E',
                                opacity: 0.16,
                            },
                        },
                        formatter: tooltipFormatter,
                        backgroundColor: 'rgba(0,0,0,0.8)',
                    },
                    grid: {
                        top: 45,
                        left: 40,
                        right: 0,
                        bottom: 48,
                        containLabel: true,
                    },
                    xAxis: [
                        {
                            type: 'category',
                            axisLine: {
                                lineStyle: {
                                    color: '#DCDEE5',
                                },
                            },
                            axisTick: {
                                show: false,
                            },
                            axisLabel: {
                                color: '#979BA5',
                                margin: 18,
                            },
                            data: dateList,
                        },
                    ],
                    yAxis: {
                        type: 'value',
                        name: I18n.t('dashboard.执行数'),
                        nameLocation: 'middle',
                        nameTextStyle: {
                            color: '#63656E',
                        },
                        nameGap: 50,
                        axisLine: {
                            show: false,
                        },
                        axisTick: {
                            show: false,
                        },
                        axisLabel: {
                            color: '#979BA5',
                            formatter: value => formatNumber(value, true),
                        },
                        splitLine: {
                            lineStyle: {
                                color: '#F0F1F5',
                            },
                        },
                    },
                    series: [
                        {
                            name: I18n.t('dashboard.≥ 10分钟'),
                            type: 'bar',
                            stack: 'bar',
                            barWidth: 20,
                            data: overTenMinList,
                            itemStyle: {
                                color: '#D4E6C1',
                            },
                        },
                        {
                            name: I18n.t('dashboard.1~10分钟以内（包含10分钟）'),
                            type: 'bar',
                            stack: 'bar',
                            barWidth: 20,
                            data: oneMinToTenMinList,
                            itemStyle: {
                                color: '#85CCA8',
                            },
                        },
                        {
                            name: I18n.t('dashboard.1分钟以内（包含1分钟）'),
                            type: 'bar',
                            stack: 'bar',
                            barWidth: 20,
                            data: lessThanOneMinList,
                            itemStyle: {
                                color: '#3786AD',
                            },
                        },
                        {
                            name: I18n.t('dashboard.执行失败次数'),
                            type: 'line',
                            data: failList,
                            itemStyle: {
                                color: '#FF5656',
                            },
                        },
                    ],
                });
            },
            /**
             * @desc 任务类型快速执行脚本下钻统计
             * @param {Array} data 数据
             *
             */
            initExecutedFastScriptDrillDown (data) {
                const dateList = [];
                const BatList = [];
                const PerlList = [];
                const PowerShellList = [];
                const PythonList = [];
                const ShellList = [];
                const SQLList = [];
                const failList = [];

                data.forEach((item) => {
                    const {
                        date,
                        distribution,
                        failCount,
                    } = item;
                    dateList.push(date);
                    const {
                        bat,
                        perl,
                        powershell,
                        python,
                        shell,
                        sql,
                    } = distribution.labelAmountMap;

                    BatList.push(bat);
                    PerlList.push(perl);
                    PowerShellList.push(powershell);
                    PythonList.push(python);
                    ShellList.push(shell);
                    SQLList.push(sql);
                    failList.push(failCount);
                });

                this.myChart.clear();
                this.myChart.setOption({
                    ...chartsOptionsBase,
                    legend: {
                        data: [
                            'Bat',
                            'Perl',
                            'Python',
                            'Shell',
                            'PowerShell',
                            'SQL',
                            {
                                name: I18n.t('dashboard.执行失败次数'),
                                icon: 'circle',
                            },
                        ],
                        bottom: '0',
                        icon: 'rect',
                        itemWidth: 8,
                        itemHeight: 8,
                        itemGap: 40,
                    },
                    tooltip: {
                        trigger: 'axis',
                        axisPointer: {
                            type: 'line',
                            lineStyle: {
                                width: 30,
                                color: '#F5F6FA',
                                opacity: 0.5,
                            },
                        },
                        formatter: tooltipFormatter,
                        backgroundColor: 'rgba(0,0,0,0.8)',
                    },
                    grid: {
                        top: 45,
                        left: 40,
                        right: 0,
                        bottom: 48,
                        containLabel: true,
                    },
                    xAxis: [
                        {
                            type: 'category',
                            axisLine: {
                                lineStyle: {
                                    color: '#DCDEE5',
                                },
                            },
                            axisTick: {
                                show: false,
                            },
                            axisLabel: {
                                color: '#979BA5',
                                margin: 18,
                            },
                            data: dateList,
                        },
                    ],
                    yAxis: {
                        type: 'value',
                        name: I18n.t('dashboard.执行数'),
                        nameLocation: 'middle',
                        nameTextStyle: {
                            color: '#63656E',
                        },
                        nameGap: 50,
                        axisLine: {
                            show: false,
                        },
                        axisTick: {
                            show: false,
                        },
                        axisLabel: {
                            color: '#979BA5',
                            formatter: value => formatNumber(value, true),
                        },
                        splitLine: {
                            lineStyle: {
                                color: '#F0F1F5',
                            },
                        },
                    },
                    series: [
                        {
                            name: 'Bat',
                            type: 'bar',
                            stack: 'bar',
                            barWidth: 20,
                            data: BatList,
                            itemStyle: {
                                color: '#2E2E99',
                            },
                        },
                        {
                            name: 'Perl',
                            type: 'bar',
                            stack: 'bar',
                            barWidth: 20,
                            data: PerlList,
                            itemStyle: {
                                color: '#366FA8',
                            },
                        },
                        {
                            name: 'Python',
                            type: 'bar',
                            stack: 'bar',
                            barWidth: 20,
                            data: PythonList,
                            itemStyle: {
                                color: '#5EADAD',
                            },
                        },
                        {
                            name: 'Shell',
                            type: 'bar',
                            stack: 'bar',
                            barWidth: 20,
                            data: ShellList,
                            itemStyle: {
                                color: '#85CCA8',
                            },
                        },
                        {
                            name: 'PowerShell',
                            type: 'bar',
                            stack: 'bar',
                            barWidth: 20,
                            data: PowerShellList,
                            itemStyle: {
                                color: '#BCEBBC',
                            },
                        },
                        {
                            name: 'SQL',
                            type: 'bar',
                            stack: 'bar',
                            barWidth: 20,
                            data: SQLList,
                            itemStyle: {
                                color: '#E7F5D7',
                            },
                        },
                        {
                            name: I18n.t('dashboard.执行失败次数'),
                            type: 'line',
                            data: failList,
                            itemStyle: {
                                color: '#FF5656',
                            },
                        },
                    ],
                });
            },
            /**
             * @desc 任务类型快速分发文件下钻统计
             * @param {Array} data 数据
             *
             */
            initExecutedFastFileDrillDown (data) {
                const dateList = [];
                const forceList = [];
                const strictList = [];
                const failList = [];
                data.forEach((item) => {
                    const {
                        date,
                        distribution,
                        failCount,
                    } = item;
                    dateList.push(date);
                    const {
                        FORCE,
                        STRICT,
                    } = distribution.labelAmountMap;

                    strictList.push(STRICT);
                    forceList.push(FORCE);
                    failList.push(failCount);
                });

                this.myChart.clear();
                this.myChart.setOption({
                    ...chartsOptionsBase,
                    legend: {
                        data: [
                            I18n.t('dashboard.强制模式'),
                            I18n.t('dashboard.严谨模式'),
                            {
                                name: I18n.t('dashboard.执行失败次数'),
                                icon: 'circle',
                            },
                        ],
                        bottom: '0',
                        icon: 'rect',
                        itemWidth: 8,
                        itemHeight: 8,
                        itemGap: 40,
                    },
                    tooltip: {
                        trigger: 'axis',
                        axisPointer: {
                            type: 'line',
                            lineStyle: {
                                width: 30,
                                color: '#F5F6FA',
                                opacity: 0.5,
                            },
                        },
                        formatter: tooltipFormatter,
                        backgroundColor: 'rgba(0,0,0,0.8)',
                    },
                    grid: {
                        top: 45,
                        left: 40,
                        right: 0,
                        bottom: 48,
                        containLabel: true,
                    },
                    xAxis: [
                        {
                            type: 'category',
                            axisLine: {
                                lineStyle: {
                                    color: '#DCDEE5',
                                },
                            },
                            axisTick: {
                                show: false,
                            },
                            axisLabel: {
                                color: '#979BA5',
                                margin: 18,
                            },
                            data: dateList,
                        },
                    ],
                    yAxis: {
                        type: 'value',
                        name: I18n.t('dashboard.执行数'),
                        nameLocation: 'middle',
                        nameTextStyle: {
                            color: '#63656E',
                        },
                        nameGap: 50,
                        axisLine: {
                            show: false,
                        },
                        axisTick: {
                            show: false,
                        },
                        axisLabel: {
                            color: '#979BA5',
                            formatter: value => formatNumber(value, true),
                        },
                        splitLine: {
                            lineStyle: {
                                color: '#F0F1F5',
                            },
                        },
                    },
                    series: [
                        {
                            name: I18n.t('dashboard.强制模式'),
                            type: 'bar',
                            stack: 'bar',
                            barWidth: 20,
                            data: forceList,
                            itemStyle: {
                                color: '#D4E6C1',
                            },
                        },
                        {
                            name: I18n.t('dashboard.严谨模式'),
                            type: 'bar',
                            stack: 'bar',
                            barWidth: 20,
                            data: strictList,
                            itemStyle: {
                                color: '#85CCA8',
                            },
                        },
                        {
                            name: I18n.t('dashboard.执行失败次数'),
                            type: 'line',
                            data: failList,
                            itemStyle: {
                                color: '#FF5656',
                            },
                        },
                    ],
                });
            },
            /**
             * @desc 指标纬度
             * @param {String} dimension 统计纬度
             *
             */
            handleDimensionChange (dimension) {
                this.isLoading = true;
                this.dimension = dimension;
                const actionMap = {
                    TASK_STARTUP_MODE: this.initTaskStartupMode,
                    TASK_TYPE: this.initTaskType,
                    TASK_TIME_CONSUMING: this.initTaskTimeConsuming,
                };
                StatisticsService.fetchReourcesDimensions({
                    resource: 'EXECUTED_TASK',
                    dimension: this.dimension,
                    startDate: prettyDateFormat(new Date(this.date) - parseInt(this.days, 10) * 86400000),
                    endDate: this.date,
                }).then((data) => {
                    actionMap[this.dimension](data);
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 时间范围
             * @param {String} days 统计纬度
             *
             */
            handleDaysChange (days) {
                this.days = days;
                if (this.isDrillDown) {
                    this.handleTaskTypeChange(this.drillDowntaskType);
                } else {
                    this.handleDimensionChange(this.dimension);
                }
            },
            /**
             * @desc 按类型统计，指标下钻
             * @param {String} taskType 作业类型
             *
             */
            handleTaskTypeChange (taskType) {
                this.isLoading = true;
                this.isDrillDown = true;
                this.drillDowntaskType = taskType;
                const dateParams = {
                    startDate: prettyDateFormat(new Date(this.date) - parseInt(this.days, 10) * 86400000),
                    endDate: this.date,
                };
                if (taskType === 'EXECUTED_FAST_SCRIPT') {
                    StatisticsService.fetchReourcesDimensions({
                        ...dateParams,
                        dimension: 'SCRIPT_TYPE',
                        resource: 'EXECUTED_FAST_SCRIPT',
                    }).then((data) => {
                        this.initExecutedFastScriptDrillDown(data);
                    })
                        .finally(() => {
                            this.isLoading = false;
                        });
                } else {
                    StatisticsService.fetchReourcesDimensions({
                        ...dateParams,
                        dimension: 'FILE_TRANSFER_MODE',
                        resource: 'EXECUTED_FAST_FILE',
                    }).then((data) => {
                        this.initExecutedFastFileDrillDown(data);
                    })
                        .finally(() => {
                            this.isLoading = false;
                        });
                }
            },
            /**
             * @desc 返回按类型统计
             *
             */
            handleDimensionBack () {
                this.isDrillDown = false;
                this.drillDowntaskType = '';
                this.handleDimensionChange(this.dimension);
            },
        },
    };
</script>
<style lang='postcss' scoped>
    .execute-statistics-dashboard {
        padding: 20px;

        .header-action {
            display: flex;
            font-size: 12px;
            color: #63656e;

            .type-tab,
            .time-tab {
                display: flex;

                .tab-item {
                    height: 28px;
                    padding: 0 16px;
                    line-height: 28px;
                    cursor: pointer;
                    border-radius: 14px;
                    transition: all 0.15s;

                    &.active,
                    &:hover {
                        color: #3a84ff;
                    }

                    &.active {
                        background: #ebf2ff;
                    }
                }
            }

            .type-tab {
                .tab-item {
                    margin-right: 8px;
                }
            }

            .time-tab {
                margin-left: auto;

                .tab-item {
                    margin-left: 8px;
                }
            }

            .tab-back {
                color: #3a84ff;

                .tab-back-action {
                    cursor: pointer;
                }
            }
        }
    }
</style>

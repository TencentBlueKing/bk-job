<!--
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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
  <card-layout
    class="execute-rolling-dashboard"
    :title="$t('dashboard.滚动执行')">
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
    <div v-bkloading="{ isLoading, opacity: 0.8 }">
      <div
        ref="dashboard"
        style="width: 100%; height: 325px;" />
    </div>
  </card-layout>
</template>
<script>
/* eslint-disable max-len */
  import * as echarts from 'echarts';
  import _ from 'lodash';

  import StatisticsService from '@service/statistics';

  import {
    formatNumber,
    prettyDateFormat,
  } from '@utils/assist';

  import I18n from '@/i18n';

  import CardLayout from '../card-layout';
  import {
    chartsOptionsBase,
  } from '../common/assist';

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
            <td style="padding-right: 24px; color: #fff; vertical-align: middle;">
              <span style="display: inline-block; width: 8px; height: 8px; background: ${color}"></span>
              <span>${seriesName}</span>
            </td>
            <td style="text-align: right">${value}</td>
          </tr>
        `;
      } if (seriesType === 'line') {
        return `
          <tr>
            <td style="padding-right: 24px; color: #fff; vertical-align: middle;">
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
    components: {
      CardLayout,
    },
    props: {
      date: {
        type: String,
        required: true,
      },
    },
    data() {
      return {
        isLoading: false,
        dimension: '',
        days: 7,
        isDrillDown: false,
      };
    },
    watch: {
      date() {
        this.handleDaysChange(this.days);
      },
    },
    mounted() {
      // 下钻的作业类型
      this.drillDowntaskType = '';

      this.myChart = echarts.init(this.$refs.dashboard);

      const resize = _.throttle(() => {
        this.myChart.resize();
      }, 300);
      window.addEventListener('resize', resize);
      this.$once('hook:beforeDestroy', () => {
        window.removeEventListener('resize', resize);
      });

      this.fetchData();
    },
    methods: {
      fetchData() {
        this.isLoading = true;
        StatisticsService.fetchReourcesDimensions({
          resource: 'EXECUTED_ROLLING_TASK',
          dimension: 'TASK_TYPE',
          startDate: prettyDateFormat(new Date(this.date) - parseInt(this.days, 10) * 86400000),
          endDate: this.date,
        }).then((data) => {
          this.initTaskType(data);
        })
          .finally(() => {
            this.isLoading = false;
          });
      },
      /**
       * @desc 按任务类型统计
       * @param {Array} data 数据
       */
      initTaskType(data) {
        const dateList = [];
        const fastPushFileList = [];
        const fastExecuteScriptList = [];
        const executeTaskList = [];
        data.forEach((item) => {
          const {
            date,
            distribution,
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
        });

        this.myChart.clear();
        this.myChart.setOption({
          ...chartsOptionsBase,
          legend: {
            data: [
              I18n.t('dashboard.快速执行脚本'),
              I18n.t('dashboard.快速分发文件'),
              I18n.t('dashboard.作业执行'),
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
            borderColor: 'transparent',
            textStyle: {
              color: '#fff',
            },
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
            nameGap: 40,
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
          ],
        });
      },
      /**
       * @desc 时间范围
       * @param {String} days 统计纬度
       *
       */
      handleDaysChange(days) {
        this.days = days;
        this.fetchData();
      },
    },
  };
</script>
<style lang="postcss">
  .execute-rolling-dashboard {
    position: relative;
    width: 100%;

    .time-tab {
      position: absolute;
      top: 20px;
      right: 20px;
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
  }
</style>

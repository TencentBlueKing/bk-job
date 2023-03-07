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
  <card-layout
    v-bkloading="{ isLoading, opacity: 0.8 }"
    class="template-step-card"
    :title="$t('dashboard.作业步骤类型使用占比')">
    <div class="wraper">
      <div
        ref="dashboard"
        style="width: 180px; height: 180px;" />
      <div class="data-info">
        <div class="row">
          <div
            class="data-label"
            @mouseover="handleMouseover($t('dashboard.文件分发'))">
            <div :style="calcItemCircleStyles('fileStep')" />
            <div>{{ $t('dashboard.文件分发') }}</div>
          </div>
          <div class="data-content">
            <div
              class="content-item"
              @mouseover="handleMouseover($t('dashboard.本地文件源'))">
              <div class="local-file-dot" />
              <div>{{ $t('dashboard.本地文件源') }}</div>
            </div>
            <div
              class="content-item"
              @mouseover="handleMouseover($t('dashboard.服务器文件源'))">
              <div class="server-file-dot" />
              <div>{{ $t('dashboard.服务器文件源') }}</div>
            </div>
          </div>
        </div>
        <div class="row">
          <div
            class="data-label"
            @mouseover="handleMouseover($t('dashboard.脚本执行'))">
            <div :style="calcItemCircleStyles('scriptStep')" />
            <div>{{ $t('dashboard.脚本执行') }}</div>
          </div>
          <div class="data-content">
            <div
              class="content-item"
              @mouseover="handleMouseover($t('dashboard.手工录入'))">
              <div class="local-script-dot" />
              <div>{{ $t('dashboard.手工录入') }}</div>
            </div>
            <div
              class="content-item"
              @mouseover="handleMouseover($t('dashboard.脚本引用'))">
              <div class="refer-script-dot" />
              <div>{{ $t('dashboard.脚本引用') }}</div>
            </div>
          </div>
        </div>
        <div class="row">
          <div
            class="data-label"
            @mouseover="handleMouseover($t('dashboard.人工确认'))">
            <div :style="calcItemCircleStyles('confirmStep')" />
            <div>{{ $t('dashboard.人工确认') }}</div>
          </div>
        </div>
      </div>
    </div>
  </card-layout>
</template>
<script>
  import echarts from 'lib/echarts.min.js';
  import _ from 'lodash';

  import StatisticsService from '@service/statistics';

  import CardLayout from '../card-layout';
  import {
    chartsOptionsBase,
  } from '../common/assist';

  import I18n from '@/i18n';

  const colorMap = {
    fileStep: '#3157A3',
    scriptStep: '#85CCA8',
    confirmStep: '#D4E6C1',
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
        isLoading: true,
        data: {
          CONFIRM: 32,
          FILE_LOCAL: 68,
          FILE_SERVER: 32,
          SCRIPT_MANUAL: 32,
          SCRIPT_REF: 32,
        },
      };
    },
    watch: {
      date() {
        this.fetchData();
      },
    },
    created() {
      this.list = [
        I18n.t('dashboard.文件分发'),
        I18n.t('dashboard.本地文件源'),
        I18n.t('dashboard.服务器文件源'),
        I18n.t('dashboard.脚本执行'),
        I18n.t('dashboard.手工录入'),
        I18n.t('dashboard.脚本引用'),
        I18n.t('dashboard.人工确认'),
      ];
      this.fetchData();
    },
    methods: {
      fetchData() {
        this.isLoading = true;
        StatisticsService.fetchDistributionMetrics({
          date: this.date,
          metric: 'STEP_TYPE',
        }).then((data) => {
          this.data = data.labelAmountMap;
          this.init();
        })
          .finally(() => {
            this.isLoading = false;
          });
      },
      init() {
        this.myChart = echarts.init(this.$refs.dashboard);
        const option = {
          ...chartsOptionsBase,
          series: [
            {
              name: I18n.t('dashboard.作业步骤'),
              type: 'pie',
              label: {
                show: false,
              },
              selectedMode: 'single',
              radius: [
                0,
                '50px',
              ],
              hoverOffset: 6,
              avoidLabelOverlap: false,
              data: [
                {
                  value: this.data.FILE_LOCAL + this.data.FILE_SERVER,
                  name: I18n.t('dashboard.文件分发'),
                  itemStyle: {
                    color: colorMap.fileStep,
                  },
                },
                {
                  value: this.data.SCRIPT_MANUAL + this.data.SCRIPT_REF,
                  name: I18n.t('dashboard.脚本执行'),
                  itemStyle: {
                    color: colorMap.scriptStep,
                  },
                },
                {
                  value: this.data.CONFIRM,
                  name: I18n.t('dashboard.人工确认'),
                  itemStyle: {
                    color: colorMap.confirmStep,
                  },
                },
              ],
            },
            {
              name: I18n.t('dashboard.作业步骤'),
              type: 'pie',
              label: {
                show: false,
              },
              radius: [
                '60px',
                '80px',
              ],
              hoverOffset: 8,
              avoidLabelOverlap: false,
              data: [
                {
                  value: this.data.FILE_LOCAL,
                  name: I18n.t('dashboard.本地文件源'),
                  itemStyle: {
                    color: '#2E2E99',
                  },
                },
                {
                  value: this.data.FILE_SERVER,
                  name: I18n.t('dashboard.服务器文件源'),
                  itemStyle: {
                    color: '#3786AD',
                  },
                },
                {
                  value: this.data.SCRIPT_MANUAL,
                  name: I18n.t('dashboard.手工录入'),
                  itemStyle: {
                    color: '#74C2C2',
                  },
                },
                {
                  value: this.data.SCRIPT_REF,
                  name: I18n.t('dashboard.脚本引用'),
                  itemStyle: {
                    color: '#9AD69A',
                  },
                },
                {
                  value: this.data.CONFIRM,
                  name: I18n.t('dashboard.人工确认'),
                  itemStyle: {
                    color: '#D4E6C1',
                  },
                },
              ],
            },
          ],
        };
        this.myChart.setOption(option);
        let maxValueItem = {
          value: -1,
        };
        option.series.forEach((series) => {
          series.data.forEach((item) => {
            if (maxValueItem.value < item.value) {
              maxValueItem = item;
            }
          });
        });
        this.myChart.dispatchAction({ type: 'highlight', name: maxValueItem.name });
        this.myChart.on('mouseover', (params) => {
          this.handleMouseover(params.data.name);
        });
      },
      calcItemCircleStyles(type) {
        return {
          width: '8px',
          height: '8px',
          marginRight: '6px',
          borderRadius: '50%',
          backgroundColor: colorMap[type],
        };
      },
      handleMouseover(label) {
        const others = _.filter(this.list, _ => _ !== label);
        this.myChart.dispatchAction({ type: 'highlight', name: label });
        this.myChart.dispatchAction({ type: 'downplay', name: others });
      },
    },
  };
</script>
<style lang='postcss' scoped>
  .template-step-card {
    width: 260px;

    .wraper {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
    }

    .data-info {
      display: flex;
      flex-direction: column;
      font-size: 12px;
      line-height: 18px;
      color: #979ba5;
      white-space: nowrap;

      .row {
        display: flex;
        align-items: flex-start;
        margin-top: 6px;
      }

      .data-label {
        display: flex;
        align-items: center;
        padding-right: 14px;
        cursor: pointer;
      }

      .data-content {
        position: relative;
        padding-left: 14px;

        &::before {
          position: absolute;
          top: 9px;
          left: -14px;
          width: 14px;
          height: 1px;
          background: #f0f1f5;
          content: "";
        }
      }

      .content-item {
        position: relative;
        display: flex;
        align-items: center;
        cursor: pointer;

        &:nth-child(n+2) {
          margin-top: 4px;

          &::after {
            position: absolute;
            top: -13px;
            left: -14px;
            width: 1px;
            height: 22px;
            background: #f0f1f5;
            content: "";
          }
        }

        &::before {
          position: absolute;
          top: 9px;
          left: -14px;
          width: 14px;
          height: 1px;
          background: #f0f1f5;
          content: "";
        }
      }

      .local-file-dot,
      .server-file-dot,
      .local-script-dot,
      .refer-script-dot {
        width: 8px;
        height: 8px;
        margin-right: 6px;
        border-radius: 50%;
      }

      .local-file-dot {
        background: rgb(35 35 148 / 40%);
        border: 2px solid #232394;
      }

      .server-file-dot {
        background: rgb(55 134 173 / 40%);
        border: 2px solid #3786ad;
      }

      .local-script-dot {
        background: rgb(116 194 194 / 40%);
        border: 2px solid #74c2c2;
      }

      .refer-script-dot {
        background: rgb(154 214 154 / 40%);
        border: 2px solid #9ad69a;
      }
    }
  }
</style>

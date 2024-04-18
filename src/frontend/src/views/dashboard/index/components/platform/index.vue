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
    v-bkloading="{ isLoading: isLoading, opacity: 0.8 }"
    class="platform-dashboard">
    <template v-for="item in systemList">
      <card-layout
        :key="item.key"
        :title="item.name">
        <div class="container">
          <div class="nums">
            {{ data[item.key] | formatNumber }}
          </div>
          <div
            :ref="item.key"
            v-bk-tooltips.right="calcPercentage(data[item.key])"
            class="dashboard"
            style="width: 24px; height: 24px;" />
        </div>
        <icon
          class="platform-flag"
          style="font-size: 38px;"
          :type="item.icon" />
      </card-layout>
    </template>
  </div>
</template>
<script>
  import * as echarts from 'echarts';
  import _ from 'lodash';

  import StatisticsService from '@service/statistics';

  import {
    formatNumber,
  } from '@utils/assist';

  import I18n from '@/i18n';

  import CardLayout from '../card-layout';

  const getOtherTotal = (data, curKey) => {
    const other = { ...data };
    delete other[curKey];
    return _.sum(Object.values(other));
  };

  export default {
    name: '',
    components: {
      CardLayout,
    },
    filters: {
      formatNumber(value) {
        return formatNumber(value);
      },
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
          AIX: 0,
          FREEBSD: 0,
          LINUX: 0,
          OTHERS: 0,
          SOLARIS: 0,
          UNIX: 0,
          WINDOWS: 0,
          MACOS: 0,
        },
      };
    },
    watch: {
      date() {
        this.fetchData();
      },
    },
    created() {
      this.systemList = [
        {
          name: I18n.t('dashboard.Linux 数量'),
          key: 'LINUX',
          icon: 'os-linux',
        },
        {
          name: I18n.t('dashboard.Windows 数量'),
          key: 'WINDOWS',
          icon: 'os-win',
        },
        {
          name: I18n.t('dashboard.AIX 数量'),
          key: 'AIX',
          icon: 'os-aix',
        },
        {
          name: I18n.t('dashboard.UNIX 数量'),
          key: 'UNIX',
          icon: 'os-unix',
        },
        {
          name: I18n.t('dashboard.Solaris 数量'),
          key: 'SOLARIS',
          icon: 'os-solaris',
        },
        {
          name: I18n.t('dashboard.Free BSD 数量'),
          key: 'FREEBSD',
          icon: 'os-freebsd',
        },
        {
          name: I18n.t('dashboard.MacOS 数量'),
          key: 'MACOS',
          icon: 'os-macos',
        },
        {
          name: I18n.t('dashboard.未知数量'),
          key: 'OTHERS',
          icon: 'os-unknown',
        },
      ];
    },
    mounted() {
      this.fetchData();
    },
    methods: {
      fetchData() {
        this.isLoading = true;
        StatisticsService.fetchDistributionMetrics({
          date: this.date,
          metric: 'HOST_SYSTEM_TYPE',
        }).then((data) => {
          this.data = Object.assign({}, this.data, data.labelAmountMap);
          this.init();
        })
          .finally(() => {
            this.isLoading = false;
          });
      },
      init() {
        this.systemList.forEach((systemItem) => {
          if (!this.$refs.LINUX) {
            return;
          }

          const other = getOtherTotal(this.data, systemItem.key);
          if (!this.$refs[systemItem.key]) {
            return;
          }
          const myChart = echarts.init(this.$refs[systemItem.key][0]);
          myChart.setOption({
            series: [
              {
                type: 'pie',
                radius: [
                  '8',
                  '12',
                ],
                hoverOffset: 0,
                label: {
                  show: false,
                },
                data: [
                  {
                    value: this.data[systemItem.key],
                    itemStyle: {
                      color: '#85CCA8',
                    },
                  },
                  {
                    value: other,
                    itemStyle: {
                      color: '#EBECF0',
                    },
                    emphasis: {
                      itemStyle: {
                        color: '#F0F1F5',
                      },
                    },
                  },
                ],
              },
            ],
          });
        });
      },
      calcPercentage(value) {
        const total = _.sum(Object.values(this.data));
        if (!total) {
          return '0 %';
        }
        return `${(value / total * 100).toFixed(2)} %`;
      },
    },
  };
</script>
<style lang='postcss'>
  .platform-dashboard {
    display: flex;
    height: 100%;
    color: #e5e6eb;
    flex-wrap: wrap;

    .dashboard-card-layout {
      position: relative;
      padding: 12px 16px;
      border: none;
      border-bottom: 1px solid #f0f1f5;
      flex: 0 0 25%;

      &:nth-child(n+2) {
        border-left: 1px solid #f0f1f5;
      }

      &:nth-child(n+5) {
        border-bottom-color: transparent;
      }

      .card-title {
        margin-bottom: 12px;
        color: #979ba5;
      }
    }

    .container {
      display: flex;
      align-items: center;

      .dashboard {
        margin-left: 10px;
      }
    }

    .nums {
      font-size: 24px;
      font-weight: bold;
      color: #63656e;
    }

    .platform-flag {
      position: absolute;
      top: 14px;
      right: 12px;
    }
  }
</style>

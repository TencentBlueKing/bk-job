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
    <div class="service-state-info" v-bkloading="{ isLoading }">
        <bk-table
            :data="serviceData"
            :size="tableSize"
            row-key="name"
            ext-cls="service-table"
            :expand-row-keys="expandRow"
            @row-click="toggleRowExpansion">
            <bk-table-column
                type="expand"
                key="expand"
                :before-expand-change="onBeforeExpandChange">
                <template slot-scope="{ row }">
                    <bk-table
                        ext-cls="service-expand-table"
                        :data="row.instanceList"
                        :outer-border="false"
                        :header-border="false">
                        <bk-table-column
                            key="index"
                            width="60"
                            :label="$t('service.序号.colHead')">
                            <template slot-scope="{ $index }">
                                #{{ $index + 1 }}
                            </template>
                        </bk-table-column>
                        <bk-table-column
                            prop="name"
                            key="name"
                            width="420"
                            :label="$t('service.实例名.colHead')">
                            <template slot-scope="scope">
                                {{ scope.row.name }}
                            </template>
                        </bk-table-column>
                        <bk-table-column
                            prop="version"
                            key="version"
                            width="310"
                            :label="$t('service.版本号.label')" />
                        <bk-table-column
                            prop="status"
                            key="status"
                            :label="$t('service.状态.colHead')">
                            <template slot-scope="scope">
                                <Icon :type="statusIcon(scope.row)" svg />
                                <span v-html="statusHtml(scope.row)" />
                            </template>
                        </bk-table-column>
                        <bk-table-column
                            prop="ip"
                            key="ip"
                            width="150"
                            :label="$t('service.绑定IP.colHead')">
                            <template slot-scope="scope">
                                <span class="service-ip" @click="handleCopyIp(scope.row.ip)">
                                    {{ scope.row.ip }}
                                    <Icon class="ml5 copy-ip-icon" type="step-copy" svg />
                                    <!-- <Icon class="skip-icon" type="edit" svg /> -->
                                </span>
                            </template>
                        </bk-table-column>
                        <bk-table-column
                            prop="port"
                            key="port"
                            width="100"
                            :label="$t('service.端口.colHead')" />
                    </bk-table>
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('service.服务名.colHead')"
                prop="name"
                width="480"
                key="name"
                align="left" />
            <bk-table-column
                :label="$t('service.版本号.colHead')"
                prop="version"
                width="310"
                key="version"
                align="left">
                <template slot-scope="{ row }">
                    <span v-html="row.versionHtml" />
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('service.实例状态.colHead')"
                prop="instanceList"
                key="instanceList"
                align="left">
                <template slot-scope="{ row }">
                    <div v-bk-tooltips="instanceTips(row)">
                        <span class="service-instance-num abnormal" v-if="row.abnormalNum">{{ row.abnormalNum }}</span>
                        <span class="service-instance-num unknown" v-if="row.unknownNum">{{ row.unknownNum }}</span>
                        <span class="service-instance-num normal" v-if="row.normalNum">{{ row.normalNum }}</span>
                    </div>
                </template>
            </bk-table-column>
        </bk-table>
    </div>
</template>
 
<script>
    import I18n from '@/i18n';
    import ServiceStateService from '@service/service-state';
    import {
        execCopy,
    } from '@utils/assist';

    export default {
        name: 'Service',
        data () {
            return {
                isLoading: false,
                tableSize: 'small',
                serviceData: [],
                expandRow: [],
                timer: null,
                showFirstExpandRow: true,
            };
        },
        computed: {
            isSkeletonLoading () {
                return this.isLoading;
            },
        },
        created () {
            this.fetchData();
        },
        destroyed () {
            clearInterval(this.timer);
        },
        methods: {
            /**
             * @desc 获取服务运行状态数据,每三秒轮询一次
             */
            fetchData () {
                ServiceStateService.serviceList().then((data) => {
                    this.serviceData = data.data;
                    // 折叠表格默认展开第一个
                    if (this.showFirstExpandRow) {
                        this.expandRow.push(this.serviceData[0].name);
                    }
                    this.serviceData.forEach((service) => {
                        let abnormalNum = 0;
                        let normalNum = 0;
                        let unknownNum = 0;
                        service.instanceList.forEach((instance) => {
                            if (instance.status === 1) {
                                normalNum += 1;
                            } else if (instance.status === -1) {
                                unknownNum += 1;
                            } else if (instance.status === 0) {
                                abnormalNum += 1;
                            }
                        });
                        const statusMap = {
                            abnormalNum,
                            normalNum,
                            unknownNum,
                        };
                        Object.assign(service, statusMap);
                        this.showFirstExpandRow = false;
                        clearInterval(this.timer);
                        this.timer = setInterval(() => {
                            this.fetchData();
                        }, 3000);
                    });
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },

            /**
             * @desc 控制折叠表格只能展开一项
             * @param {Object} row 表格当前行数据
             */
            toggleRowExpansion (row) {
                if (this.expandRow.includes(row.name)) {
                    this.expandRow = [];
                } else {
                    this.expandRow = [
                        row.name,
                    ];
                }
            },

            /**
             * @desc 控制折叠表格只能展开一项
             * @param {Object} row 表格当前行数据
             */
            onBeforeExpandChange ({ row }) {
                this.toggleRowExpansion(row);
            },

            /**
             * @desc 自定义表格状态内容
             */
            statusHtml (row) {
                const styles = 'color: #aaacb5';
                const statusHtmlMap = {
                    0: `<span>${I18n.t('异常')}<span style="${styles}"> #SERVICE UNAVAILABLE (503)</span></span>`,
                    1: `<span>${I18n.t('正常')}</span>`,
                    '-1': `<span>${I18n.t('未知')}<span style="${styles}"> #NO MAPPING</span></span>`,
                };
                return statusHtmlMap[row.status];
            },

            /**
             * @desc 自定义表格状态图标
             */
            statusIcon (row) {
                const statusIcomMap = {
                    0: 'abnormal',
                    1: 'normal',
                    '-1': 'unknown',
                };
                return statusIcomMap[row.status];
            },

            /**
             * @desc 自定义实例状态tooltips内容
             */
            instanceTips (row) {
                let tipsStr = '';
                if (row.abnormalNum) {
                    tipsStr = `${I18n.t('异常')}: ${row.abnormalNum}`;
                }
                if (row.unknownNum) {
                    tipsStr += ` ${I18n.t('未知')}: ${row.unknownNum}`;
                }
                if (row.normalNum) {
                    tipsStr += ` ${I18n.t('正常')}: ${row.normalNum}`;
                }
                return { content: tipsStr, placement: 'top' };
            },

            /**
             * @desc 复制IP
             */
            handleCopyIp (ip) {
                execCopy(ip, `${I18n.t('复制成功')}`);
            },
        },
    };
</script>

<style lang="postcss">
    .service-state-info {
        .service-table > .bk-table-body-wrapper > table > tbody > .bk-table-row {
            cursor: pointer;
        }

        .service-expand-table {
            background-color: #fafbfd;

            tr {
                background-color: #fafbfd;
            }
        }

        .bk-table .bk-table-body td.bk-table-expanded-cell {
            padding: 0 48px;
            background-color: #fafbfd;
        }

        .service-ip {
            display: flex;
            align-items: center;
            cursor: pointer;

            &:hover {
                .copy-ip-icon {
                    color: #3a84ff;
                }
            }
        }

        .copy-ip-icon {
            color: #979ba5;
        }

        .service-instance-num {
            display: inline-block;
            width: 20px;
            height: 20px;
            font-size: 12px;
            font-weight: 700;
            line-height: 18px;
            text-align: center;
            border: 1px solid #fff;
        }

        .unknown {
            color: #979ba5;
            background-color: #e6e8f0;
        }

        .abnormal {
            color: #ea3536;
            background-color: #fdd;
        }

        .normal {
            color: #14a568;
            background-color: #dff0e4;
        }

        .expanded {
            background-color: #f0f1f5;
        }

        .bk-table-expand-icon-expanded {
            color: #979ba5;
        }
    }
</style>

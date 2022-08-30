<template>
    <div
        class="ip-selector-view-host"
        v-bkloading="{ isLoading }">
        <collapse-box>
            <template #title>
                <span style="font-weight: bold;">【静态拓扑】</span>
                <span>
                    <span>
                        - 共
                        <span class="bk-ip-selector-number">{{ tableData.length }}</span>
                        台
                    </span>
                    <span v-if="newHostNum">
                        ，新增
                        <span class="bk-ip-selector-number-success">{{ newHostNum }}</span>
                        台
                    </span>
                    <span v-if="removedHostList.length">
                        ，删除
                        <span class="bk-ip-selector-number-error">{{ removedHostList.length }}</span>
                        台
                    </span>
                </span>
            </template>
            <template #action>
                <extend-action>
                    <div @click="handleCopyAllIP">复制所有 IP</div>
                    <div>复制异常 IP</div>
                    <div @click="handleRemoveAll">清除所有</div>
                    <div>清除异常 IP</div>
                </extend-action>
            </template>
            <!-- <table>
                <thead>
                    <tr>
                        <th style="width: 12%;">IP</th>
                        <th style="width: 18%;">IPv6</th>
                        <th>主机名称</th>
                        <th style="width: 120px;">Agent 状态</th>
                        <th>云区域</th>
                        <th>系统</th>
                        <th style="width: 100px;" />
                    </tr>
                </thead>
                <tbody>
                    <tr
                        v-for="(item, index) in renderData"
                        :key="index"
                        :class="diffMap[item.hostId]">
                        <td>
                            <div class="cell">
                                {{ item.ip }}
                                <span v-if="hostIpRepeatMap[item.hostId]">
                                    {{ item.hostId }}
                                </span>
                                <diff-tag :value="diffMap[item.hostId]" />
                            </div>
                        </td>
                        <td>
                            <div class="cell">
                                {{ item.ipv6 || '--' }}
                            </div>
                        </td>
                        <td>
                            <div class="cell">
                                {{ item.ipDesc || '--' }}
                            </div>
                        </td>
                        <td>
                            <div class="cell">
                                <agent-status :data="item.agentStatus" />
                            </div>
                        </td>
                        <td>
                            <div class="cell">
                                {{ item.cloudArea.name || '--' }}
                            </div>
                        </td>
                        <td>
                            <div class="cell">
                                {{ item.osName || '--' }}
                            </div>
                        </td>
                        <td>
                            <bk-button
                                text
                                theme="primary"
                                @click="handleRemove(item)">
                                删除
                            </bk-button>
                        </td>
                    </tr>
                </tbody>
            </table> -->
            <render-host-table
                :data="renderData"
                :show-setting="false"
                :column-width-callback="columnWidthCallback">
                <template #[hostRenderKey]="{ row }">
                    <diff-tag :value="diffMap[row.hostId]" />
                </template>
                <template #action="{ row }">
                    <bk-button
                        text
                        theme="primary"
                        @click="handleRemove(row)">
                        删除
                    </bk-button>
                </template>
            </render-host-table>
            <div
                v-if="isShowPagination"
                style="padding: 0 10px 8px 0;">
                <bk-pagination
                    style="margin-top: 8px;"
                    v-bind="pagination"
                    @change="handlePaginationCurrentChange"
                    @limit-change="handlePaginationLimitChange" />
            </div>
        </collapse-box>
    </div>
</template>
<script setup>
    import {
        ref,
        watch,
        shallowRef,
    } from 'vue';
    import AppManageService from '@service/app-manage';
    import useLocalPagination from '../hooks/use-local-pagination';
    import useIpSelector from '../hooks/use-ip-selector';
    import useHostRenderKey from '../hooks/use-host-render-key';
    import ExtendAction from '../common/extend-action.vue';
    import DiffTag from '../common/diff-tag.vue';
    import RenderHostTable from '../common/render-table/host.vue';
    import {
        execCopy,
        getInvalidHostList,
        getRemoveHostList,
        getHostDiffMap,
        getDiffNewNum,
        getRepeatIpHostMap,
        groupHostList,
    } from '../utils';
    import CollapseBox from './components/collapse-box/index.vue';

    const props = defineProps({
        data: {
            type: Array,
            default: () => [],
        },
    });

    const emits = defineEmits([
        'change',
    ]);

    const isLoading = ref(false);
    const tableData = shallowRef([]);
    const diffMap = shallowRef({});
    const hostIpRepeatMap = shallowRef({});
    const validHostList = shallowRef([]);
    const removedHostList = shallowRef([]);
    const invalidHostList = shallowRef([]);
    const resultList = shallowRef([]);

    const newHostNum = ref(0);

    let isInnerChange = false;

    const context = useIpSelector();
    const {
        key: hostRenderKey,
    } = useHostRenderKey();

    const {
        isShowPagination,
        pagination,
        data: renderData,
        handlePaginationCurrentChange,
        handlePaginationLimitChange,
    } = useLocalPagination(tableData);

    // 通过 hostId 获取主机详情
    const fetchData = () => {
        isLoading.value = true;
        AppManageService.fetchHostInfoByHostId({
            hostList: props.data,
        })
        .then((data) => {
            validHostList.value = data;
        })
        .finally(() => {
            isLoading.value = false;
        });
    };

    const columnWidthCallback = (index) => {
        if (index <= 1) {
            return '15%';
        }
        return '';
    };

    watch(() => props.data, () => {
        if (isInnerChange) {
            isInnerChange = false;
            return;
        }
        
        if (props.data.length > 0) {
            fetchData();
        } else {
            tableData.value = [];
        }
    }, {
        immediate: true,
    });

    watch([validHostList, resultList], () => {
        invalidHostList.value = getInvalidHostList(props.data, validHostList.value);
        removedHostList.value = getRemoveHostList(props.data, context.orinigalValue);
        diffMap.value = getHostDiffMap(props.data, context.orinigalValue, invalidHostList.value);
        newHostNum.value = getDiffNewNum(diffMap.value);
        hostIpRepeatMap.value = getRepeatIpHostMap(validHostList.value);

        const {
            newList,
            originalList,
        } = groupHostList(validHostList.value, diffMap.value);
        tableData.value = [
            ...invalidHostList.value,
            ...newList,
            ...removedHostList.value,
            ...originalList,
        ];
        pagination.count = tableData.value.length;
    });

    const triggerChange = () => {
        isInnerChange = true;
        emits('change', 'hostList', resultList.value);
    };

    // 删除指定主机
    const handleRemove = (hostData) => {
        resultList.value = props.data.reduce((result, item) => {
            if (item.hostId !== hostData.hostId) {
                result.push(item);
            }
            return result;
        }, []);
        if (diffMap.value[hostData.hostId] === 'new') {
            validHostList.value = validHostList.value.reduce((result, item) => {
                if (item.hostId !== hostData.hostId) {
                    result.push(item);
                }
                return result;
            }, []);
        }
        triggerChange();
    };

    // 复制所有 IP
    const handleCopyAllIP = () => {
        const IPList = tableData.value.map(item => item[hostRenderKey.value]);
        execCopy(IPList.join('\n'), `复制成功 ${IPList.length} 个 IP`);
    };
    
    // 清除所有主机
    const handleRemoveAll = () => {
        resultList.value = [];
        validHostList.value = [];
        triggerChange();
    };
</script>
<style lang="postcss">
    @import "../styles/table.mixin.css";

    .ip-selector-view-host {
        @include table;

        table {
            th {
                background: #fafbfd;

                &:hover {
                    background: #f0f1f5;
                }
            }
        }
    }
</style>

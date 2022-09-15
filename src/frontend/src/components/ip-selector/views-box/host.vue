<template>
    <div
        v-bkloading="{ isLoading }"
        class="ip-selector-view-host">
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
                    <div @click="handleCopyAllIP">
                        复制所有 IP
                    </div>
                    <div @click="handleCopyFaidedIP">
                        复制异常 IP
                    </div>
                    <template v-if="!context.readonly">
                        <div @click="handleRemoveAll">
                            清除所有
                        </div>
                        <div @click="handleRemoveFailedIP">
                            清除异常 IP
                        </div>
                    </template>
                </extend-action>
            </template>
            <render-host-table
                :column-width-callback="columnWidthCallback"
                :data="renderData"
                :show-setting="false">
                <template #[hostRenderKey]="{ row }">
                    <diff-tag :value="diffMap[row.host_id]" />
                </template>
                <template
                    v-if="!context.readonly"
                    #action="{ row }">
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
        shallowRef,
        watch,
    } from 'vue';

    import DiffTag from '../common/diff-tag.vue';
    import ExtendAction from '../common/extend-action.vue';
    import RenderHostTable from '../common/render-table/host/index.vue';
    import useHostRenderKey from '../hooks/use-host-render-key';
    import useIpSelector from '../hooks/use-ip-selector';
    import useLocalPagination from '../hooks/use-local-pagination';
    import Manager from '../manager';
    import {
        execCopy,
        getHostDiffMap,
        getInvalidHostList,
        getRemoveHostList,
        getRepeatIpHostMap,
        groupHostList,
        isAliveHost,
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

    // 通过 host_id 获取主机详情
    const fetchData = () => {
        isLoading.value = true;
        Manager.service.fetchHostsDetails({
            [Manager.nameStyle('hostList')]: props.data.map(item => ({
                [Manager.nameStyle('hostId')]: item.host_id,
                [Manager.nameStyle('meta')]: item.meta,
            })),
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
        removedHostList.value = getRemoveHostList(props.data, context.originalValue);
        diffMap.value = getHostDiffMap(props.data, context.originalValue, invalidHostList.value);
        
        hostIpRepeatMap.value = getRepeatIpHostMap(validHostList.value);

        const {
            newList,
            originalList,
        } = groupHostList(validHostList.value, diffMap.value);

        newHostNum.value = newList.length;

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
            if (item.host_id !== hostData.host_id) {
                result.push(item);
            }
            return result;
        }, []);
        if (diffMap.value[hostData.host_id] !== 'invalid') {
            validHostList.value = validHostList.value.reduce((result, item) => {
                if (item.host_id !== hostData.host_id) {
                    result.push(item);
                }
                return result;
            }, []);
        }
        triggerChange();
    };

    // 复制所有 IP
    const handleCopyAllIP = () => {
        const ipList = tableData.value.map(item => item[hostRenderKey.value]);
        execCopy(ipList.join('\n'), `复制成功 ${ipList.length} 个 IP`);
    };

    // 复制异常 IP
    const handleCopyFaidedIP = () => {
        const ipList = tableData.value.reduce((result, item) => {
            if (!isAliveHost(item)) {
                result.push(item[hostRenderKey.value]);
            }
            return result;
        }, []);
        execCopy(ipList.join('\n'), `复制成功 ${ipList.length} 个 IP`);
    };

    // 清除异常 IP
    const handleRemoveFailedIP = () => {
        const newValidHostList = [];
        const newValidHostIdMap = {};
        validHostList.value.forEach((hostData) => {
            if (isAliveHost(hostData)) {
                newValidHostList.push(hostData);
                newValidHostIdMap[hostData.host_id] = true;
            }
        });
        resultList.value = props.data.reduce((result, item) => {
            if (newValidHostIdMap[item.host_id]) {
                result.push(item);
            }
            return result;
        }, []);
        validHostList.value = newValidHostList;
        triggerChange();
    };
    
    // 清除所有主机
    const handleRemoveAll = () => {
        resultList.value = [];
        validHostList.value = [];
        triggerChange();
    };

    defineExpose({
        // 所有 IP 列表
        getHostIpList () {
            return validHostList.value.map(item => item[hostRenderKey.value]);
        },
        // 异常 IP 列表
        getNotAlivelHostIpList () {
            const result = validHostList.value.reduce((result, item) => {
                if (item.alive !== 1) {
                    result.push(item[hostRenderKey.value]);
                }
                return result;
            }, []);
            invalidHostList.value.forEach((hostData) => {
                if (hostData[hostRenderKey.value]) {
                    result.push(hostData[hostRenderKey.value]);
                }
            });
            return result;
        },
        refresh () {
            fetchData();
        },
    });
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

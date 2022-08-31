<template>
    <div
        class="ip-selector-view-host"
        v-bkloading="{ isLoading }">
        <collapse-box>
            <template #title>
                <span style="font-weight: bold;">【已筛选出主机】</span>
                <span>
                    <span>
                        - 共
                        <span class="bk-ip-selector-number">{{ serachList.length }}</span>
                        台
                    </span>
                </span>
            </template>
            <template #action>
                <extend-action>
                    <div @click="handleCopyAllIP">复制所有 IP</div>
                    <div @click="handleCopyFaidedIP">复制异常 IP</div>
                    <template v-if="!context.readonly">
                        <div @click="handleRemoveAll">清除所有</div>
                        <div @click="handleRemoveFailedIP">清除异常 IP</div>
                    </template>
                </extend-action>
            </template>
            <render-host-table
                :data="renderData"
                :show-setting="false"
                :column-width-callback="columnWidthCallback">
                <template #[hostRenderKey]="{ row }">
                    <diff-tag :value="diffMap[row.hostId]" />
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
                <template
                    v-if="serachList.length < 1"
                    #empty>
                    <img src="../../images/empty.svg">
                    <div>搜索结果为空</div>
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
    import useLocalPagination from '../../hooks/use-local-pagination';
    import useHostRenderKey from '../../hooks/use-host-render-key';
    import useIpSelector from '../../hooks/use-ip-selector';
    import ExtendAction from '../../common/extend-action.vue';
    import DiffTag from '../../common/diff-tag.vue';
    import RenderHostTable from '../../common/render-table/host.vue';
    import {
        execCopy,
        getInvalidHostList,
        getHostDiffMap,
        groupHostList,
    } from '../../utils';
    import CollapseBox from './collapse-box/index.vue';

    const props = defineProps({
        data: {
            type: Array,
            default: () => [],
        },
        searchKey: {
            type: String,
            required: true,
        },
    });

    const emits = defineEmits([
        'change',
    ]);

    const isLoading = ref(false);
    const tableData = shallowRef([]);
    const diffMap = shallowRef({});
    const validHostList = shallowRef([]);
    const invalidHostList = shallowRef([]);

    let isInnerChange = false;

    const context = useIpSelector();

    const {
        key: hostRenderKey,
    } = useHostRenderKey();

    const {
        searchKey,
        serachList,
        isShowPagination,
        pagination,
        data: renderData,
        handlePaginationCurrentChange,
        handlePaginationLimitChange,
    } = useLocalPagination(
        tableData,
        {},
        (hostData, rule) => rule.test(hostData.ip) || rule.test(hostData.ipv6),
    );

    // 通过 hostId 获取主机详情
    const fetchData = () => {
        isLoading.value = true;
        AppManageService.fetchHostInfoByHostId({
            hostList: props.data,
        })
        .then((data) => {
            validHostList.value = data;
            invalidHostList.value = getInvalidHostList(props.data, validHostList.value);
            diffMap.value = getHostDiffMap(props.data, undefined, invalidHostList.value);

            const {
                newList,
                originalList,
            } = groupHostList(validHostList.value, diffMap.value);
            tableData.value = [
                ...invalidHostList.value,
                ...newList,
                ...originalList,
            ];
            pagination.count = tableData.value.length;
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

    watch(() => props.searchKey, () => {
        searchKey.value = props.searchKey;
    }, {
        immediate: true,
    });

    const triggerChange = () => {
        isInnerChange = true;
        emits('change', 'hostList', tableData.value);
    };

    // 删除指定主机
    const handleRemove = (hostData) => {
        tableData.value = tableData.value.reduce((result, item) => {
            if (item.hostId !== hostData.hostId) {
                result.push(item);
            }
            return result;
        }, []);
        triggerChange();
    };

    // 复制所有 IP
    const handleCopyAllIP = () => {
        const IPList = serachList.value.map(item => item[hostRenderKey.value]);
        execCopy(IPList.join('\n'), `复制成功 ${IPList.length} 个 IP`);
    };
    // 复制异常 IP
    const handleCopyFaidedIP = () => {
        const IPList = serachList.value.reduce((result, item) => {
            if (item.alive !== 1) {
                result.push(item[hostRenderKey.value]);
            }
            return result;
        }, []);
        execCopy(IPList.join('\n'), `复制成功 ${IPList.length} 个 IP`);
    };
    const handleRemoveFailedIP = () => {
        tableData.value = tableData.value.reduce((result, item) => {
            if (item.alive !== 1) {
                result.push(item);
            }
            return result;
        }, []);
        triggerChange();
    };
    // 清除所有主机
    const handleRemoveAll = () => {
        tableData.value = [];
        triggerChange();
    };
</script>
<style lang="postcss">
    @import "../../styles/table.mixin.css";

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

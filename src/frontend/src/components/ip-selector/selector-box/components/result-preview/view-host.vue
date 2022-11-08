<template>
    <CollapseBox v-if="data.length > 0">
        <template #title>
            【静态拓扑】
            <span>
                - 共
                <span class="bk-ip-selector-number">{{ data.length }}</span>
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
        </template>
        <template #action>
            <div
                v-bk-tooltips="'复制所有 IP'"
                @click="handleCopyIP">
                <ip-selector-icon type="copy" />
            </div>
            <div
                v-bk-tooltips="'清空'"
                @click="handlRemoveAll">
                <ip-selector-icon type="delete" />
            </div>
        </template>
        <div v-bkloading="{ isLoading }">
            <CallapseContentItem
                v-for="(item, index) in listData"
                :key="index"
                :content="item[hostRenderKey]"
                :removable="diffMap[item.host_id]!== 'remove'"
                @remove="handleRemove(item)">
                <span
                    v-if="diffMap[item.host_id] === 'repeat'"
                    style="color: #c4c6cc;">
                    (Host ID: {{ item.host_id }})
                </span>
                {{ item[hostRenderKey] || '--' }}
                <template #append>
                    <diff-tag :value="diffMap[item.host_id]" />
                </template>
            </CallapseContentItem>
        </div>
    </CollapseBox>
</template>
<script setup>
    import _ from 'lodash';
    import {
        ref,
        shallowRef,
        watch,
    } from 'vue';

    import DiffTag from '../../../common/diff-tag.vue';
    import IpSelectorIcon from '../../../common/ip-selector-icon';
    import useHostRenderKey from '../../../hooks/use-host-render-key';
    import useIpSelector from '../../../hooks/use-ip-selector';
    import Manager from '../../../manager';
    import {
        execCopy,
        getHostDiffMap,
        getInvalidHostList,
        getRemoveHostList,
        groupHostList,
    } from '../../../utils';

    import CallapseContentItem from './collapse-box/content-item.vue';
    import CollapseBox from './collapse-box/index.vue';

    const props = defineProps({
        data: {
            type: Array,
            required: true,
        },
    });

    const emits = defineEmits(['change']);

    const context = useIpSelector();
    const {
        key: hostRenderKey,
    } = useHostRenderKey();
    const isLoading = ref(false);
    const listData = shallowRef([]);

    const diffMap = shallowRef({});
    const validHostList = shallowRef([]);
    const removedHostList = shallowRef([]);
    const invalidHostList = shallowRef([]);

    const newHostNum = ref(0);

    let isCreated = false;

    // 通过 host_id 查询主机详情
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

    watch(() => props.data, () => {
        if (props.data.length > 0) {
            const needFetchHostDetail = _.find(props.data, item => !item.ip);
            if (needFetchHostDetail || !isCreated) {
                fetchData();
            } else {
                validHostList.value = [...props.data];
            }
            isCreated = true;
        } else {
            validHostList.value = [];
        }
    }, {
        immediate: true,
    });

    watch(validHostList, () => {
        invalidHostList.value = getInvalidHostList(props.data, validHostList.value);
        removedHostList.value = getRemoveHostList(props.data, context.originalValue);
        diffMap.value = getHostDiffMap(props.data, context.originalValue, invalidHostList.value);

        const {
            newList,
            originalList,
        } = groupHostList(validHostList.value, diffMap.value);

        newHostNum.value = newList.length;

        listData.value = [
            ...invalidHostList.value,
            ...newList,
            ...removedHostList.value,
            ...originalList,
        ];
    }, {
        immediate: true,
    });

    // 移除单个IP
    const handleRemove = (removeTarget) => {
        const result = props.data.reduce((result, item) => {
            if (removeTarget !== item) {
                result.push(item);
            }
            return result;
        }, []);

        emits('change', 'hostList', result);
    };

    // 复制IP
    const handleCopyIP = () => {
        const IPList = listData.value.map(item => item[hostRenderKey.value]);
        execCopy(IPList.join('\n'), `复制成功 ${IPList.length} 个 IP`);
    };
    
    // 移除所有IP
    const handlRemoveAll = () => {
        emits('change', 'hostList', []);
    };
</script>

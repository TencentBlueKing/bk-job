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
            <div @click="handleCopyIP">
                <i class="bk-ipselector-icon bk-ipselector-copy" />
            </div>
            <div @click="handlRemoveAll">
                <i class="bk-ipselector-icon bk-ipselector-delete" />
            </div>
        </template>
        <div v-bkloading="{ isLoading }">
            <CallapseContentItem
                v-for="(item, index) in listData"
                :key="index"
                :content="item.ip"
                :removable="diffMap[item.hostId]!== 'remove'"
                @remove="handleRemove(item)">
                <span
                    v-if="diffMap[item.hostId] === 'repeat'"
                    style="color: #c4c6cc;">
                    (Host ID: {{ item.hostId }})
                </span>
                {{ item.ip }}
                <template #append>
                    <diff-tag :value="diffMap[item.hostId]" />
                </template>
            </CallapseContentItem>
        </div>
    </CollapseBox>
</template>
<script setup>
    import {
        ref,
        watch,
        shallowRef,
    } from 'vue';
    import _ from 'lodash';
    import AppManageService from '@service/app-manage';
    import {
        execCopy,
        getInvalidHostList,
        getRemoveHostList,
        getHostDiffMap,
        getDiffNewNum,
        groupHostList,
    } from '../../../utils';
    import useIpSelector from '../../../hooks/use-ip-selector';
    import DiffTag from '../../../common/diff-tag.vue';
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

    const isLoading = ref(false);
    const listData = shallowRef([]);

    const diffMap = shallowRef({});
    const validHostList = shallowRef([]);
    const removedHostList = shallowRef([]);
    const invalidHostList = shallowRef([]);

    const newHostNum = ref(0);

    // 通过 hostId 查询主机详情
    const fetchData = () => {
        isLoading.value = true;
        AppManageService.fetchHostOfHost({
            hostIdList: props.data.map(({ hostId }) => hostId),
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
            if (needFetchHostDetail) {
                fetchData();
            } else {
                validHostList.value = [...props.data];
            }
        } else {
            validHostList.value = [];
        }
    }, {
        immediate: true,
    });

    watch(validHostList, () => {
        invalidHostList.value = getInvalidHostList(props.data, validHostList.value);
        removedHostList.value = getRemoveHostList(props.data, context.orinigalValue);
        diffMap.value = getHostDiffMap(props.data, context.orinigalValue, invalidHostList.value);
        newHostNum.value = getDiffNewNum(diffMap.value);

        const {
            newList,
            originalList,
        } = groupHostList(validHostList.value, diffMap.value);

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
        execCopy(listData.value.map(({ ip }) => ip).join('\n'), `复制成功 ${listData.value.length} 个 IP`);
    };
    
    // 移除所有IP
    const handlRemoveAll = () => {
        emits('change', 'hostList', []);
    };
</script>

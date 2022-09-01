<template>
    <CollapseBox v-if="data.length > 0">
        <template #title>
            【动态分组】
            <span>
                - 共
                <span class="bk-ip-selector-number">{{ data.length }}</span>
                个
            </span>
            <span v-if="newDynamicGroupNum">
                ，新增
                <span class="bk-ip-selector-number-success">{{ newDynamicGroupNum }}</span>
                个
            </span>
            <span v-if="removedDynamicGroupList.length">
                ，删除
                <span class="bk-ip-selector-number-error">{{ removedDynamicGroupList.length }}</span>
                个
            </span>
        </template>
        <template #action>
            <div @click="handlRemoveAll">
                <i class="bk-ipselector-icon bk-ipselector-delete" />
            </div>
        </template>
        <div v-bkloading="{ isLoading }">
            <CallapseContentItem
                v-for="(item, index) in listData"
                :key="index"
                :removable="diffMap[item.id]!== 'remove'"
                @remove="handleRemove(item)">
                {{ item.name || `#${item.id}` }}
                <template #append>
                    <diff-tag :value="diffMap[item.id]" />
                </template>
            </CallapseContentItem>
        </div>
    </CollapseBox>
</template>
<script setup>
    import _ from 'lodash';
    import {
        ref,
        watch,
        shallowRef,
    } from 'vue';
    import Manager from '../../../manager';
    import {
        getDiffNewNum,
        getDynamicGroupDiffMap,
        getInvalidDynamicGroupList,
        getRemoveDynamicGroupList,
        groupDynamicGroupList,
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

    const validDynamicGroupList = shallowRef([]);
    const removedDynamicGroupList = shallowRef([]);
    const invalidDynamicGroupList = shallowRef([]);
    const newDynamicGroupNum = ref(0);
    const diffMap = shallowRef({});

    // 根据 ID 获取分组详情
    const fetchData = () => {
        isLoading.value = true;
        Manager.service.fetchDynamicGroups({
            dynamicGroupList: props.data,
        })
        .then((data) => {
            validDynamicGroupList.value = data;
        })
        .finally(() => {
            isLoading.value = false;
        });
    };

    watch(() => props.data, () => {
        if (props.data.length > 0) {
            const needFetchHostDetail = _.find(props.data, item => !item.name);
            if (needFetchHostDetail) {
                fetchData();
            } else {
                validDynamicGroupList.value = [...props.data];
            }
        } else {
            validDynamicGroupList.value = [];
        }
    }, {
        immediate: true,
    });

    watch(validDynamicGroupList, () => {
        invalidDynamicGroupList.value = getInvalidDynamicGroupList(props.data, validDynamicGroupList.value);
        removedDynamicGroupList.value = getRemoveDynamicGroupList(props.data, context.orinigalValue);
        diffMap.value = getDynamicGroupDiffMap(props.data, context.orinigalValue, invalidDynamicGroupList.value);
        newDynamicGroupNum.value = getDiffNewNum(diffMap.value);

        const {
            newList,
            originalList,
        } = groupDynamicGroupList(validDynamicGroupList.value, diffMap.value);

        listData.value = [
            ...invalidDynamicGroupList.value,
            ...newList,
            ...removedDynamicGroupList.value,
            ...originalList,
        ];
    });

    // 移除指定分组
    const handleRemove = (removeTarget) => {
        const result = props.data.reduce((result, item) => {
            if (removeTarget !== item) {
                result.push(item);
            }
            return result;
        }, []);

        emits('change', 'dynamicGroupList', result);
    };

    // 移除所有分组
    const handlRemoveAll = () => {
        emits('change', 'dynamicGroupList', []);
    };
</script>

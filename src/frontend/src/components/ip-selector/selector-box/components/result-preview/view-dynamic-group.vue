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
        shallowRef,
        watch,
    } from 'vue';

    import DiffTag from '../../../common/diff-tag.vue';
    import IpSelectorIcon from '../../../common/ip-selector-icon';
    import useIpSelector from '../../../hooks/use-ip-selector';
    import Manager from '../../../manager';
    import {
        getDynamicGroupDiffMap,
        getInvalidDynamicGroupList,
        getRemoveDynamicGroupList,
        groupDynamicGroupList,
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

    const isLoading = ref(false);
    const listData = shallowRef([]);

    const validDynamicGroupList = shallowRef([]);
    const removedDynamicGroupList = shallowRef([]);
    const invalidDynamicGroupList = shallowRef([]);
    const newDynamicGroupNum = ref(0);
    const diffMap = shallowRef({});

    const isCreated = false;

    // 根据 ID 获取分组详情
    const fetchData = () => {
        isLoading.value = true;
        Manager.service.fetchDynamicGroups({
            [Manager.nameStyle('dynamicGroupList')]: props.data.map(item => ({
                [Manager.nameStyle('id')]: item.id,
                [Manager.nameStyle('meta')]: item.meta,
            })),
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
            if (needFetchHostDetail || !isCreated) {
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
        removedDynamicGroupList.value = getRemoveDynamicGroupList(props.data, context.originalValue);
        diffMap.value = getDynamicGroupDiffMap(props.data, context.originalValue, invalidDynamicGroupList.value);

        const {
            newList,
            originalList,
        } = groupDynamicGroupList(validDynamicGroupList.value, diffMap.value);

        newDynamicGroupNum.value = newList.length;

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

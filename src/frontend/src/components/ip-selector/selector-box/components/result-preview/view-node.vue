<template>
    <CollapseBox v-if="data.length > 0">
        <template #title>
            【动态拓扑】
            <span>
                - 共
                <span class="bk-ip-selector-number">{{ listData.length }}</span>
                个
            </span>
            <span v-if="newNodeNum">
                ，新增
                <span class="bk-ip-selector-number-success">{{ newNodeNum }}</span>
                个
            </span>
            <span v-if="removedNodeList.length">
                ，删除
                <span class="bk-ip-selector-number-error">{{ removedNodeList.length }}</span>
                台
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
                :removable="diffMap[genNodeKey(item)]!== 'remove'"
                @remove="handleRemove(item)">
                {{ listDataNamePathMap[genNodeKey(item)] || `#${item.instance_id}` }}
                <template #append>
                    <diff-tag :value="diffMap[genNodeKey(item)]" />
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
        genNodeKey,
        getInvalidNodeList,
        getNodeDiffMap,
        getRemoveNodeList,
        groupNodeList,
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
    const listDataNamePathMap = shallowRef({});

    const validNodeList = shallowRef([]);
    const removedNodeList = shallowRef([]);
    const invalidNodeList = shallowRef([]);
    const diffMap = shallowRef({});
    const newNodeNum = ref(0);

    // 通过节点的 id 查询节点的完整层级名字
    const fetchData = () => {
        isLoading.value = true;
        Manager.service.fetchNodesQueryPath({
            [Manager.nameStyle('nodeList')]: props.data.map(item => ({
                [Manager.nameStyle('objectId')]: item.object_id,
                [Manager.nameStyle('instanceId')]: item.instance_id,
                [Manager.nameStyle('meta')]: item.meta,
            })),
        })
            .then((data) => {
                const validData = [];
                const nodeNamePathMap = {};
                data.forEach((item) => {
                    const tailNode = _.last(item);
                    validData.push(tailNode);
                    nodeNamePathMap[genNodeKey(tailNode)] = item.map(nodeData => nodeData.instance_name).join('/');
                });
                listDataNamePathMap.value = nodeNamePathMap;
                validNodeList.value = validData;
            })
            .finally(() => {
                isLoading.value = false;
            });
    };

    watch(() => props.data, () => {
        if (props.data.length > 0) {
            const needFetchNodeDetail = _.find(props.data, item => !item.namePath);
            if (needFetchNodeDetail) {
                fetchData();
            } else {
                validNodeList.value = [...props.data];
            }
        } else {
            validNodeList.value = [];
        }
    }, {
        immediate: true,
    });

    watch(validNodeList, () => {
        invalidNodeList.value = getInvalidNodeList(props.data, validNodeList.value);
        removedNodeList.value = getRemoveNodeList(props.data, context.originalValue);
        diffMap.value = getNodeDiffMap(props.data, context.originalValue, invalidNodeList.value);

        const {
            newList,
            originalList,
        } = groupNodeList(validNodeList.value, diffMap.value);

        newNodeNum.value = newList.length;

        listData.value = [
            ...invalidNodeList.value,
            ...newList,
            ...removedNodeList.value,
            ...originalList,
        ];
    });

    // 移除指定节点
    const handleRemove = (removeTarget) => {
        const result = props.data.reduce((result, item) => {
            if (genNodeKey(removeTarget) !== genNodeKey(item)) {
                result.push(item);
            }
            return result;
        }, []);

        emits('change', 'nodeList', result);
    };

    // 移除所有
    const handlRemoveAll = () => {
        emits('change', 'nodeList', []);
    };
</script>

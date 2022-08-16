<template>
    <div class="ip-selector-result-preview">
        <div class="preview-header">结果预览</div>
        <div
            v-if="resultNum"
            class="result-num">
            已选择（{{ resultNum }}）
        </div>
        <view-host :data="hostList" />
        <view-node :data="nodeList" />
        <view-group :data="groupList" />
    </div>
</template>
<script setup>
    import { computed } from 'vue';
    import ViewHost from './view-host.vue';
    import ViewNode from './view-node.vue';
    import ViewGroup from './view-group.vue';

    const props = defineProps({
        hostList: {
            type: Array,
            default: () => [],
        },
        nodeList: {
            type: Array,
            default: () => [],
        },
        groupList: {
            type: Array,
            default: () => [],
        },
    });

    const resultNum = computed(() => {
        let num = 0;
        if (props.hostList.length > 0) {
            num += 1;
        }
        if (props.nodeList.length > 0) {
            num += 1;
        }
        if (props.groupList.length > 0) {
            num += 1;
        }

        return num;
    });
</script>
<style lang="postcss">
    .ip-selector-result-preview {
        width: 240px;
        height: 100%;
        font-size: 12px;
        background: #f5f6fa;
        border-left: 1px solid #dcdee5;

        .preview-header {
            padding: 12px 24px 16px;
            font-size: 14px;
            line-height: 22px;
            color: #313238;
        }

        .result-num {
            display: flex;
            height: 32px;
            padding-left: 16px;
            margin: 0 24px;
            margin-bottom: 4px;
            font-size: 12px;
            font-weight: bold;
            color: #3a84ff;
            background: #e1ecff;
            align-items: center;
        }
    }
</style>

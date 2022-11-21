<template>
    <div class="ip-selector-result-preview">
        <div class="preview-header">
            结果预览
        </div>
        <div
            v-if="resultNum"
            class="result-num">
            已选择（{{ resultNum }}）
        </div>
        <div
            class="result-wrapper"
            :style="styles">
            <view-host
                ref="viewHostRef"
                :data="hostList"
                v-on="listeners" />
            <view-node
                :data="nodeList"
                v-on="listeners" />
            <view-dynamic-group
                :data="dynamicGroupList"
                v-on="listeners" />
        </div>
        <div
            v-if="resultNum < 1"
            class="result-empty">
            <img src="../../../images/empty.svg">
            <div style="margin-top: 8px;">
                暂无数据，请从左侧添加对象
            </div>
        </div>
        <div class="extend-action">
            <extend-action>
                <div @click="handleClear">
                    清除所有
                </div>
                <template v-if="hostList.length > 0">
                    <div @click="handleRemoveFailedIP">
                        清除异常 IP
                    </div>
                    <div @click="handleCopyAllIP">
                        复制所有 IP
                    </div>
                    <div @click="handleCopeFailedAIP">
                        复制异常 IP
                    </div>
                </template>
            </extend-action>
        </div>
    </div>
</template>
<script setup>
    import {
        computed,
        ref,
        useListeners,
    } from 'vue';

    import ExtendAction from '../../../common/extend-action.vue';
    import useDialogSize from '../../../hooks/use-dialog-size';
    import useHostRenderKey from '../../../hooks/use-host-render-key';
    import {
        execCopy,
        isAliveHost,
     } from '../../../utils';

    import ViewDynamicGroup from './view-dynamic-group.vue';
    import ViewHost from './view-host.vue';
    import ViewNode from './view-node.vue';

    const props = defineProps({
        hostList: {
            type: Array,
            default: () => [],
        },
        nodeList: {
            type: Array,
            default: () => [],
        },
        dynamicGroupList: {
            type: Array,
            default: () => [],
        },
    });

    const emits = defineEmits([
        'change',
        'clear',
    ]);

    const listeners = useListeners();

    const viewHostRef = ref();

    const resultNum = computed(() => {
        let num = 0;
        if (props.hostList.length > 0) {
            num += 1;
        }
        if (props.nodeList.length > 0) {
            num += 1;
        }
        if (props.dynamicGroupList.length > 0) {
            num += 1;
        }

        return num;
    });

    const {
        contentHeight: dialogContentHeight,
    } = useDialogSize();
    const {
        key: hostRenderKey,
    } = useHostRenderKey();

    const styles = computed(() => ({
        height: `${dialogContentHeight.value - 68}px`,
    }));

    // 清空所有
    const handleClear = () => {
        emits('clear');
    };

    // 移除异常 IP
    const handleRemoveFailedIP = () => {
        const hostList = viewHostRef.value.getAllHostList().reduce((result, item) => {
            if (isAliveHost(item)) {
                result.push(item);
            }
            return result;
        }, []);

        emits('change', 'hostList', hostList);
    };

    // 复制所有 IP
    const handleCopyAllIP = () => {
        const IPList = viewHostRef.value.getAllHostList().map(item => item[hostRenderKey.value]);
        execCopy(IPList.join('\n'), `复制成功 ${IPList.length} 个 IP`);
    };

    // 复制异常 IP
    const handleCopeFailedAIP = () => {
        const IPList = viewHostRef.value.getAllHostList().reduce((result, item) => {
            if (!isAliveHost(item)) {
                result.push(item[hostRenderKey.value]);
            }
            return result;
        }, []);
        execCopy(IPList.join('\n'), `复制成功 ${IPList.length} 个 IP`);
    };
</script>
<style lang="postcss">
    .ip-selector-result-preview {
        position: relative;
        height: 100%;
        font-size: 12px;
        background: #f5f6fa;

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

        .result-wrapper {
            overflow-y: auto;

            & > * {
                margin-top: 8px;
            }
        }

        .result-empty {
            position: absolute;
            top: 190px;
            right: 0;
            left: 0;
            font-size: 12px;
            color: #63656e;
            text-align: center;
        }

        .extend-action {
            position: absolute;
            top: 16px;
            right: 24px;
        }
    }
</style>

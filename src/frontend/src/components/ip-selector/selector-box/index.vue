<template>
    <bk-dialog
        :value="isShow"
        :width="dialogWidth"
        :close-icon="false"
        :draggable="false"
        class="bk-ip-selector-dialog">
        <div class="container-layout">
            <div
                class="layout-left"
                v-bkloading="{ isLoading: isTopoDataLoading } ">
                <panel-tab
                    :value="panelType"
                    :unique-type="panelTableUniqueType"
                    @change="handleTypeChange" />
                <div :style="contentStyles">
                    <panel-content
                        v-if="!isTopoDataLoading"
                        :topo-tree-data="topoTreeData"
                        :type="panelType"
                        :last-host-list="lastHostList"
                        :last-node-list="lastNodeList"
                        :last-dynamic-group-list="lastDynamicGroupList"
                        @change="handleChange" />
                </div>
            </div>
            <div class="layout-right">
                <result-preview
                    v-if="isShow"
                    :host-list="lastHostList"
                    :node-list="lastNodeList"
                    :dynamic-group-list="lastDynamicGroupList"
                    @change="handleChange"
                    @clear="handleClearChange" />
            </div>
        </div>
        <template #footer>
            <div>
                <bk-button
                    theme="primary"
                    @click="handleSubmit">
                    确定
                </bk-button>
                <bk-button @click="handleCancel">
                    取消
                </bk-button>
            </div>
        </template>
    </bk-dialog>
</template>
<script setup>
    import _ from 'lodash';
    import {
        ref,
        shallowRef,
        computed,
        watch,
    } from 'vue';
    import Manager from '../manager';
    import {
        transformTopoTree,
        formatInput,
        formatOutput,
    } from '../utils/index';
    import useDialogSize from '../hooks/use-dialog-size';
    import PanelTab from './components/panel-tab/index.vue';
    import PanelContent from './components/panel-content/index.vue';
    import ResultPreview from './components/result-preview/index.vue';

    const props = defineProps({
        isShow: {
            type: Boolean,
            default: false,
        },
        value: {
            type: Object,
            default: () => ({
                hostList: [],
                nodeList: [],
                dynamicGroupList: [],
            }),
        },
    });

    const emits = defineEmits([
        'change',
        'cancel',
    ]);

    const isTopoDataLoading = ref(true);
    const panelType = ref('staticTopo');
    const panelTableUniqueType = computed(() => {
        if (!Manager.config.unqiuePanelValue) {
            return '';
        }
        if (!_.isEmpty(lastHostList.value)) {
            return 'staticTopo';
        }
        if (!_.isEmpty(lastNodeList.value)) {
            return 'dynamicTopo';
        }
        if (!_.isEmpty(lastDynamicGroupList.value)) {
            return 'dynamicGroup';
        }
        return '';
    });

    const topoTreeData = shallowRef([]);

    const lastHostList = shallowRef([]);
    const lastNodeList = shallowRef([]);
    const lastDynamicGroupList = shallowRef([]);

    const {
        width: dialogWidth,
        contentHeight: dialogContentHeight,
    } = useDialogSize();

    const contentStyles = computed(() => ({
        height: `${dialogContentHeight.value}px`,
    }));

    const fetchTopoData = () => {
        isTopoDataLoading.value = true;
        // 获取拓扑树
        Manager.service.fetchTopologyHostCount()
            .then((data) => {
                topoTreeData.value = transformTopoTree(data);
            })
            .finally(() => {
                isTopoDataLoading.value = false;
            });
    };

    watch(() => props.isShow, () => {
        if (props.isShow) {
            panelType.value = 'staticTopo';
            fetchTopoData();
            const {
                host_list: hostList,
                node_list: nodeList,
                dynamic_group_list: dynamicGroupList,
            } = formatInput(props.value || {});

            lastHostList.value = hostList;
            lastNodeList.value = nodeList;
            lastDynamicGroupList.value = dynamicGroupList;
        }
    });
    
    // 面板类型切换
    const handleTypeChange = (type) => {
        panelType.value = type;
    };
    
    // 用户操作数据
    const handleChange = (name, value) => {
        switch (name) {
            case 'hostList':
                lastHostList.value = value;
                break;
            case 'nodeList':
                lastNodeList.value = value;
                break;
            case 'dynamicGroupList':
                lastDynamicGroupList.value = value;
                break;
        }
    };
    const handleClearChange = () => {
        lastHostList.value = [];
        lastNodeList.value = [];
        lastDynamicGroupList.value = [];
    };
    // 提交编辑
    const handleSubmit = () => {
        emits('change', formatOutput({
            hostList: lastHostList.value,
            nodeList: lastNodeList.value,
            dynamicGroupList: lastDynamicGroupList.value,
        }));
    };
    // 取消编辑
    const handleCancel = () => {
        emits('cancel');
    };
</script>
<style lang="postcss">
    .bk-ip-selector-dialog {
        .bk-dialog {
            .bk-dialog-tool {
                display: none;
            }

            .bk-dialog-body {
                padding: 0;
            }
        }

        .container-layout {
            display: flex;
            color: #63656e;

            .layout-left {
                flex: 1;
            }
        }
    }
</style>

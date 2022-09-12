<template>
    <bk-dialog
        class="bk-ip-selector-dialog"
        :close-icon="false"
        :draggable="false"
        :value="isShow"
        :width="dialogWidth">
        <resize-layout
            :default-width="320"
            flex-direction="right">
            <div v-bkloading="{ isLoading: isTopoDataLoading } ">
                <panel-tab
                    :is-show="isShow"
                    :unique-type="panelTableUniqueType"
                    :value="panelType"
                    @change="handleTypeChange" />
                <div :style="contentStyles">
                    <panel-content
                        v-if="!isTopoDataLoading"
                        :last-dynamic-group-list="lastDynamicGroupList"
                        :last-host-list="lastHostList"
                        :last-node-list="lastNodeList"
                        :topo-tree-data="topoTreeData"
                        :type="panelType"
                        @change="handleChange" />
                </div>
            </div>
            <template slot="right">
                <result-preview
                    v-if="isShow"
                    :dynamic-group-list="lastDynamicGroupList"
                    :host-list="lastHostList"
                    :node-list="lastNodeList"
                    @change="handleChange"
                    @clear="handleClearChange" />
            </template>
        </resize-layout>
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
        computed,
        ref,
        shallowRef,
        watch,
    } from 'vue';

    import useDialogSize from '../hooks/use-dialog-size';
    import Manager from '../manager';
    import {
        formatInput,
        formatOutput,
        transformTopoTree,
    } from '../utils/index';

    import PanelContent from './components/panel-content/index.vue';
    import PanelTab from './components/panel-tab/index.vue';
    import ResizeLayout from './components/resize-layout.vue';
    import ResultPreview from './components/result-preview/index.vue';

    const props = defineProps({
        isShow: {
            type: Boolean,
            default: false,
        },
        value: {
            type: Object,
            default: () => ({
                host_list: [],
                node_list: [],
                dynamic_group_list: [],
            }),
        },
    });

    const emits = defineEmits([
        'change',
        'cancel',
    ]);

    const panelType = ref('');
    
    const isTopoDataLoading = ref(true);

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
            // panelType.value = 'staticTopo';
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

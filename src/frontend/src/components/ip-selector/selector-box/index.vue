<template>
    <component
        :is="rootCom"
        class="bk-ip-selector-box"
        :class="{
            [mode]: true
        }"
        :close-icon="false"
        :draggable="false"
        :value="isShow"
        :width="dialogWidth">
        <resize-layout
            :default-width="320"
            flex-direction="right">
            <div v-bkloading="{ isLoading: isTopoDataLoading } ">
                <panel-tab
                    :is-show="hasRendered"
                    :unique-type="panelTabUniqueType"
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
                    v-if="hasRendered"
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
    </component>
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
        mode: {
            type: String,
            required: true,
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

    window.ipManager = Manager;

    const panelType = ref('');
    
    const isTopoDataLoading = ref(true);

    // 面板单选时数据回填选中的面板
    const panelTabUniqueType = computed(() => {
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

    const isDialogMode = computed(() => props.mode === 'dialog');
    const rootCom = computed(() => isDialogMode.value ? 'bk-dialog' : 'div');
    const hasRendered = computed(() => isDialogMode.value ? props.isShow : true);

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

    watch(hasRendered, () => {
        if (hasRendered.value) {
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
    }, {
        immediate: true,
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
        if (!isDialogMode.value) {
            handleSubmit();
        }
    };
    // 清空所有
    const handleClearChange = () => {
        lastHostList.value = [];
        lastNodeList.value = [];
        lastDynamicGroupList.value = [];
        handleSubmit();
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
.bk-ip-selector-box {
    background: #fff;

    &.section {
        border: 1px solid #dcdee5;
        border-radius: 2px;
    }

    &.dialog {
        .bk-dialog-wrapper {
            text-align: center;

            &::after {
                display: inline-block;
                width: 1px;
                height: 100%;
                vertical-align: middle;
                content: "";
            }

            .bk-dialog {
                top: unset;
                display: inline-block;
                margin: unset;
                text-align: initial;
                vertical-align: middle;

                &.bk-dialog-fullscreen {
                    top: 0;
                    right: 0;
                    bottom: 0;
                    left: 0;
                }

                &.bk-info-box {
                    .bk-dialog-content-drag {
                        position: relative;
                    }
                }

                .bk-dialog-tool {
                    display: none;
                }

                .bk-dialog-body {
                    padding: 0;
                }
            }

            .header-on-left {
                padding-bottom: 0 !important;
            }

            .bk-dialog-content {
                width: 100%;
            }
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

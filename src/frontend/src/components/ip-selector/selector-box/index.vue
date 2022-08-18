<template>
    <bk-dialog
        :value="isShow"
        width="1140"
        :close-icon="false"
        :draggable="false"
        class="ip-selector-dialog">
        <div class="container-layout">
            <div class="layout-left">
                <type-tab
                    :value="panelType"
                    @change="handleTypeChange" />
                <panel-content
                    :topo-tree-data="topoTreeData"
                    :type="panelType"
                    :last-host-list="lastHostList"
                    :last-node-list="lastNodeList"
                    :last-group-list="lastGroupList"
                    @change="handleChange" />
            </div>
            <div class="layout-right">
                <result-preview
                    :host-list="lastHostList"
                    :node-list="lastNodeList"
                    :group-list="lastGroupList" />
            </div>
        </div>
        <template #footer>
            <div>
                <bk-button
                    theme="primary"
                    @click="handleSubmit">
                    确定qwe
                </bk-button>
                <bk-button @click="handleCancle">取消</bk-button>
            </div>
        </template>
    </bk-dialog>
</template>
<script setup>
    import {
        ref,
        shallowRef,
    } from 'vue';
    import TypeTab from './components/type-tab';
    import PanelContent from './components/panel-content';
    import ResultPreview from './components/result-preview';
    import { transformTopoTree } from './utils/index';

    import AppManageService from '@service/app-manage';

    defineProps({
        isShow: {
            type: Boolean,
            default: false,
        },
    });

    const emits = defineEmits(['change']);

    const panelType = ref('staticTopo');
    const topoTreeData = shallowRef([]);

    const lastHostList = shallowRef([]);
    const lastNodeList = shallowRef([]);
    const lastGroupList = shallowRef([]);
    
    const handleTypeChange = (type) => {
        panelType.value = type;
    };

    // 获取拓扑树
    AppManageService.fetchTopologyWithCount()
        .then((data) => {
            topoTreeData.value = transformTopoTree([data]);
            console.log('fromasda = ', topoTreeData.value);
        });
    // 用户操作数据
    const handleChange = (name, value) => {
        console.log('from changechange  =  ', name, value);
        switch (name) {
            case 'host':
                lastHostList.value = value;
                break;
            case 'node':
                lastNodeList.value = value;
                break;
            case 'group':
                lastGroupList.value = value;
                break;
        }
    };
    // 提交编辑
    const handleSubmit = () => {
        emits('change', {
            hostList: lastHostList.value.map(item => ({
                hostId: item.hostId,
                ip: item.ip,
                ipv6: item.ipv6,
            })),
            nodeList: lastNodeList.value.map(item => ({
                objectId: item.objectId,
                instanceId: item.instanceId,
            })),
            groupList: lastGroupList.value.map(item => ({
                id: item.id,
            })),
        });
    };
    // 取消编辑
    const handleCancle = () => {
        
    };
</script>
<style lang="postcss">
    .ip-selector-dialog {
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

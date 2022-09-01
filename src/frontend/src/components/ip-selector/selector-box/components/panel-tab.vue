<template>
    <div class="ip-selector-panel-tab">
        <div
            v-if="panelMap['staticTopo']"
            class="tab-item"
            :class="{
                active: value === 'staticTopo',
                disabled: isUnqiuePanelValue && uniqueType !== 'staticTopo',
            }"
            @click="handleChange('staticTopo', isUnqiuePanelValue && unqiuePanelValue !== 'staticTopo')">
            静态拓扑
        </div>
        <div
            v-if="panelMap['dynamicTopo']"
            class="tab-item"
            :class="{
                active: value === 'dynamicTopo',
                disabled: isUnqiuePanelValue && uniqueType !== 'dynamicTopo',
            }"
            @click="handleChange('dynamicTopo', isUnqiuePanelValue && unqiuePanelValue !== 'dynamicTopo')">
            动态拓扑
        </div>
        <div
            v-if="panelMap['dynamicGroup']"
            class="tab-item"
            :class="{
                active: value === 'dynamicGroup',
                disabled: isUnqiuePanelValue && uniqueType !== 'dynamicGroup',
            }"
            @click="handleChange('dynamicGroup', isUnqiuePanelValue && uniqueType !== 'dynamicGroup')">
            动态分组
        </div>
        <div
            v-if="panelMap['serviceTemplate']"
            class="tab-item"
            :class="{
                active: value === 'serviceTemplate',
                disabled: isUnqiuePanelValue && uniqueType !== 'serviceTemplate',
            }"
            @click="handleChange('serviceTemplate', isUnqiuePanelValue && uniqueType !== 'serviceTemplate')">
            服务模板
        </div>
        <div
            v-if="panelMap['setTemplate']"
            class="tab-item"
            :class="{
                active: value === 'setTemplate',
                disabled: isUnqiuePanelValue && uniqueType !== 'setTemplate',
            }"
            @click="handleChange('setTemplate', isUnqiuePanelValue && uniqueType !== 'setTemplate')">
            集群模板
        </div>
        <div
            v-if="panelMap['customInput']"
            class="tab-item"
            :class="{
                active: value === 'customInput',
                disabled: isUnqiuePanelValue && uniqueType !== 'staticTopo',
            }"
            @click="handleChange('customInput', isUnqiuePanelValue && uniqueType !== 'staticTopo')">
            自定义输入
        </div>
    </div>
</template>
<script setup>
    import { computed } from 'vue';
    import Manager from '../../manager';
    import { makeMap } from '../../utils';

    const props = defineProps({
        value: {
            type: String,
            required: true,
        },
        uniqueType: {
            type: String,
        },
    });

    const emits = defineEmits(['change', 'update:value']);

    const {
        unqiuePanelValue,
        panelList,
    } = Manager.config;

    const isUnqiuePanelValue = computed(() => Boolean(props.uniqueType) && unqiuePanelValue);

    const panelMap = makeMap(panelList);

    const handleChange = (value, disabled) => {
        if (disabled) {
            return;
        }
        if (value === props.value) {
            return;
        }
        emits('change', value);
        emits('update:value', value);
    };
</script>
<style lang="postcss" scoped>
    .ip-selector-panel-tab {
        display: flex;
        background: #fafbfd;
        border-bottom: 1px solid #dcdee5;
        user-select: none;

        .tab-item {
            flex: 1;
            height: 40px;
            line-height: 40px;
            color: #63656e;
            text-align: center;
            cursor: pointer;
            border-right: 1px solid #dcdee5;

            &.active {
                position: relative;
                color: #313238;
                cursor: default;
                background: #fff;

                &::after {
                    position: absolute;
                    bottom: -2px;
                    left: 0;
                    width: 100%;
                    height: 3px;
                    background: #fff;
                    content: "";
                }
            }

            &.disabled {
                color: #c4c6cc;
                cursor: not-allowed;
            }

            &:last-child {
                border-right: none;
            }
        }
    }
</style>

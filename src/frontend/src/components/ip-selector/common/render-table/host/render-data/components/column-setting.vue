<template>
    <div
        ref="rootRef"
        class="host-table-column-setting-box"
        style="width: 100%; height: 100%;">
        <slot />
        <div
            ref="popRef"
            class="host-table-column-setting-popover">
            <div class="setting-header">
                表格设置
            </div>
            <bk-checkbox-group v-model="selectedList">
                <vuedraggable
                    :animation="200"
                    class="column-list"
                    :disabled="false"
                    ghost-class="ghost"
                    group="description"
                    :list="renderColumnList">
                    <div
                        v-for="item in renderColumnList"
                        :key="item.label"
                        class="column-item">
                        <span
                            v-if="item.key === 'ip'"
                            v-bk-tooltips="{
                                content: 'IP 与 IPv6 至少需保留一个',
                                disabled: selectedList.includes('ipv6'),
                            }">
                            <bk-checkbox
                                :disabled="!selectedList.includes('ipv6')"
                                :value="item.key">
                                {{ item.label }}
                            </bk-checkbox>
                        </span>
                        <span
                            v-else-if="item.key === 'ipv6'"
                            v-bk-tooltips="{
                                content: 'IP 与 IPv6 至少需保留一个',
                                disabled: selectedList.includes('ip'),
                            }">
                            <bk-checkbox
                                :disabled="!selectedList.includes('ip')"
                                :value="item.key">
                                {{ item.label }}
                            </bk-checkbox>
                        </span>
                        <template v-else>
                            <bk-checkbox :value="item.key">
                                {{ item.label }}
                            </bk-checkbox>
                        </template>
                        <div class="column-item-drag">
                            <ip-selector-icon type="ketuodong" />
                        </div>
                    </div>
                </vuedraggable>
            </bk-checkbox-group>
            <div class="setting-footer">
                <bk-button
                    style="margin-right: 8px;"
                    theme="primary"
                    @click="handleSubmitSetting">
                    确定
                </bk-button>
                <bk-button @click="handleHideSetting">
                    取消
                </bk-button>
            </div>
        </div>
    </div>
</template>
<script setup>
    import tippy from 'tippy.js';
    import {
        onBeforeUnmount,
        onMounted,
        ref,
        shallowRef,
    } from 'vue';
    import vuedraggable from 'vuedraggable';

    import Manager from '../../../../../manager';
    import IpSelectorIcon from '../../../../ip-selector-icon';
    import tableColumnConfig from '../../column-config';

    const props = defineProps({
        selectedList: {
            type: Array,
            required: true,
        },
        sortList: {
            type: Array,
            required: true,
        },
    });

    const emits = defineEmits([
        'change',
        'sort-change',
        'close',
    ]);

    const { hostTableCustomColumnList } = Manager.config;

    const tableCustomColumnConfig = hostTableCustomColumnList.reduce((result, item) => ({
        ...result,
        [item.key]: item,
    }), {});

    const rootRef = ref();
    const popRef = ref();
    const selectedList = shallowRef([...props.selectedList]);

    const renderColumnList = ref(props.sortList.reduce((result, key) => {
        if (tableColumnConfig[key]) {
            result.push({
                key,
                ...tableColumnConfig[key],
            });
        } else if (tableCustomColumnConfig[key]) {
            result.push({
                key,
                ...tableCustomColumnConfig[key],
            });
        }
        return result;
    }, []));

    let tippyInstance;

    const handleSubmitSetting = () => {
        const sortList = renderColumnList.value.map(item => item.key);
        emits('change', selectedList.value, sortList);
        tippyInstance.hide();
    };

    const handleHideSetting = () => {
        tippyInstance.hide();
    };

    onMounted(() => {
        tippyInstance = tippy(rootRef.value, {
            content: popRef.value,
            placement: 'bottom-end',
            appendTo: () => document.body,
            theme: 'light',
            maxWidth: 'none',
            trigger: 'click',
            interactive: true,
            arrow: true,
            offset: [0, 8],
            zIndex: 999999,
            hideOnClick: true,
        });
    });

    onBeforeUnmount(() => {
        if (tippyInstance) {
            tippyInstance.hide();
            tippyInstance.destroy();
            tippyInstance = null;
        }
    });

</script>
<style lang="postcss" scoped>
    .host-table-column-setting-box {
        display: flex;
        width: 100%;
        height: 100%;
        align-items: center;
        justify-content: center;
        border-left: 1px solid rgb(220 222 229);
    }

    .host-table-column-setting-popover {
        width: 545px;
        padding-top: 24px;
        margin: -5px -9px;
        background: #fff;
        border-radius: 2px;

        .setting-header {
            padding: 0 24px;
            font-size: 20px;
            line-height: 20px;
            color: #313238;
        }

        .column-list {
            display: flex;
            flex-wrap: wrap;
            padding: 0 24px 36px;
        }

        .column-item {
            display: flex;
            align-items: center;
            width: 165px;
            height: 32px;
            padding: 0 8px;
            margin-top: 16px;
            border-radius: 2px;

            &:hover {
                background: #f5f7fa;

                .column-item-drag {
                    display: flex;
                }
            }

            .bk-checkbox-text {
                font-size: 12px;
            }
        }

        .column-item-drag {
            display: none;
            width: 16px;
            height: 16px;
            margin-left: auto;
            font-size: 12px;
            color: #979ba5;
            justify-content: center;
            align-items: center;
            cursor: move;
        }

        .setting-footer {
            display: flex;
            height: 50px;
            padding-right: 24px;
            background: #fafbfd;
            border-top: 1px solid #dcdee5;
            justify-content: flex-end;
            align-items: center;
        }

        .ghost {
            background: #c8ebfb;
            opacity: 50%;
        }
    }
</style>

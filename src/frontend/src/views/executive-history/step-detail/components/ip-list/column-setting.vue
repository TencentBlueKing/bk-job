<template>
    <div class="list-column-setting">
        <div class="select-body">
            <div class="title">
                {{ $t('history.字段显示设置') }}
            </div>
            <bk-checkbox
                :checked="isAllColumn"
                :indeterminate="isIndeterminate"
                @click.native="handleToggleAll">
                {{ $t('history.全选') }}
            </bk-checkbox>
            <bk-checkbox-group v-model="tempAllShowColumn">
                <template v-for="item in columnList">
                    <span
                        v-if="item.name === 'ip'"
                        :key="`ip_${item.name}`"
                        v-bk-tooltips="{
                            content: 'IP 与 IPv6 至少需保留一个',
                            disabled: tempAllShowColumn.includes('ipv6'),
                        }"
                        class="select-column">
                        <bk-checkbox
                            :checked="item.checked"
                            :disabled="!tempAllShowColumn.includes('ipv6')"
                            :value="item.name">
                            {{ item.label }}
                        </bk-checkbox>
                    </span>
                    <span
                        v-else-if="item.name === 'ipv6'"
                        :key="`ipv6_${item.name}`"
                        v-bk-tooltips="{
                            content: 'IP 与 IPv6 至少需保留一个',
                            disabled: tempAllShowColumn.includes('ip'),
                        }"
                        class="select-column">
                        <bk-checkbox
                            :checked="item.checked"
                            :disabled="!tempAllShowColumn.includes('ip')"
                            :value="item.name">
                            {{ item.label }}
                        </bk-checkbox>
                    </span>
                    <bk-checkbox
                        v-else
                        :key="item.name"
                        :checked="item.checked"
                        class="select-column"
                        :value="item.name">
                        {{ item.label }}
                    </bk-checkbox>
                </template>
            </bk-checkbox-group>
        </div>
        <div class="select-footer">
            <bk-button
                theme="primary"
                @click="handleSubmitSetting">
                {{ $t('history.确定') }}
            </bk-button>
            <bk-button @click="handleHideSetting">
                {{ $t('history.取消') }}
            </bk-button>
        </div>
    </div>
</template>
<script setup>
    import {
        computed,
        ref,
    } from 'vue';

    const props = defineProps({
        columnList: {
            type: Array,
            required: true,
        },
        value: {
            type: Array,
            required: true,
        },
    });
    const emits = defineEmits([
        'change',
        'close',
    ]);

    const tempAllShowColumn = ref([...props.value]);

    const isIndeterminate = computed(() => tempAllShowColumn.value.length !== props.columnList.length);

    const isAllColumn = computed(() => tempAllShowColumn.value.length === props.columnList.length);

    const handleToggleAll = () => {
        if (isAllColumn.value) {
            tempAllShowColumn.value = props.columnList.reduce((result, item) => {
                if (item.disabled) {
                    result.push(item.name);
                }
                return result;
            }, []);
        } else {
            tempAllShowColumn.value = props.columnList.map(item => item.name);
        }
    };

    const handleSubmitSetting = () => {
        emits('change', [...tempAllShowColumn.value]);
        emits('close');
    };

    const handleHideSetting = () => {
        emits('close');
    };
</script>
<style lang="postcss" scoped>
    .list-column-setting {
        position: absolute;
        top: 45px;
        left: 0;
        z-index: 1;
        width: 100%;
        background: #fff;
        box-shadow: 1px 1px 5px 0 #dcdee5;

        .select-body {
            padding: 15px 22px 30px;

            .title {
                margin-bottom: 22px;
                font-size: 16px;
                color: #313238;
            }
        }

        .select-column {
            display: inline-block;
            margin-top: 20px;
            margin-right: 36px;
            vertical-align: middle;

            &:last-child {
                margin-right: 0;
            }
        }

        .select-footer {
            display: flex;
            height: 50px;
            background: #fafbfd;
            border-top: 1px solid #dbdde4;
            align-items: center;
            justify-content: center;

            .bk-button {
                margin: 0 5px;
            }
        }
    }
</style>

<template>
    <div class="bk-ip-selector">
        <selector-box
            :is-show="showDialog"
            :value="selectorValue"
            @cancel="handleCancel"
            @change="handleValueChange" />
        <views-box
            v-if="showView"
            ref="viewsRef"
            :search-key="viewSearchKey"
            :value="selectorValue"
            @change="handleValueChange" />
    </div>
</template>
<script setup>
    import {
        onBeforeUnmount,
        onMounted,
        provide,
        reactive,
        ref,
        shallowRef,
        watch,
    } from 'vue';

    import { mergeLocalService } from './manager.js';
    import SelectorBox from './selector-box/index.vue';
    import { formatInput } from './utils/index';
    import ViewsBox from './views-box/index.vue';

    import './bk-icon/style.css';
    import './bk-icon/iconcool.js';
    import 'tippy.js/dist/tippy.css';
    import 'tippy.js/themes/light.css';

    const props = defineProps({
        showDialog: {
            type: Boolean,
            default: false,
        },
        showView: {
            type: Boolean,
            default: false,
        },
        value: {
            type: Object,
            validator: () => true,
        },
        originalValue: {
            type: Object,
        },
        showViewDiff: {
            type: Boolean,
            default: false,
        },
        viewSearchKey: {
            type: String,
        },
        readonly: {
            type: Boolean,
            default: false,
        },
        service: {
            type: Object,
            default: () => ({}),
        },
    });

    const emits = defineEmits([
        'change',
        'close-dialog',
    ]);

    const selectorValue = shallowRef({});
    const viewsRef = ref();

    watch(() => props.value, () => {
        console.log('from ip-selector watch value = ', props.value);
        selectorValue.value = props.value;
    }, {
        immediate: true,
    });

    const handleValueChange = (value) => {
        selectorValue.value = value;
        emits('change', value);
        emits('update:value', value);
        emits('update:modelValue', value);
        emits('close-dialog');
    };

    const handleCancel = () => {
        emits('close-dialog');
    };

    provide('BKIPSELECTOR', reactive({
        originalValue: props.originalValue && formatInput(props.originalValue),
        readonly: props.readonly,
    }));

    onMounted(() => {
        mergeLocalService(props.service);
    });
    onBeforeUnmount(() => {
        mergeLocalService({});
    });

    defineExpose({
        getHostIpList () {
            if (!viewsRef.value) {
                return [];
            }
            return viewsRef.value.getHostIpList();
        },
        getNotAlivelHostIpList () {
            if (!viewsRef.value) {
                return [];
            }
            return viewsRef.value.getNotAlivelHostIpList();
        },
        resetValue () {
            if (props.value) {
                return;
            }
            selectorValue.value = {};
        },
        refresh () {
            viewsRef.value && viewsRef.value.refresh();
        },
    });
</script>
<style lang="postcss">
    @keyframes bk-ip-selector-rotate-loading {
        0% {
            transform: rotateZ(0);
        }

        100% {
            transform: rotateZ(360deg);
        }
    }

    .bk-ip-selector {
        display: block;
    }

    .bk-ip-selector-number {
        font-weight: bold;
        color: #3a84ff;
    }

    .bk-ip-selector-number-error {
        font-weight: bold;
        color: #ea3636;
    }

    .bk-ip-selector-number-success {
        font-weight: bold;
        color: #2dcb56;
    }

    .bk-ip-selector-rotate-loading {
        display: flex;
        width: 20px;
        height: 20px;
        color: #3a84ff;
        align-items: center;
        justify-content: center;
        animation: bk-ip-selector-rotate-loading 1s linear infinite;
    }
</style>

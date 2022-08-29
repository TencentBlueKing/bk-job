<template>
    <div class="bk-ip-selector">
        <bk-button @click="handleShowDialog">
            添加服务器
        </bk-button>
        <selector-box
            :is-show="isShow"
            :value="selectorValue"
            @change="handleValueChange"
            @cancel="handleCancel" />
        <selector-view
            :value="selectorValue"
            @change="handleValueChange" />
    </div>
</template>
<script setup>
    import {
        ref,
        shallowRef,
        provide,
        toRefs,
        watch,
        reactive,
    } from 'vue';
    import './bk-icon/style.css';
    import './bk-icon/iconcool.js';
    import SelectorBox from './selector-box/index.vue';
    import SelectorView from './views-box/index.vue';

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
        showDiff: {
            type: Boolean,
            default: false,
        },
    });

    const emits = defineEmits(['change']);

    watch(() => props.value, () => {
        console.log('from ip-selector watch value = ', props.value);
    });

    const isShow = ref(false);
    const selectorValue = shallowRef({});

    const handleShowDialog = () => {
        isShow.value = true;
    };

    const handleValueChange = (value) => {
        console.log('fom handleValueChangehandleValueChange = ', value);
        selectorValue.value = value;
        isShow.value = false;
        emits('change', value);
        emits('update:value', value);
        emits('update:modelValue', value);
    };

    const handleCancel = () => {
        isShow.value = false;
    };

    provide('BKIPSELECTOR', reactive({
        ...toRefs(props),
    }));
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

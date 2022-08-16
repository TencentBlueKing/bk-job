<template>
    <div class="bk-ip-selector">
        <bk-button @click="handleShowDialog">
            添加服务器
        </bk-button>
        <selector-box :is-show="isShow" />
        <selector-view />
    </div>
</template>
<script setup>
    import { ref } from 'vue';
    import SelectorBox from './selector-box';
    import SelectorView from './selector-box/views/index.vue';

    const isShow = ref(false);

    const handleShowDialog = () => {
        isShow.value = true;
    };
</script>
<style lang="postcss">
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
</style>

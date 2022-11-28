<template>
    <div class="ip-selectore-render-host-menu-extends">
        <div
            v-for="(item ,index) in hostMemuExtends"
            :key="`${item}_${index}`"
            class="menu-item"
            @click="item.action">
            {{ item.name }}
        </div>
    </div>
</template>
<script setup>
    import Manager from '../manager';

    const { hostMemuExtends } = Manager.config;
</script>
<style lang="postcss">
    .ip-selectore-render-host-menu-extends {
        padding: 0 !important;
        color: #63656e !important;
        background: #fff !important;

        .menu-item {
            padding: 0 12px;

            &:hover {
                color: #3a84ff !important;
                background: #f5f6fa !important;
            }
        }
    }
</style>

<template>
    <div>
        <collapse-box>
            <template #title>
                <span style="font-weight: bold;">【动态拓扑】</span>
                <span>
                    - 共
                    <span class="bk-ip-selector-number">131231</span>
                    个，新增
                    <span class="bk-ip-selector-number-success">131231</span>
                    个，删除
                    <span class="bk-ip-selector-number-error">131231</span>
                    个
                </span>
            </template>
            【动态拓扑】
        </collapse-box>
    </div>
</template>
<script setup>
    import CollapseBox from './components/collapse-box/index.vue';
</script>
<style lang="postcss">
    .root {
        display: block;
    }
</style>

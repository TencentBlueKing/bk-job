<template>
    <div>
        <collapse-box>
            <template #title>
                <span style="font-weight: bold;">【静态拓扑】</span>
                <span>
                    - 共
                    <span class="bk-ip-selector-number">131231</span>
                    台，新增
                    <span class="bk-ip-selector-number-success">131231</span>
                    台，删除
                    <span class="bk-ip-selector-number-error">131231</span>
                    台
                </span>
            </template>
            【静态拓扑】
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

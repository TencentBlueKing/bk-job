<template>
    <div class="ip-selector-view-collsapn">
        <div
            class="box-header"
            @click="handleToggle">
            <ip-selector-icon
                class="arrow-flag"
                :class="{ 'is-show': isShow }"
                type="close" />
            <slot name="title" />
        </div>
        <div
            v-if="isShow"
            class="box-content">
            <slot />
        </div>
        <div class="box-action">
            <slot name="action" />
        </div>
    </div>
</template>
<script setup>
  import { ref } from 'vue';

  import IpSelectorIcon from '../../../common/ip-selector-icon';

  const isShow = ref(true);

  const handleToggle = () => {
    isShow.value = !isShow.value;
  };
</script>
<style lang="postcss" scoped>
    .ip-selector-view-collsapn {
        position: relative;

        .box-header {
            display: flex;
            height: 42px;
            padding: 0 16px;
            font-size: 12px;
            color: #63656e;
            cursor: pointer;
            align-items: center;
            background: #eff1f5;
            user-select: none;
        }

        .box-title {
            display: flex;
            align-items: center;

            .arrow-flag {
                margin-right: 4px;
                transform: rotateZ(90deg);
                transition: all 0.15s;

                &.is-show {
                    transform: rotateZ(0);
                }
            }
        }

        .box-content {
            background: #fff;
        }

        .box-action {
            position: absolute;
            top: 0;
            right: 12px;
            display: flex;
            align-items: center;
            height: 42px;
            margin-left: auto;
            cursor: pointer;
        }
    }
</style>

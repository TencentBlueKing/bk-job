<template>
    <div class="ip-selector-view-collsapn">
        <div
            class="box-header"
            @click="handleToggle">
            <ip-selector-icon
                class="arrow-flag"
                :class="{ 'is-show': isShow }"
                type="close" />
            <div class="box-header-text">
                <slot name="title" />
            </div>
        </div>
        <div
            v-show="isShow"
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

  import IpSelectorIcon from '../../common/ip-selector-icon';

  const props = defineProps({
    name: {
        type: String,
    },
  });

  const isShow = ref(true);

  const handleToggle = () => {
    isShow.value = !isShow.value;
  };

  defineExpose({
    toggle (lastStatus) {
        if (lastStatus === undefined) {
            handleToggle();
        } else if (lastStatus === false) {
            isShow.value = false;
        } else if (lastStatus === true) {
            isShow.value = true;
        } else if (Array.isArray(lastStatus) && props.name && lastStatus.includes(props.name)) {
            isShow.value = true;
        }
    },
  });
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

            .arrow-flag {
                margin-right: 16px;
                transform: rotateZ(0);
                transition: all 0.15s;

                &.is-show {
                    transform: rotateZ(90deg);
                }
            }
        }

        .box-header-text {
            display: flex;
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

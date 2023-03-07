<template>
  <div
    class="render-global-variable-box"
    :class="{ active }"
    @click="handleVariableSelect"
    @mouseenter="handleShowDetail"
    @mouseleave="handleHideDetail">
    <div
      ref="type"
      class="variable-type">
      <icon :type="data.icon" />
    </div>
    <div class="variable-name">
      {{ data.name }}
    </div>
    <div class="select-checked" />
    <div style="display: none;">
      <div
        ref="detail"
        class="batch-global-variable-popover-detail">
        <table>
          <tr>
            <td>{{ $t('template.变量名称：') }}</td>
            <td>{{ data.name }}</td>
          </tr>
          <tr>
            <td>{{ $t('template.变量类型：') }}</td>
            <td>{{ data.typeText }}</td>
          </tr>
          <tr>
            <td>{{ $t('template.初始值：') }}</td>
            <td>{{ data.valueText }}</td>
          </tr>
          <tr>
            <td>{{ $t('template.变量描述：') }}</td>
            <td>{{ data.description || '--' }}</td>
          </tr>
          <tr>
            <td>{{ $t('template.执行时必填：') }}</td>
            <td>{{ data.requiredText }}</td>
          </tr>
          <tr>
            <td>{{ $t('template.赋值可变：') }}</td>
            <td>{{ data.changeableText }}</td>
          </tr>
        </table>
      </div>
    </div>
  </div>
</template>
<script>
  import Tippy from 'bk-magic-vue/lib/utils/tippy';

  export default {
    props: {
      data: {
        type: Object,
        required: true,
      },
      active: {
        type: Boolean,
        default: false,
      },
    },
    data() {
      return {
        descPopover: {
          disable: true,
        },
        isShowDetail: false,
      };
    },
    mounted() {
      this.popperInstance = Tippy(this.$refs.type, {
        arrow: true,
        placement: 'bottom-start',
        theme: 'light',
        hideOnClick: true,
        animateFill: false,
        animation: 'slide-toggle',
        width: 330,
        lazy: false,
        ignoreAttributes: true,
        boundary: 'window',
        distance: 28,
        content: this.$refs.detail,
        zIndex: window.__bk_zIndex_manager.nextZIndex(), // eslint-disable-line no-underscore-dangle
      });
    },
    beforeDestroy() {
      if (this.popperInstance) {
        this.popperInstance.hide();
        this.popperInstance.destroy();
      }
    },
    methods: {
      handleVariableSelect() {
        this.$emit('select');
      },
      handleShowDetail() {
        this.popperInstance.show();
      },
      handleHideDetail() {
        this.popperInstance.hide();
      },
    },
  };
</script>
<style lang="postcss">
  .render-global-variable-box {
    position: relative;
    display: flex;
    width: 160px;
    height: 32px;
    padding-right: 26px;
    margin-top: 12px;
    margin-right: 5px;
    margin-left: 5px;
    color: #63656e;
    cursor: pointer;
    border: 1px solid #dcdee5;
    border-radius: 2px;
    transition: all 0.1s;
    align-items: center;

    &:hover {
      z-index: 1;
    }

    .variable-type {
      display: flex;
      align-items: center;
      justify-content: center;
      flex: 0 0 32px;
      height: 100%;
      margin-left: -1px;
      font-size: 17px;
      color: #fff;
      background: #d3d5db;
      transition: all 0.1s;
    }

    .select-checked {
      position: absolute;
      top: 9px;
      right: 6px;
      bottom: 0;
      width: 14px;
      height: 14px;
      background: #fff;
      border: 1px solid #c4c6cc;
      border-radius: 50%;
    }

    &.active {
      border-color: #3a84ff;

      .variable-type {
        background: #3a84ff;
      }

      .select-checked {
        background: #3a84ff;
        border-color: #3a84ff;

        &::after {
          position: absolute;
          top: 2px;
          left: 4px;
          width: 3px;
          height: 6px;
          border: 1px solid #fff;
          border-top: 0;
          border-left: 0;
          content: "";
          transform: rotate(45deg) scale(1);
          transition: all 0.1s;
          transform-origin: center;
        }
      }
    }

    .variable-name {
      padding-left: 6px;
      overflow: hidden;
      font-size: 13px;
      text-overflow: ellipsis;
      white-space: nowrap;
      flex: 0 1 auto;
      align-items: center;
    }
  }

  .batch-global-variable-popover-detail {
    table {
      td {
        font-size: 12px;
        line-height: 20px;
        color: #63656e;
        vertical-align: top;

        &:first-child {
          color: #b2b6be;
          text-align: right;
          word-break: keep-all;
        }
      }
    }
  }
</style>

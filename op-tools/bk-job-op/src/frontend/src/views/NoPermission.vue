<template>
  <div class="no-permission">
    <div class="no-permission-title">无访问权限</div>
    <div class="no-permission-desc">{{ message }}</div>
  </div>
</template>

<script>
  export default {
    name: 'NoPermission',
    computed: {
      message() {
        return this.$route.query.message || '当前用户不在OP系统白名单中，请联系管理员添加后重试。';
      },
    },
  };
</script>

<style>
  .no-permission {
    padding: 80px 0;
    text-align: center;
  }

  .no-permission-title {
    font-size: 20px;
    color: #313238;
  }

  .no-permission-desc {
    margin-top: 12px;
    color: #979ba5;
  }
</style>

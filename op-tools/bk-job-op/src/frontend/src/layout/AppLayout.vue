<template>
  <div class="app-layout">
    <aside class="app-nav">
      <div class="app-nav-title">BK-JOB OP</div>
      <nav class="app-nav-list">
        <router-link
          class="app-nav-item"
          to="/issue-status">
          Issue状态管理
        </router-link>
        <router-link
          class="app-nav-item"
          to="/pull-request">
          PR数据管理
        </router-link>
      </nav>
    </aside>
    <section class="app-main">
      <header class="app-header">
        <span
          v-if="username"
          class="app-header-user">
          {{ username }}
        </span>
      </header>
      <div class="app-content">
        <router-view />
      </div>
    </section>
  </div>
</template>

<script>
  import { getUserInfo } from '../service/user';

  export default {
    name: 'AppLayout',
    data() {
      return {
        username: '',
      };
    },
    mounted() {
      this.fetchUserInfo();
    },
    methods: {
      // 用户名只是辅助信息，取不到就不展示，不阻塞页面主体
      fetchUserInfo() {
        getUserInfo()
          .then((data) => {
            this.username = (data && data.username) || '';
          })
          .catch(() => {
            this.username = '';
          });
      },
    },
  };
</script>

<style>
  .app-layout {
    display: flex;
    height: 100%;
  }

  .app-nav {
    display: flex;
    flex: none;
    flex-direction: column;
    width: 260px;
    background: #182132;
  }

  .app-nav-title {
    height: 52px;
    font-size: 16px;
    line-height: 52px;
    color: #fff;
    text-align: center;
  }

  .app-nav-list {
    padding-top: 8px;
  }

  .app-nav-item {
    display: block;
    padding: 0 24px;
    font-size: 14px;
    line-height: 42px;
    color: #96a2b9;
  }

  .app-nav-item.router-link-active {
    color: #fff;
    background: #3a84ff;
  }

  .app-main {
    display: flex;
    flex: 1;
    flex-direction: column;
    min-width: 0;
  }

  .app-header {
    display: flex;
    flex: none;
    align-items: center;
    justify-content: flex-end;
    height: 52px;
    padding: 0 24px;
    background: #fff;
    box-shadow: 0 2px 4px 0 rgb(0 0 0 / 6%);
  }

  .app-header-user {
    font-size: 14px;
    color: #63656e;
  }

  .app-content {
    flex: 1;
    padding: 20px 24px;
    overflow: auto;
  }
</style>

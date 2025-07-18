<!--
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
-->

<template>
  <div
    ref="appRef"
    class="job-app-select"
    :class="{ focus: isFocus }">
    <div>
      <div
        v-if="showIcon"
        class="app-icon">
        {{ valueIcon }}
      </div>
      <template v-else>
        <input
          class="app-name"
          readonly
          :value="currentScopeId ? `${currentScopeName} (${currentScopeId})` : ''"
          @keydown.down.prevent="handleStep('next')"
          @keydown.enter.prevent="handleSelect"
          @keydown.up.prevent="handleStep('prev')">
        <i class="bk-icon icon-angle-down panel-arrow" />
      </template>
    </div>
    <div style="display: none;">
      <div
        ref="panelRef"
        class="app-panel">
        <div class="app-search">
          <input
            ref="searchRef"
            v-model="keyword"
            :placeholder="$t('关键字')"
            spellcheck="false"
            @input="handleInputChange"
            @keydown.down.prevent="handleStep('next')"
            @keydown.enter.prevent="handleSelect"
            @keydown.up.prevent="handleStep('prev')">
          <i class="bk-icon icon-search app-search-flag" />
        </div>
        <div
          ref="listRef"
          class="app-list">
          <auth-component
            v-for="(app, index) in renderPaginationData"
            :key="app.id"
            auth="biz/access_business"
            class="app-item"
            :class="{
              active: app.scopeType === currentScopeType && app.scopeId === currentScopeId,
              hover: index === activeIndex,
            }"
            :permission="app.hasPermission"
            :resource-id="app.scopeId"
            :scope-id="app.scopeId"
            :scope-type="app.scopeType">
            <div
              @click="handleAppChange(app)"
              @mouseenter.self="handleMouseenter(index)">
              <div class="app-wrapper">
                <span class="app-name">{{ app.name }}</span>
                <span class="app-id">({{ app.scopeId }})</span>
              </div>
              <div class="app-collection">
                <icon
                  v-if="app.favor"
                  class="favor"
                  svg
                  type="collection"
                  @click.stop="handleFavor(app.scopeType, app.scopeId, false)" />
                <icon
                  v-else
                  class="unfavor"
                  svg
                  type="star-line"
                  @click.stop="handleFavor(app.scopeType, app.scopeId, true)" />
              </div>
            </div>
            <div
              slot="forbid"
              class="app-wrapper">
              <span class="app-name">{{ app.name }}</span>
              <span class="app-id">(#{{ app.scopeId }})</span>
            </div>
          </auth-component>
          <div ref="loadingPlaceholderRef" />
          <div
            v-if="renderList.length < 1"
            class="app-list-empty">
            {{ $t('无匹配数据') }}
          </div>
        </div>
        <div
          key="create"
          class="app-create"
          @click="handleGoCreateApp">
          <i class="bk-icon icon-plus-circle mr10" />{{ $t('新建业务') }}
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
  import pinyin from 'bk-magic-vue/lib/utils/pinyin';
  import Tippy from 'bk-magic-vue/lib/utils/tippy';
  import _ from 'lodash';
  import { computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef } from 'vue';

  import { useRoute, useRouter } from '@router';

  import AppManageService from '@service/app-manage';
  import QueryGlobalSettingService from '@service/query-global-setting';

  import { messageSuccess } from '@common/bkmagic';

  import I18n from '@/i18n';
  import {
    encodeRegexp,
    prettyDateTimeFormat,
  } from '@/utils/assist';
  import {
    scopeCache,
  } from '@/utils/cache-helper';

  import usePagination from './usePagination';

  defineProps({
    showIcon: {
      type: Boolean,
      default: false,
    },
  });

  const currentRoute = useRoute();
  const router = useRouter();

  const getTransformInfo = (text) => {
    const sentence = [];
    const head = [];
    let word = [];
    const parseArr = pinyin.parse(text);
    parseArr.forEach((target) => {
      if (target.type === 2) {
        if (word.length > 0) {
          head.push(word[0]);
          sentence.push(word.join(''));
          word = [];
        }
        head.push(target.target[0]);
        sentence.push(target.target);
      } else {
        word.push(target.target);
      }
    });
    if (word.length > 0) {
      head.push(word[0]);
      sentence.push(word.join(''));
    }
    return {
      sentence: sentence.join(''),
      head: head.join(''),
    };
  };

  const sortAPPList = (list) => {
    const favorList = [];
    const unfavorList = [];

    list.forEach((item) => {
      if (item.favor) {
        favorList.push(item);
      } else {
        unfavorList.push(item);
      }
    });
    favorList.sort((pre, next) => pre.favorTime > next.favorTime);
    unfavorList.sort((pre, next) => pre.id < next.id);

    return favorList.concat(unfavorList);
  };

  let list = [];

  const appRef = ref();
  const panelRef = ref();
  const listRef = ref();
  const searchRef = ref();
  const loadingPlaceholderRef = ref();
  const isFocus = ref(false);
  const renderList = shallowRef([]);
  const currentScopeType = window.PROJECT_CONFIG.SCOPE_TYPE;
  const currentScopeId = window.PROJECT_CONFIG.SCOPE_ID;
  const currentScopeName = ref('');
  const activeIndex = ref(-1);
  const keyword = ref('');
  const relatedSystemUrls = ref({
    BK_CMDB_ROOT_URL: '',
  });

  const { data: renderPaginationData } = usePagination(listRef, loadingPlaceholderRef, renderList);

  const valueIcon = computed(() => currentScopeName.value.slice(0, 1));

  QueryGlobalSettingService.fetchRelatedSystemUrls()
    .then((data) => {
      relatedSystemUrls.value = data;
    });

  AppManageService.fetchWholeAppList()
    .then((data) => {
      list = data.data.map(item => ({
        ...item,
        ...getTransformInfo(item.name),
      }));

      renderList.value = [...list];
      // eslint-disable-next-line no-plusplus
      for (let i = 0; i < list.length; i++) {
        const {
          scopeType,
          scopeId,
          name,
        } = list[i];
        if (scopeType === currentScopeType && scopeId === currentScopeId) {
          activeIndex.value = i;
          currentScopeName.value = name;
          break;
        }
      }
    });

  const handleGoCreateApp = () => {
    if (!relatedSystemUrls.value.BK_CMDB_ROOT_URL) {
      alert(I18n.t('网络错误，请刷新页面重试'));
      return;
    }
    window.open(`${relatedSystemUrls.value.BK_CMDB_ROOT_URL}/#/resource/business`);
  };

  const handleStep = (step) => {
    if (step === 'next') {
      activeIndex.value += 1;
      if (activeIndex.value === renderList.value.length) {
        activeIndex.value = 0;
      }
    } else if (step === 'prev') {
      activeIndex.value -= 1;
      if (activeIndex.value < 0) {
        activeIndex.value = renderList.value.length - 1;
      }
    }
    nextTick(() => {
      const wraperHeight = listRef.value.getBoundingClientRect().height;
      const activeOffsetTop = listRef.value.querySelector('.hover').offsetTop + 32;

      if (activeOffsetTop > wraperHeight) {
        listRef.value.scrollTop = activeOffsetTop - wraperHeight + 10;
      } else if (activeOffsetTop <= 42) {
        listRef.value.scrollTop = 0;
      }
    });
  };

  const handleMouseenter = (index) => {
    activeIndex.value = index;
  };

  const handleSelect = () => {
    listRef.value.querySelector('.hover').click();
  };

  const handleInputChange = _.debounce(() => {
    const query = _.trim(keyword.value);
    let nextRenderList = [];
    if (!query) {
      nextRenderList = [...list];
    } else {
      const rule = new RegExp(encodeRegexp(query), 'i');
      if (/[\u4e00-\u9fa5]/.test(query)) {
        nextRenderList = _.filter(list, _ => rule.test(_.name));
      } else {
        nextRenderList = _.filter(list, _ => rule.test(_.head)
          || rule.test(_.sentence)
          || rule.test(_.scopeId));
      }
    }
    renderList.value = nextRenderList;
    activeIndex.value = 0;
  }, 100);

  const handleFavor = (scopeType, scopeId, favor) => {
    const app = _.find(list, _ => _.scopeType === scopeType && _.scopeId === scopeId);
    if (favor) {
      AppManageService.favorApp({
        scopeType,
        scopeId,
      }).then(() => {
        app.favor = true;
        app.favorTime = prettyDateTimeFormat(Date.now());
        renderList.value = [...renderList.value];
        messageSuccess(I18n.t('收藏成功'));
      });
    } else {
      AppManageService.cancelFavorApp({
        scopeType,
        scopeId,
      }).then(() => {
        app.favor = false;
        renderList.value = [...renderList.value];
        messageSuccess(I18n.t('取消收藏成功'));
      });
    }
  };

  const handleAppChange = (appInfo) => {
    const {
      scopeType,
      scopeId,
    } = appInfo;

    const pathRoot = `/${scopeType}/${scopeId}`;
    if (!window.PROJECT_CONFIG.SCOPE_TYPE || !window.PROJECT_CONFIG.SCOPE_ID) {
      window.location.href = pathRoot;
      return;
    }

    scopeCache.setItem({
      scopeType,
      scopeId,
    });
    const reload = (targetPath) => {
      setTimeout(() => {
        const path = targetPath.replace(/^\/[^/]+\/\d+/, pathRoot);
        window.location.href = path;
      }, 100);
    };
    // 1，当前路由不带参数，切换业务时停留在当前页面
    let currentRouteHasNotParams = true;
    for (const paramKey in currentRoute.value.params) {
      if (currentRoute.value.params[paramKey] === undefined || currentRoute.value.params[paramKey] === null) {
        break;
      }
      currentRouteHasNotParams = false;
    }
    if (currentRouteHasNotParams) {
      reload(currentRoute.value.path);
      return;
    }
    const { matched } = currentRoute.value;
    // 2，当前路由带有请求参数，切换业务时则需要做回退处理
    // 路由只匹配到了一个
    if (matched.length < 2) {
      const [{ path }] = matched;
      reload(path);
      return;
    }

    // 路由有多层嵌套
    /* eslint-disable prefer-destructuring */
    const { path, redirect } = matched[1];
    // 重定向到指定的路由path
    if (_.isString(redirect)) {
      reload(redirect);
      return;
    }
    // 重定向到指定的路由name
    if (_.isPlainObject(redirect) && redirect.name) {
      const route = router.resolve({
        name: redirect.name,
      });
      reload(route.href);
      return;
    }
    reload(path);
  };

  let popperInstance;

  onMounted(() => {
    if (!popperInstance) {
      popperInstance = Tippy(appRef.value, {
        theme: 'light app-list',
        arrow: false,
        interactive: true,
        animateFill: false,
        placement: 'bottom-start',
        content: panelRef.value,
        trigger: 'click',
        distance: 20,
        width: '320px',
        size: 'small',
        appendTo: document.querySelector('.jb-navigation-side'),
        zIndex: window.__bk_zIndex_manager.nextZIndex(), // eslint-disable-line no-underscore-dangle
        onShow: () => {
          isFocus.value = true;
          setTimeout(() => {
            searchRef.value.focus();
          });
        },
        onHidden: () => {
          isFocus.value = false;
          keyword.value = '';
          list = sortAPPList(list);
          handleInputChange();
        },
      });
    }
  });

  onBeforeUnmount(() => {
    popperInstance.destroy();
  });
</script>
<style lang='postcss'>
  .job-app-select {
    position: relative;
    color: #dcdee5;
    background: #f0f1f5;
    border: none;
    border-radius: 2px;

    &.focus {
      .panel-arrow {
        transform: rotateZ(-180deg);
      }
    }

    .app-icon {
      height: 30px;
      line-height: 30px;
      color: inherit;
      text-align: center;
      cursor: pointer;
      background: inherit;
      border: none;
      outline: none;
    }

    .app-name {
      width: 100%;
      height: 30px;
      padding: 0 10px;
      line-height: 30px;
      color: inherit;
      cursor: pointer;
      background: inherit;
      border: none;
      outline: none;

      &::placeholder {
        color: #c4c6cc;
      }
    }

    .panel-arrow {
      position: absolute;
      top: 4px;
      right: 2px;
      font-size: 22px;
      color: rgb(151 155 165);
      pointer-events: none;
      transition: all 0.15s;
    }
  }

  .app-list-theme {
    padding: 0 !important;
    background: #182233 !important;
    border: 1px solid #2f3847;
    box-shadow: 0 2px 3px 0 rgb(0 0 0 / 10%) !important;

    .component-permission-disabled {
      color: #70737a !important;

      * {
        color: #70737a !important;
      }
    }

    .app-panel {
      overflow: hidden;
      color: #c4c6cc;
    }

    .app-search {
      position: relative;
      padding: 0 10px;

      input {
        width: 100%;
        height: 32px;
        padding: 0 10px 0 30px;
        line-height: 32px;
        background: transparent;
        border: none;
        border-bottom: 1px solid #404a5c;
        outline: none;

        &::placeholder {
          color: #747e94;
        }
      }

      .app-search-flag {
        position: absolute;
        top: 8px;
        left: 10px;
        font-size: 16px;
        color: #979ba5;
      }
    }

    .app-list {
      position: relative;
      max-height: 238px;
      margin-top: 8px;
      margin-bottom: 8px;
      overflow-y: auto;

      &::-webkit-scrollbar {
        width: 6px;
      }

      &::-webkit-scrollbar-thumb {
        background-color: #5f6e85;
        border-radius: 3px;
      }
    }

    .app-list-empty {
      text-align: center;
    }

    .app-item {
      display: flex;
      height: 32px;
      padding: 0 16px 0 10px;
      line-height: 32px;
      cursor: pointer;
      transition: all 0.1s;
      align-items: center;

      &:hover,
      &.hover {
        color: #f0f1f5;
        background: #294066;
      }

      &:hover {
        .app-collection {
          .unfavor {
            opacity: 100%;
          }
        }
      }

      &.active {
        color: #f0f1f5;
        background-color: #2d3542;
      }

      .app-wrapper {
        display: flex;
        overflow: hidden;
      }

      .app-name {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        flex: 0 1 auto;
      }

      .app-id {
        padding-left: 4px;
        color: #979ba5;
        flex: 0 0 auto;
      }

      .app-collection {
        display: flex;
        margin-left: auto;
        font-size: 16px;
        color: #979ba5;
        align-items: center;

        .favor {
          color: #ffb848;
        }

        .unfavor {
          opacity: 0%;
          transition: all 0.1s;
        }
      }
    }

    .app-create {
      display: flex;
      height: 33px;
      padding: 0 10px;
      color: #c4c6cc;
      cursor: pointer;
      background: #28354d;
      border-radius: 0 0 1px 1px;
      align-items: center;
    }
  }
</style>

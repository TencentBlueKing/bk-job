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
          :value="currentScopeId ? `${currentScopeName} (${currentScopeId})` : ''">
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
            :placeholder="$t('关键字')"
            spellcheck="false"
            :value="keyword"
            @input="handleInputChange">
          <i class="bk-icon icon-search app-search-flag" />
        </div>
        <div
          ref="listRef"
          class="app-list"
          :style="{
            'max-height': `${238 + scopeGroupData.length * 32}px`
          }">
          <template v-for="(app, index) in renderPaginationData">
            <div
              v-if="!app.groupId"
              :key="index"
              class="group-item"
              :class="{
                'is-expanded': expandScopeGroupMap[app.id]
              }"
              @click="() => handleExpandGroup(app.id)">
              <icon type="arrow-full-right" />
              <span style="margin-left: 8px">{{ app.name }}</span>
              <span class="group-children-count">{{ groupChildrenCountMap[app.id] || 0 }}</span>
            </div>
            <auth-component
              v-else
              :key="index"
              auth="biz/access_business"
              class="app-item is-scope"
              :class="{
                active: app.groupId === currentScopeType && app.id === currentScopeId,
              }"
              :permission="app.data.hasPermission"
              :resource-id="app.id"
              :scope-id="app.id"
              :scope-type="app.groupId">
              <div
                @click="handleAppChange(app)">
                <div class="app-wrapper">
                  <span class="app-name">{{ app.name }}</span>
                  <span class="app-id">({{ app.id }})</span>
                </div>
                <div class="app-collection">
                  <icon
                    v-if="app.data.favor"
                    class="favor"
                    svg
                    type="collection"
                    @click.stop="handleFavor(app.groupId, app.id, false)" />
                  <icon
                    v-else
                    class="unfavor"
                    svg
                    type="star-line"
                    @click.stop="handleFavor(app.groupId, app.id, true)" />
                </div>
              </div>
              <div
                slot="forbid"
                class="app-wrapper">
                <span class="app-name">{{ app.name }}</span>
                <span class="app-id">(#{{ app.id }})</span>
              </div>
            </auth-component>
          </template>
          <div ref="loadingPlaceholderRef" />
          <div
            v-if="filterList.length < 1"
            class="app-list-empty">
            {{ $t('无匹配数据') }}
          </div>
        </div>
        <div
          key="operation"
          class="footer-operation">
          <div
            class="operation-item"
            @click="handleGoCreateApp">
            <i class="bk-icon icon-plus-circle mr10" />{{ $t('去新建') }}
          </div>
          <div
            v-bk-tooltips="{
              content: $t('请联系业务运维加入业务'),
              disabled: canApply,
            }"
            class="operation-item"
            @click="handleGoApplyApp">
            <i class="bk-icon icon-plus-circle mr10" />{{ $t('去申请') }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
  import pinyin from 'bk-magic-vue/lib/utils/pinyin';
  import Tippy from 'bk-magic-vue/lib/utils/tippy';
  import _ from 'lodash';
  import { computed, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue';

  import { useRoute, useRouter } from '@router';

  import AppManageService from '@service/app-manage';
  import QueryGlobalSettingService from '@service/query-global-setting';

  import { messageSuccess } from '@common/bkmagic';

  import I18n from '@/i18n';
  import {
    encodeRegexp,
  } from '@/utils/assist';
  import {
    scopeCache,
  } from '@/utils/cache-helper';

  import useGroup from './useGroup';
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
  const isShowSelectPanel = ref(false);
  const scopeGroupData = shallowRef([]);
  const currentScopeType = window.PROJECT_CONFIG.SCOPE_TYPE;
  const currentScopeId = window.PROJECT_CONFIG.SCOPE_ID;
  const currentScopeName = ref('');
  const canApply = ref(false);
  const applyUrl = ref('/');
  const keyword = ref('');
  const relatedSystemUrls = ref({
    BK_CMDB_ROOT_URL: '',
  });
  const expandScopeGroupMap = shallowRef({});
  const groupChildrenCountMap = shallowRef({});
  const filterList = shallowRef([]);

  const valueIcon = computed(() => currentScopeName.value.slice(0, 1));
  // const filterList = computed(() => {
  //   const keywordStr = _.trim(keyword.value);
  //   const rule = new RegExp(encodeRegexp(keywordStr), 'i');
  //   const isExactMatch = /[\u4e00-\u9fa5]/.test(keywordStr);
  //   return scopeGroupData.value.reduce((result, groupItem) => {
  //     result.push({
  //       id: groupItem.id,
  //       name: groupItem.name,
  //       groupId: undefined,
  //       data: undefined,
  //     });
  //     if (expandScopeGroupMap.value[groupItem.id]) {
  //       groupItem.children.forEach((item) => {
  //         if (
  //           (!keywordStr)
  //           || (isExactMatch && rule.test(item.name))
  //           || rule.test(item.headLetter)
  //           || rule.test(item.sentence)
  //           || rule.test(`${item.id}`)
  //         ) {
  //           result.push({
  //             id: item.id,
  //             name: item.name,
  //             groupId: groupItem.id,
  //             data: item,
  //           });
  //         }
  //       });
  //     }

  //     return result;
  //   }, []);
  // });

  watch([keyword, scopeGroupData, expandScopeGroupMap], () => {
    const keywordStr = _.trim(keyword.value);
    const rule = new RegExp(encodeRegexp(keywordStr), 'i');
    const isExactMatch = /[\u4e00-\u9fa5]/.test(keywordStr);
    const filterListResult = [];
    const childrenCountMap = {};
    scopeGroupData.value.forEach((groupItem) => {
      filterListResult.push({
        id: groupItem.id,
        name: groupItem.name,
        groupId: undefined,
        data: undefined,
      });
      childrenCountMap[groupItem.id] = 0;
      groupItem.children.forEach((item) => {
        if (
          (!keywordStr)
          || (isExactMatch && rule.test(item.name))
          || rule.test(item.headLetter)
          || rule.test(item.sentence)
          || rule.test(`${item.id}`)
        ) {
          if (expandScopeGroupMap.value[groupItem.id]) {
            filterListResult.push({
              id: item.id,
              name: item.name,
              groupId: groupItem.id,
              data: item,
            });
          }
          childrenCountMap[groupItem.id] += 1;
        }
      });
    });
    filterList.value = filterListResult;
    groupChildrenCountMap.value = childrenCountMap;
  }, {
    immediate: true,
  });

  const { data: renderPaginationData } = usePagination(listRef, loadingPlaceholderRef, filterList);
  useGroup(listRef, expandScopeGroupMap, filterList, isShowSelectPanel);

  QueryGlobalSettingService.fetchRelatedSystemUrls()
    .then((data) => {
      relatedSystemUrls.value = data;
    });

  const fetchGroupPanel = () => {
    AppManageService.fetchGroupPanel()
      .then((data) => {
        applyUrl.value = data.applyUrl;
        canApply.value = data.canApply;

        const result = data.scopeGroupList.map(item => ({
          ...item,
          children: item.children.map(item => ({
            ...item,
            ...getTransformInfo(item.name),
          })),
        }));
        scopeGroupData.value = result;
        if (result.length > 0) {
          for (const groupItem of result) {
            if (groupItem.id === currentScopeType) {
              for (const scopeItem of groupItem.children) {
                if (scopeItem.id === currentScopeId) {
                  currentScopeName.value = scopeItem.name;
                  window.PROJECT_CONFIG.BUSINESS_TIME_ZONE = scopeItem.timeZone;
                  break;
                }
              }
            }
          }
          // 默认展开有权限的第一个业务组
          if (Object.keys(expandScopeGroupMap.value).length < 1) {
            // 业务有权限——优先展开业务组
            const bizGroup = result.find(item => item.id === 'biz');
            if (bizGroup && _.some(bizGroup?.children || [], item => item.hasPermission)) {
              expandScopeGroupMap.value = {
                [bizGroup.id]: true,
              };
              return;
            }
            // 业务没有权限，业务级有权限——其次展开业务集
            const bizSetGroup = result.find(item => item.id === 'biz_set');
            if (bizSetGroup && _.some(bizSetGroup?.children || [], item => item.hasPermission)) {
              expandScopeGroupMap.value = {
                [bizSetGroup.id]: true,
              };
              return;
            }
            // 业务级也没有权限——默认展开第一个业务组
            expandScopeGroupMap.value = {
              [bizGroup ? bizGroup.id : result[0].id]: true,
            };
          }
        }
      });
  };
  const fetchGroupApply = () => {
    AppManageService.fetchGroupPanel()
      .then((data) => {
        applyUrl.value = data.applyUrl;
        canApply.value = data.canApply;
      });
  };

  fetchGroupPanel();

  const handleExpandGroup = (groupId) => {
    const latestScopeExpandGroup = { ...expandScopeGroupMap.value };
    if (latestScopeExpandGroup[groupId]) {
      delete latestScopeExpandGroup[groupId];
    } else {
      latestScopeExpandGroup[groupId] = true;
    }
    expandScopeGroupMap.value = latestScopeExpandGroup;
  };
  const handleGoCreateApp = () => {
    if (!relatedSystemUrls.value.BK_CMDB_ROOT_URL) {
      alert(I18n.t('网络错误，请刷新页面重试'));
      return;
    }
    window.open(`${relatedSystemUrls.value.BK_CMDB_ROOT_URL}/#/resource/business`);
  };

  const handleGoApplyApp = () => {
    if (!canApply.value) {
      return;
    }
    window.open(applyUrl.value);
  };

  const handleInputChange = _.debounce((event) => {
    keyword.value = _.trim(event.target.value);
  }, 100);

  const handleFavor = (scopeType, scopeId, favor) => {
    if (favor) {
      AppManageService.favorApp({
        scopeType,
        scopeId,
      }).then(() => {
        fetchGroupPanel();
        messageSuccess(I18n.t('收藏成功'));
      });
    } else {
      AppManageService.cancelFavorApp({
        scopeType,
        scopeId,
      }).then(() => {
        fetchGroupPanel();
        messageSuccess(I18n.t('取消收藏成功'));
      });
    }
  };

  const handleAppChange = (appInfo) => {
    const {
      groupId: scopeType,
      id: scopeId,
    } = appInfo;

    const pathRoot = `${window.PROJECT_CONFIG.BK_SITE_PATH}${scopeType}/${scopeId}`;
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
        const path = targetPath.replace(new RegExp(`^${window.PROJECT_CONFIG.BK_SITE_PATH}[^/]+/\\d+`), pathRoot);
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
          fetchGroupApply();
          setTimeout(() => {
            searchRef.value.focus();
            isShowSelectPanel.value = true;
          }, 100);
        },
        onHidden: () => {
          isFocus.value = false;
          keyword.value = '';
          isShowSelectPanel.value = false;
          list = sortAPPList(list);
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

        &:focus{
          border-color: #3a84ff;
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

    .group-item,
    .app-item {
      display: flex;
      height: 32px;
      padding: 0 16px 0 10px;
      cursor: pointer;
      background: #182233;
      transition: all 0.1s;
      align-items: center;
    }

    .group-item{
      user-select: none;

      &.is-expanded{
        .job-icon-arrow-full-right{
          transform: rotateZ(90deg);
          transition: all .15s;
        }
      }

      .group-children-count{
        height: 20px;
        padding: 0 8px;
        margin-left: 4px;
        line-height: 20px;
        background: #294066;
        border-radius: 2px;
      }
    }

    .app-item{
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

    .footer-operation {
      display: flex;
      height: 33px;
      color: #c4c6cc;
      background: #28354d;
      border-radius: 0 0 1px 1px;
      user-select: none;

      .operation-item{
        position: relative;
        display: flex;
        cursor: pointer;
        flex: 1;
        align-items: center;
        justify-content: center;

        & ~ .operation-item{
          &::before{
            position: absolute;
            left: 1px;
            width: 1px;
            height: 16px;
            background: #C4C6CC;
            content: '';
          }
        }
      }
    }
  }
</style>

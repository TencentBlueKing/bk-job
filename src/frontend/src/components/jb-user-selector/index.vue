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
  <bk-user-selector
    v-if="!isLoading"
    v-model="selectedUsers"
    :api-base-url="apiBaseUrl"
    :exclude-user-ids="excludeUserList"
    multiple
    :placeholder="placeholder"
    :render-list-item="renderTag"
    :render-tag="renderTag"
    :tenant-id="tenantId"
    :user-group="roleList"
    :user-group-name="t('角色')"
    @change="handleChange" />
</template>
<script setup>
  import _ from 'lodash';
  import { computed, ref, shallowRef, watch } from 'vue';

  import NotifyService from '@service/notify';
  import QueryGlobalSettingService from '@service/query-global-setting';
  import UserService from '@service/user';

  import { makeMap } from '@utils/assist';

  import BkUserSelector from '@blueking/bk-user-selector/vue2';

  import { useI18n } from '@/i18n';

  import '@blueking/bk-user-selector/vue2/vue2.css';

  const props = defineProps({
    // 已选 user
    user: {
      type: Array,
      default: () => [],
    },
    // 已选 role
    role: {
      type: Array,
      default: () => [],
    },
    // 支持选择角色
    showRole: {
      type: Boolean,
      default: true,
    },
    // 排除待选的角色
    excludeRoleList: {
      type: Array,
      default: () => [],
    },
    excludeUserList: {
      type: Array,
      default: () => [],
    },
    placeholder: {
      type: String,
    },
  });

  const emits = defineEmits(['on-change']);


  const { t } = useI18n();

  const isLoading = ref(true);
  const tenantId = ref('');
  const apiBaseUrl = ref('');

  const selectedUsers = ref([]);

  const roleList = shallowRef([]);

  const roleIdMap = computed(() => roleList.value.reduce((result, item) => Object.assign(result, {
    [item.id]: item.name,
  }), {}));

  watch(() => props.showRole, () => {
    if (!props.showRole) {
      roleList.value = [];
      return;
    }
    NotifyService.fetchRoleList()
      .then((data) => {
        const excludeRoleMap = makeMap(props.excludeRoleList);
        roleList.value = data.map(item => ({
          id: item.code,
          name: item.name,
          hidden: excludeRoleMap[item.code],
        }));
      });
  }, {
    immediate: true,
  });

  watch(() => [props.user, props.role], () => {
    const excludeRoleMap = makeMap(props.excludeRoleList);

    const roleList = _.filter(props.role, item => !excludeRoleMap[item]);
    selectedUsers.value = [...roleList, ...props.user];
  }, {
    immediate: true,
  });

  Promise.all([
    QueryGlobalSettingService.fetchRelatedSystemUrls(),
    UserService.fetchUserInfo(),
  ]).then(([relatedSystemUrls, useInfo]) => {
    tenantId.value = useInfo.tenantId;
    apiBaseUrl.value = relatedSystemUrls.BK_USER_WEB_API_ROOT_URL;
  })
    .finally(() => {
      isLoading.value = false;
    });


  const renderTag = (h, userInfo) => {
    if (userInfo.type === 'userGroup') {
      return h('span', [
        h('i', {
          class: 'job-icon job-icon-user-group-gray',
          style: 'margin-right: 2px; font-size: 14px; color: #979ba5;',
        }),
        userInfo.name,
      ]);
    }
    return h('span', [
      h('i', {
        class: 'job-icon job-icon-user',
        style: 'margin-right: 2px; font-size: 14px; color: #979ba5;',
      }),
      userInfo.name,
    ]);
  };


  const handleChange = (valueList) => {
    const role = [];
    const user = [];
    valueList.forEach((item) => {
      if (roleIdMap.value[item]) {
        role.push(item);
      } else {
        user.push(item);
      }
    });

    emits('on-change', user, role);
  };
</script>

<template>
  <div class="headerPage relative overflow-hidden h-full">
    <div
      class="text-black float-left font-bold cursor-pointer pt-1"
      @click="mainStoreData.mainStatus = !mainStoreData.mainStatus"
    >
      <svg
        :class="{ 'is-active': !mainStoreData.mainStatus }"
        class="hamburger"
        viewBox="0 0 1024 1024"
        xmlns="http://www.w3.org/2000/svg"
        width="64"
        height="64"
      >
        <path
          d="M408 442h480c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8H408c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8zm-8 204c0 4.4 3.6 8 8 8h480c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8H408c-4.4 0-8 3.6-8 8v56zm504-486H120c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8h784c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8zm0 632H120c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8h784c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8zM142.4 642.1L298.7 519a8.84 8.84 0 0 0 0-13.9L142.4 381.9c-5.8-4.6-14.4-.5-14.4 6.9v246.3a8.9 8.9 0 0 0 14.4 7z"
        />
      </svg>
    </div>
    <div class="float-left pl-4 pt-2.5">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item v-if="router.currentRoute.value.path == '/'" :to="{ path: '/' }">
          {{ $t(router.currentRoute.value.meta.title as string) }}
        </el-breadcrumb-item>
        <template v-else>
          <el-breadcrumb-item v-for="(item, index) in navArray" :key="index">{{
            $t(item.meta.title)
          }}</el-breadcrumb-item>
        </template>
      </el-breadcrumb>
    </div>
    <div class="float-right mt-1">
      <!-- 添加语言切换 -->
      <el-dropdown class="mr-8">
        <span class="el-dropdown-link text-sm text-slate cursor-pointer leading-7">
          <span>{{ $t('domain.language') }}</span>
          <i-ic-outline-arrow-drop-down class="text-xl inline-block align-middle"></i-ic-outline-arrow-drop-down>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="changeLanguage('en')">English</el-dropdown-item>
            <el-dropdown-item @click="changeLanguage('zh-CN')">中文</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-dropdown>
        <span class="el-dropdown-link text-base text-slate cursor-pointer leading-7">
          {{ mainStoreData.userInfo ? mainStoreData.userInfo.name : '' }}
          <i-ic-outline-arrow-drop-down class="text-xl inline-block align-middle"></i-ic-outline-arrow-drop-down>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item divided>
              <span @click="openModifyPassword">
                <i-ep:edit class="text-lg inline-block align-top mr-2"></i-ep:edit>
                {{ $t('components.layout.header.modifyPassword') }}
              </span>
            </el-dropdown-item>
            <el-dropdown-item divided>
              <span @click="toLogout">
                <i-clarity-logout-line class="text-lg inline-block align-top mr-2"></i-clarity-logout-line>
                {{ $t('components.layout.header.logout') }}
              </span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
    <el-dialog
      v-model="modifyPasswordDialog"
      :title="$t('components.layout.header.modifyPassword')"
      width="600px"
      destroy-on-close
      @close="modifyPasswordDialogClose"
    >
      <el-form :model="modifyPasswordForm" :rules="modifyPasswordRules" label-width="145px">
        <el-form-item :label="$t('components.layout.header.oldPassword') + ':'" prop="oldPassword">
          <el-input
            v-model="modifyPasswordForm.oldPassword"
            type="password"
            show-password
            autocomplete="off"
            :placeholder="$t('components.layout.header.oldPasswordPlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="$t('components.layout.header.newPassword') + ':'" prop="newPassword">
          <el-input
            v-model="modifyPasswordForm.newPassword"
            type="password"
            show-password
            autocomplete="off"
            :placeholder="$t('components.layout.header.newPasswordPlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="$t('components.layout.header.confirmPassword') + ':'" prop="confirmPassword">
          <el-input
            v-model="modifyPasswordForm.confirmPassword"
            type="password"
            show-password
            autocomplete="off"
            :placeholder="$t('components.layout.header.confirmPasswordPlaceholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button type="primary" @click="modifyPassword">
            {{ $t('common.confirm') }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';

import Cookies from 'js-cookie';

import loginApi from '@/api/modules/login';
import mainApi from '@/api/modules/main';
import mainStore from '@/store/mainStore';

const { proxy }: any = getCurrentInstance();
const { locale } = useI18n();
const route = useRoute();
const router = useRouter();
const mainStoreData = mainStore();
const navArray: any = ref([]);

const modifyPasswordDialog: any = ref(false);
const modifyPasswordForm: any = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});
const validatePass = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error(proxy.$t('components.layout.header.validate.password')));
  } else {
    const reg = /(?=.*[a-zA-Z])(?=.*\d)(?=.*[~!@#$%^&*()_+\-=}{:";<>?,./]).{6,18}$/;
    if (!reg.test(value)) {
      callback(new Error(proxy.$t('components.layout.header.validate.passwordLength')));
    } else {
      callback();
    }
  }
};
const validatePass2 = (rule: any, value: any, callback: any) => {
  if (modifyPasswordForm.value.confirmPassword === '') {
    callback(new Error(proxy.$t('components.layout.header.validate.password')));
  } else {
    // 必须包含数字 字母 符号三种组合

    const reg = /(?=.*[a-zA-Z])(?=.*\d)(?=.*[~!@#$%^&*()_+\-=}{:";<>?,./]).{6,18}$/;
    // 确认两次密码是否一致
    if (value !== modifyPasswordForm.value.newPassword) {
      callback(new Error(proxy.$t('components.layout.header.validate.passwordNotMatch')));
    } else {
      callback();
    }
  }
};
const modifyPasswordRules: any = ref({
  oldPassword: [
    { required: true, message: proxy.$t('components.layout.header.validate.oldPassword'), trigger: 'blur' },
  ],
  newPassword: [
    { required: true, validator: validatePass, trigger: 'change' },
    { required: true, validator: validatePass, trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, validator: validatePass2, trigger: 'change' },
    { required: true, validator: validatePass2, trigger: 'blur' },
  ],
});

const changeLanguage = (lang: string) => {
  locale.value = lang;
  localStorage.setItem('locale', lang);
  document.title = proxy.$t(route.meta.title as string) as string;
};
// 退出登录
const toLogout = () => {
  localStorage.clear();
  Cookies.remove('Access-Token');
  mainStoreData.userInfo = '';
  router.push('/login');
};

// 修改密码
const openModifyPassword = () => {
  modifyPasswordDialog.value = true;
};
const modifyPasswordDialogClose = () => {
  modifyPasswordForm.value = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
  };
};
const modifyPassword = () => {
  // modifyPassword
  const params = {
    old_password: modifyPasswordForm.value.oldPassword,
    new_password: modifyPasswordForm.value.newPassword,
  };
  mainApi.modifyPassword(params).then((res: any) => {
    ElMessage.success(proxy.$t('components.layout.header.message.modifyPasswordSuccess'));
    modifyPasswordDialogClose();
    modifyPasswordDialog.value = false;
    toLogout();
  });
};

watch(
  () => route.path,
  () => {
    navArray.value = route.matched;
  },
);

onMounted(() => {
  navArray.value = route.matched;
});
</script>

<style lang="scss" scpoed>
.headerPage {
  padding: 5px;
  background-color: #fff;

  .hamburger {
    display: inline-block;
    vertical-align: middle;
    width: 20px;
    height: 20px;
  }

  .is-active {
    transform: rotate(180deg);
  }

  .el-breadcrumb {
    .el-breadcrumb__item,
    .el-breadcrumb__inner {
      color: #999;
    }
  }
}
</style>

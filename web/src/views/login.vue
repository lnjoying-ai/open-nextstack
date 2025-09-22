<template>
  <div class="loginPage bg-white h-full">
    <div class="fixed top-0 right-0 text-white">
      <el-dropdown class="mt-2 mr-4">
        <span class="el-dropdown-link text-base text-slate cursor-pointer leading-7 text-white text-shadow-xl">
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
    </div>
    <div class="loginCenter">
      <div class="loginLeft"></div>
      <div class="loginRight">
        <div class="loginText mx-auto w-120 absolute top-1/2 inset-x-0 transform -translate-y-1/2">
          <div class="text-center">
            <img src="@/assets/img/login-logo.png" alt="" class="w-56 mx-auto mb-8" />
          </div>
          <div class="pt-5">
            <el-tabs v-model="activeName" class="demo-tabs" :stretch="true">
              <el-tab-pane :label="$t('login.accountPassword')" name="first">
                <el-form
                  v-loading="loading"
                  :model="form"
                  label-width="0"
                  :element-loading-text="$t('login.loggingIn')"
                >
                  <el-form-item>
                    <el-input
                      v-model="form.phone"
                      class="w-full m-2"
                      :placeholder="$t('login.message.inputAccount')"
                      size="large"
                    >
                      <template #prefix>
                        <img src="@/assets/img/login-user.png" class="loginIcon" alt="" />
                      </template>
                    </el-input>
                  </el-form-item>
                  <el-form-item>
                    <el-input
                      v-model="form.password"
                      type="password"
                      class="w-full m-2"
                      :placeholder="$t('login.message.inputPassword')"
                      size="large"
                    >
                      <template #prefix>
                        <img src="@/assets/img/login-pwd.png" class="loginIcon" alt="" />
                      </template>
                    </el-input>
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" class="w-100" size="large" @click="onSubmit">
                      {{ $t('login.loginButton') }}
                    </el-button>
                  </el-form-item>
                  <el-form-item>
                    <router-link to="/register" class="flex-1 text-right text-blue-400">
                      {{ $t('login.register') }}
                    </router-link>
                  </el-form-item>
                </el-form>
              </el-tab-pane>
              <el-tab-pane :label="$t('login.phoneLogin')" name="second">
                <el-form :model="formphone" label-width="0">
                  <el-form-item>
                    <el-input
                      v-model="formphone.name"
                      class="w-full m-2"
                      :placeholder="$t('login.inputPhoneNumber')"
                      size="large"
                    >
                      <template #prefix>
                        <img src="@/assets/img/login-phone.png" class="loginIcon" alt="" />
                      </template>
                    </el-input>
                  </el-form-item>
                  <el-form-item>
                    <el-input
                      v-model="formphone.code"
                      class="m-2 flex-1"
                      :placeholder="$t('login.verificationCode')"
                      size="large"
                    >
                      <template #prefix>
                        <img src="@/assets/img/login-sms.png" class="loginIcon" alt="" />
                      </template>
                    </el-input>
                    <el-button size="large" class="toSMS">
                      {{ $t('login.getVerificationCode') }}
                    </el-button>
                  </el-form-item>
                  <!-- <el-form-item>
                    <el-checkbox v-model="formphone.type" :label="$t('login.rememberPassword')" class="w-1/2" />
                    <router-link to="/" class="flex-1 text-right text-blue-400">
                      {{ $t('login.forgotPassword') }}
                    </router-link>
                  </el-form-item> -->
                  <el-form-item>
                    <el-button type="primary" class="w-100" size="large" @click="onSubmit">
                      {{ $t('login.loginButton') }}
                    </el-button>
                  </el-form-item>
                  <el-form-item>
                    <router-link to="/register" class="flex-1 text-right text-blue-400">
                      {{ $t('login.register') }}
                    </router-link>
                  </el-form-item>
                </el-form>
              </el-tab-pane>
            </el-tabs>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import loginApi from '@/api/modules/login';
import mainApi from '@/api/modules/main';
import mainStore from '@/store/mainStore';

const { locale } = useI18n();

const { proxy }: any = getCurrentInstance();
const route = useRoute();

const router: any = useRouter();

const mainStoreData = mainStore(); // pinia store

const activeName = ref('first'); // 登录方式
const loading = ref(false);
const form = reactive({
  phone: '',
  password: '',
});
const formphone = reactive({
  name: '',
  code: '',
  type: false,
});
const onSubmit = () => {
  if (form.phone === '') {
    ElMessage({
      message: proxy.$t('login.message.inputAccount'),
      type: 'warning',
      duration: 2000,
    });
    return;
  }
  if (form.password === '') {
    ElMessage({
      message: proxy.$t('login.message.inputPassword'),
      type: 'warning',
      duration: 2000,
    });
    return;
  }
  loading.value = true;
  loginApi
    .login({ name: form.phone, password: form.password })
    .then((res: any) => {
      getInfo();
    })
    .catch((error: any) => {
      loading.value = false;
    });
};
const getInfo = () => {
  mainApi
    .infoList()
    .then((res: any) => {
      localStorage.setItem('userInfo', JSON.stringify(res));
      mainStoreData.userInfo = res;
      loading.value = false;

      if (router.currentRoute.value.query.redirect) {
        router.push(router.currentRoute.value.query.redirect);
      } else {
        router.push('/');
      }
    })
    .catch((error: any) => {});
};
const onKeyPress = () => {
  document.onkeydown = (e) => {
    if (e.keyCode === 13 && loading.value === false) {
      onSubmit();
    }
  };
};
const changeLanguage = (lang: string) => {
  locale.value = lang;
  localStorage.setItem('locale', lang);
  document.title = proxy.$t(route.meta.title as string) as string;
};
onUnmounted(() => {
  document.onkeydown = null;
});
onMounted(() => {
  onKeyPress();
});
</script>

<style lang="scss" scoped>
.loginPage {
  position: relative;
  height: 100vh;
  min-height: 800px;
  background-image: url('@/assets/img/bg.png');
  background-repeat: no-repeat;
  background-size: cover;

  .toSMS {
    color: rgba(96, 165, 250, var(--tw-text-opacity));
    border-color: rgba(96, 165, 250, var(--tw-text-opacity));
  }

  .el-tabs {
    ::v-deep .el-tabs__nav-wrap {
      &::after {
        display: none;
      }
    }

    ::v-deep .el-tabs__item {
      font-size: 17px;
      font-weight: 500;
      color: #666666;

      &.is-active {
        color: #1890ff;
      }
    }
  }

  .loginCenter {
    width: 1200px;
    position: absolute;
    top: 50%;
    right: 0;
    left: 0;
    transform: translateY(-50%);
    margin: 0 auto;
    height: 650px;
    overflow: hidden;
    border-radius: 25px;

    .loginLeft {
      width: 50%;
      height: 100%;
      float: left;
      background-image: url('@/assets/img/login-bg.png');
      background-size: cover;
      background-position: center center;
      background-repeat: no-repeat;
    }

    .loginRight {
      position: relative;
      width: 50%;
      height: 100%;
      float: right;
      background-color: #fff;

      .loginText {
        padding: 0 60px;

        .loginLogo {
          width: 120px;
          display: block;
          margin: 60px auto;
        }

        .el-form-item {
          margin-bottom: 20px;
        }

        .loginIcon {
          width: 18px;
          height: 18px;
          transform: translate(-6px, 10px);
        }
      }

      .loginBottom {
        position: absolute;
        bottom: 50px;
        right: 0;
        left: 0;
        width: 100%;
        text-align: center;
        display: inline-block;
        padding: 0 1.5em;
        background: #fff;
        font-size: 14px;
        margin: 0;
      }
    }
  }
}
</style>

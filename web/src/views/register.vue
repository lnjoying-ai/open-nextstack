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
            <img src="@/assets/img/login-logo.png" alt="" class="w-46 mx-auto mb-10" />
          </div>
          <div class="pt-5">
            <h5 class="mb-1 text-xl text-center">
              {{ $t('login.registerText') }}
            </h5>
            <el-form :model="loginForm" label-width="0">
              <el-form-item>
                <el-input
                  v-model="loginForm.username"
                  class="w-full m-2"
                  :placeholder="$t('login.username')"
                  size="large"
                >
                  <template #prefix>
                    <img src="@/assets/img/login-user.png" class="loginIcon" alt="" />
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item>
                <el-input
                  v-model="loginForm.password"
                  :type="passwordType ? 'text' : 'password'"
                  class="w-full m-2"
                  :placeholder="$t('login.password')"
                  size="large"
                >
                  <template #prefix>
                    <img src="@/assets/img/login-pwd.png" class="loginIcon" alt="" />
                  </template>
                  <template #suffix>
                    <el-icon class="el-input__icon cursor-pointer mt-3" @click="passwordType = !passwordType">
                      <i-teenyicons:eye-closed-outline v-show="!passwordType"></i-teenyicons:eye-closed-outline>
                      <i-teenyicons:eye-outline v-show="passwordType"></i-teenyicons:eye-outline>
                    </el-icon>
                  </template>
                </el-input>
              </el-form-item>
              <el-form-item>
                <el-input
                  v-model="loginForm.phoneNum"
                  class="w-full m-2"
                  :placeholder="$t('login.phoneNumber')"
                  size="large"
                >
                  <template #prefix>
                    <img src="@/assets/img/login-phone.png" class="loginIcon" alt="" />
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item>
                <el-input
                  v-model="loginForm.verificationCode"
                  class="flex-1 m-2"
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

              <el-form-item>
                <el-button type="primary" class="w-40" size="large" @click="onSubmit">
                  {{ $t('login.registerButton') }}
                </el-button>
                <router-link to="/login" class="flex-1 text-right text-blue-400">
                  {{ $t('login.useExistingAccount') }}
                </router-link>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import mainStore from '@/store/mainStore';

const { locale } = useI18n();
const route = useRoute();
const { proxy }: any = getCurrentInstance();
const mainStoreData = mainStore(); // pinia store
const passwordType = ref(false);
const loginForm = reactive({
  username: '',
  password: '',
  phoneNum: '',
  verificationCode: '',
});

const onSubmit = () => {
  console.log('submit!');
};
const changeLanguage = (lang: string) => {
  locale.value = lang;
  localStorage.setItem('locale', lang);
  document.title = proxy.$t(route.meta.title as string) as string;
};
onMounted(() => {});
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

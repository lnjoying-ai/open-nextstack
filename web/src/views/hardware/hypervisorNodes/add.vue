<template>
  <div class="hypervisorNodesAddPage h-full">
    <h5 v-if="!(drawerData && drawerData.isDrawer)" class="bg-white mb-3 px-5 pt-2 pb-2 rounded-md">
      <el-page-header :title="$t('common.backToList')" @back="goBack">
        <template #content> {{ $route.meta.title }} </template>
      </el-page-header>
    </h5>
    <el-form
      ref="addVmForm"
      v-loading="loading"
      :size="mainStoreData.viewSize.main"
      :model="form"
      :rules="rules"
      label-width="120px"
      :element-loading-text="$t('common.loading')"
    >
      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('hardware.hypervisorNodes.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('hardware.hypervisorNodes.form.name') + ':'" prop="name">
            <el-input v-model="form.name" class="!w-60" :placeholder="$t('hardware.hypervisorNodes.form.inputName')" />
          </el-form-item>

          <el-form-item :label="$t('hardware.hypervisorNodes.form.description') + ':'" prop="description">
            <el-input
              v-model="form.description"
              class="!w-100"
              maxlength="255"
              show-word-limit
              type="textarea"
              :rows="2"
              :placeholder="$t('hardware.hypervisorNodes.form.inputDescription')"
            />
          </el-form-item>
          <el-form-item :label="$t('hardware.hypervisorNodes.form.manageIp') + ':'" prop="manageIp">
            <el-input
              v-model="form.manageIp"
              class="!w-60"
              :placeholder="$t('hardware.hypervisorNodes.form.inputManageIp')"
            />
          </el-form-item>
          <el-form-item :label="$t('hardware.hypervisorNodes.advancedConfig') + ':'">
            <el-checkbox
              v-model="checked1"
              :label="$t('hardware.hypervisorNodes.advancedConfig')"
              size="large"
              @change="changeChecked1"
            />
          </el-form-item>
        </div>
      </el-card>

      <el-card v-if="checked1" class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('hardware.hypervisorNodes.advancedConfig') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('hardware.hypervisorNodes.form.hostname') + ':'" prop="hostname">
            <el-input
              v-model="form.hostname"
              class="!w-60"
              :placeholder="$t('hardware.hypervisorNodes.form.inputHostname')"
            />
          </el-form-item>
          <el-form-item :label="$t('hardware.hypervisorNodes.form.sysUsername') + ':'" prop="sysUsername">
            <el-input
              v-model="form.sysUsername"
              class="!w-60"
              :placeholder="$t('hardware.hypervisorNodes.form.inputSysUsername')"
            />
          </el-form-item>
          <el-form-item :label="$t('hardware.hypervisorNodes.form.loginType') + ':'" prop="loginType">
            <el-radio-group v-model="loginType">
              <el-radio :label="true" :value="true">{{ $t('hardware.hypervisorNodes.form.password') }}</el-radio>
              <el-radio :label="false" :value="false">{{ $t('hardware.hypervisorNodes.form.pubkeyId') }}</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item v-if="loginType" :label="$t('hardware.hypervisorNodes.form.password') + ':'">
            <el-input
              v-model="isPassword"
              class="!w-60"
              type="password"
              :placeholder="$t('hardware.hypervisorNodes.form.inputPassword')"
            />
          </el-form-item>
          <el-form-item
            v-if="loginType"
            :label="$t('hardware.hypervisorNodes.form.confirmPassword') + ':'"
            prop="sysPassword"
          >
            <el-input
              v-model="form.sysPassword"
              class="!w-60"
              type="password"
              :placeholder="$t('hardware.hypervisorNodes.form.inputConfirmPassword')"
            />
          </el-form-item>
          <el-form-item v-if="!loginType" :label="$t('hardware.hypervisorNodes.form.pubkeyId') + ':'" prop="pubkeyId">
            <el-select
              v-model="form.pubkeyId"
              class="m-2 ml-0"
              :placeholder="$t('hardware.hypervisorNodes.form.inputPubkeyId')"
            >
              <el-option
                v-for="(item, index) in pubkeysDataList"
                :key="index"
                :label="item.name"
                :value="item.pubkeyId"
              />
            </el-select>
          </el-form-item>
        </div>
      </el-card>
      <el-card class="!border-none mb-3">
        <div class="text item text-center">
          <el-button type="primary" @click="toHypervisorNodesAdd()">{{ $t('common.createNow') }}</el-button>
        </div>
      </el-card>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import mainStore from '@/store/mainStore';
import mainApi from '@/api/modules/main';

const { drawerData } = defineProps<{
  drawerData: any;
}>();

const mainStoreData = mainStore(); // pinia 信息

const { proxy }: any = getCurrentInstance();
const checked1 = ref(false);
const router = useRouter();
const loading = ref(false);
const addVmForm = ref<any>();

const loginType = ref(true);
const isPassword = ref('');
const form = reactive({
  // 表单
  name: '',
  description: '',
  manageIp: '', // 管理IP
  hostname: '', // 主机名
  sysUsername: '', // 系统用户名
  sysPassword: '', // 系统密码
  pubkeyId: '', // 密钥对ID
});
const changeChecked1 = (val: any) => {
  if (val) {
    form.hostname = '';
    form.sysUsername = '';
    form.sysPassword = '';
    form.pubkeyId = '';
    loginType.value = true;
    isPassword.value = '';
  }
};
const pubkeysDataList: any = ref([]); // 密钥对列表

const validatePass = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error(proxy.$t('hardware.hypervisorNodes.validation.inputPassword')));
  } else if (value !== isPassword.value) {
    callback(new Error(proxy.$t('hardware.hypervisorNodes.validation.passwordNotMatch')));
  } else {
    callback();
  }
};
const rules = reactive({
  name: [{ required: true, validator: proxy.$scriptMain.validateName, trigger: 'change' }],
  manageIp: [
    { required: true, message: proxy.$t('hardware.hypervisorNodes.validation.inputManageIp'), trigger: 'blur' },
  ],
  deviceId: [
    {
      required: true,
      message: proxy.$t('hardware.hypervisorNodes.validation.selectDeviceId'),
      trigger: 'change',
    },
  ],
  hostname: [
    { required: true, message: proxy.$t('hardware.hypervisorNodes.validation.inputHostname'), trigger: 'blur' },
    { min: 3, max: 64, message: proxy.$t('hardware.hypervisorNodes.validation.hostnameMin'), trigger: 'blur' },
  ],
  sysUsername: [
    { required: true, message: proxy.$t('hardware.hypervisorNodes.validation.inputSysUsername'), trigger: 'blur' },
    { min: 3, max: 64, message: proxy.$t('hardware.hypervisorNodes.validation.sysUsernameMin'), trigger: 'blur' },
  ],
  sysPassword: [{ required: true, validator: validatePass, trigger: 'change' }],
});

const goBack = () => {
  router.push('/hypervisorNodes');
};
const resetForm = () => {
  // 重置
  addVmForm.value.resetFields();
  isPassword.value = '';
};
const toHypervisorNodesAdd = () => {
  // 计算节点add
  loading.value = true;
  addVmForm.value.validate(async (valid: any) => {
    if (valid) {
      mainApi
        .vmsHypervisorNodesAdd(form)
        .then((res: any) => {
          loading.value = false;
          resetForm();
          proxy.$emit('closeDrawer');
        })
        .catch((error: any) => {
          resetForm();
          loading.value = false;
        });
    } else {
      loading.value = false;
    }
  });
};

const getpubkeysList = () => {
  // 公钥列表

  mainApi
    .pubkeysList()
    .then((res: any) => {
      pubkeysDataList.value = res.pubkeys;
    })
    .catch((error: any) => {});
};

watch(loginType, (newValue) => {
  if (newValue) {
    form.pubkeyId = '';
  } else {
    form.sysUsername = '';
    form.sysPassword = '';
    isPassword.value = '';
  }
});

onMounted(() => {
  getpubkeysList(); // 公钥列表
});
</script>

<style lang="scss" scoped>
.hypervisorNodesAddPage {
}
</style>

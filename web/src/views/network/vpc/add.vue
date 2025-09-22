<template>
  <div class="vpcAddPage h-full">
    <h5 v-if="!(drawerData && drawerData.isDrawer)" class="bg-white mb-3 px-5 pt-2 pb-2 rounded-md">
      <el-page-header :title="$t('common.backToList')" @back="goBack">
        <template #content> {{ $route.meta.title }} </template>
      </el-page-header>
    </h5>
    <el-form
      ref="addVpcForm"
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
            <span>{{ $t('network.vpc.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('network.vpc.form.name') + ':'" prop="name">
            <el-input v-model="form.name" class="!w-60" :placeholder="$t('network.vpc.form.namePlaceholder')" />
          </el-form-item>
          <el-form-item :label="$t('network.vpc.form.ipV4Cidr') + ':'">
            <div class="block">
              <div class="block">
                <el-input
                  v-model="ip1"
                  class="!w-15"
                  :controls="false"
                  :step="1"
                  :min="0"
                  :max="255"
                  step-strictly
                  :disabled="true"
                  @input="changeIp"
                />
                <span class="text-xl px-1">.</span>
                <el-input
                  v-model="ip2"
                  class="!w-15"
                  :controls="false"
                  :step="1"
                  :min="0"
                  :max="255"
                  step-strictly
                  :disabled="ip5 < 9 || ip1 == 192"
                  @input="changeIp"
                />
                <span class="text-xl px-1">.</span>
                <el-input
                  v-model="ip3"
                  class="!w-15"
                  :controls="false"
                  :step="1"
                  :min="0"
                  :max="255"
                  step-strictly
                  :disabled="!(ip5 > 16)"
                  @input="changeIp"
                />
                <span class="text-xl px-1">.</span>
                <el-input
                  v-model="ip4"
                  class="!w-15"
                  :controls="false"
                  :step="1"
                  :min="0"
                  :max="255"
                  step-strictly
                  :disabled="!(ip5 > 24)"
                  @input="changeIp"
                />
                <span class="text-xl px-1">/</span>
                <el-tooltip :visible="ipStatus" placement="right">
                  <template #content>
                    <span class="inline-block w-4 h-4 bg-red-500 rounded-1/2 text-right leading-tight">！</span>
                    {{ $t('network.vpc.form.ipV4CidrMaskError') }}
                  </template>
                  <el-select v-model="ip5" class="!w-20" placeholder="Select">
                    <template v-for="(item, index) in maskList" :key="index">
                      <el-option
                        v-show="(ip1 == 10 && item > 7) || (ip1 == 172 && item > 11) || (ip1 == 192 && item > 15)"
                        :label="item"
                        :value="item"
                        >{{ item }}</el-option
                      >
                    </template>
                  </el-select>
                </el-tooltip>
              </div>
              <div class="text-xs py-2 text-gray-500 block">
                {{ $t('network.vpc.form.suggestCidr') }}:10.0.0.0/8-24
                <span class="text-blue-400 mr-2 cursor-pointer" @click="changeCidr(10, 0, 0, 0, 8, 24)">
                  ({{ $t('network.vpc.form.changeCidr') }})
                </span>
                172.16.0.0/12-24
                <span class="text-blue-400 mr-2 cursor-pointer" @click="changeCidr(172, 16, 0, 0, 12, 24)">
                  ({{ $t('network.vpc.form.changeCidr') }})
                </span>
                192.168.0.0/16-24
                <span class="text-blue-400 mr-2 cursor-pointer" @click="changeCidr(192, 168, 0, 0, 16, 24)">
                  ({{ $t('network.vpc.form.changeCidr') }})
                </span>
              </div>
            </div>
          </el-form-item>
        </div>
      </el-card>
    </el-form>
    <el-form
      v-loading="loading"
      :size="mainStoreData.viewSize.main"
      :model="subnetForm"
      :rules="subnetRules"
      label-width="120px"
      :element-loading-text="$t('common.loading')"
    >
      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('network.vpc.subnetInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('network.vpc.form.subnetName') + ':'" prop="name">
            <el-input v-model="subnetForm.name" class="!w-60" :placeholder="$t('network.vpc.form.inputSubnetName')" />
          </el-form-item>
          <el-form-item :label="$t('network.vpc.form.subnetIpV4Cidr') + ':'">
            <el-input
              v-model="subnetIp1"
              class="!w-15"
              :controls="false"
              :step="1"
              :min="0"
              :max="255"
              step-strictly
              :disabled="true"
              @input="subnetChangeIp"
            />
            <span class="text-xl px-1">.</span>
            <el-input
              v-model="subnetIp2"
              class="!w-15"
              :controls="false"
              :step="1"
              :min="0"
              :max="255"
              step-strictly
              :disabled="ip5 > 15"
              @input="subnetChangeIp"
            />
            <span class="text-xl px-1">.</span>
            <el-input
              v-model="subnetIp3"
              class="!w-15"
              :controls="false"
              :step="1"
              :min="0"
              :max="255"
              step-strictly
              :disabled="ip5 > 23"
              @input="subnetChangeIp"
            />
            <span class="text-xl px-1">.</span>
            <el-input
              v-model="subnetIp4"
              class="!w-15"
              :controls="false"
              :step="1"
              :min="0"
              :max="255"
              step-strictly
              :disabled="subnetIp5 < 25"
              @input="subnetChangeIp"
            />
            <span class="text-xl px-1">/</span>
            <el-tooltip :visible="subnetIpStatus" placement="right">
              <template #content>
                <span class="inline-block w-4 h-4 bg-red-500 rounded-1/2 text-right leading-tight">！</span>
                {{ $t('network.vpc.form.subnetCidrMaskError') }}
              </template>
              <el-select v-model="subnetIp5" class="!w-20" placeholder="Select">
                <template v-for="(item, index) in maskList" :key="index">
                  <el-option v-show="item > ip5 - 1" :label="item" :value="item">{{ item }}</el-option>
                </template>
                <el-option :label="29" :value="29">29</el-option>
              </el-select>
            </el-tooltip>
          </el-form-item>
        </div>
      </el-card>
      <el-card class="!border-none mb-3">
        <div class="text item text-center">
          <el-button type="primary" @click="toVpcAdd">{{ $t('common.createNow') }}</el-button>
        </div>
      </el-card>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import netmask from 'netmask';
import mainStore from '@/store/mainStore';
import mainApi from '@/api/modules/main';

const { drawerData } = defineProps<{
  drawerData: any;
}>();

const mainStoreData = mainStore(); // pinia 信息

const { proxy }: any = getCurrentInstance();

const { Netmask } = netmask;

const router = useRouter();
const loading = ref(false);
const addVpcForm = ref<any>();

const ip1 = ref(10); // IP分配
const ip2 = ref(0); // IP分配
const ip3 = ref(0); // IP分配
const ip4 = ref(0); // IP分配
const ip5 = ref(16); // IP分配
const ipStatus = ref(false); // IP状态
const subnetIpStatus = ref(false); // 子网IP状态
const subnetIp1 = ref(10); // IP分配
const subnetIp2 = ref(0); // IP分配
const subnetIp3 = ref(0); // IP分配
const subnetIp4 = ref(0); // IP分配
const subnetIp5 = ref(24); // IP分配
const maskList = ref([8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28]);
const form = reactive({
  // vpc表单
  name: `vpc-${proxy.$scriptMain.createRandomStr(4, 'Aa0')}`, // 名称
  addressType: 0, // 0：IPV4 1：IPV6
  cidr: '',
});
const subnetForm = reactive({
  // 子网表单
  name: `subnet-${proxy.$scriptMain.createRandomStr(4, 'Aa0')}`,
  vpcId: '',
  addressType: 0, // 0：IPV4 1：IPV6
  cidr: '',
});
const rules = {
  name: [{ required: true, validator: proxy.$scriptMain.validateName, trigger: 'change' }],
};
const subnetRules = {
  name: [{ required: true, validator: proxy.$scriptMain.validateName, trigger: 'change' }],
};
const goBack = () => {
  router.push('/vpc');
};
const toVpcAdd = () => {
  // 虚拟私有云add
  if (ipStatus.value) {
    return;
  }
  if (subnetIpStatus.value) {
    return;
  }
  loading.value = true;
  form.cidr = `${ip1.value}.${ip2.value}.${ip3.value}.${ip4.value}/${ip5.value}`;
  addVpcForm.value.validate(async (valid: any) => {
    if (valid) {
      mainApi
        .vpcAdd(form)
        .then((res: any) => {
          loading.value = false;
          ElMessage.success(proxy.$t('network.vpc.message.startCreateVpc'));
          toSubNetAdd(res.vpcId);
        })
        .catch((error: any) => {
          loading.value = false;
        });
    } else {
      loading.value = false;
    }
  });
};
const toSubNetAdd = (vpcId: any) => {
  // 子网add

  loading.value = true;
  subnetForm.vpcId = vpcId;
  subnetForm.cidr = `${subnetIp1.value}.${subnetIp2.value}.${subnetIp3.value}.${subnetIp4.value}/${subnetIp5.value}`;
  mainApi
    .subnetsAdd(subnetForm)
    .then((res: any) => {
      loading.value = false;
      ElMessage.success(proxy.$t('network.vpc.message.startCreateSubnet'));
      proxy.$emit('closeDrawer');
    })
    .catch((error: any) => {
      loading.value = false;
    });
};
const changeCidr = (ip01: number, ip02: number, ip03: number, ip04: number, min: number, max: number) => {
  ip1.value = ip01;
  ip2.value = ip02;
  ip3.value = ip03;
  ip4.value = ip04;
  ip5.value = min;
  changeIp();
};
const subnetChangeIp = () => {
  // subnetIP变动
  subnetIpStatus.value = false;
  subnetIp1.value = proxy.$scriptMain.parseIntIpNum(subnetIp1.value);
  subnetIp2.value = proxy.$scriptMain.parseIntIpNum(subnetIp2.value);
  subnetIp3.value = proxy.$scriptMain.parseIntIpNum(subnetIp3.value);
  subnetIp4.value = proxy.$scriptMain.parseIntIpNum(subnetIp4.value);
  const subnetIp = `${subnetIp1.value}.${subnetIp2.value}.${subnetIp3.value}.${subnetIp4.value}/${subnetIp5.value}`;
  const netBlock = new Netmask(subnetIp);
  if (`${subnetIp1.value}.${subnetIp2.value}.${subnetIp3.value}.${subnetIp4.value}` == netBlock.base) {
    console.log('ok');
  } else {
    subnetIpStatus.value = true;
  }
};
const changeIp = () => {
  // IP变动
  ipStatus.value = false;
  ip1.value = proxy.$scriptMain.parseIntIpNum(ip1.value);
  ip2.value = proxy.$scriptMain.parseIntIpNum(ip2.value);
  ip3.value = proxy.$scriptMain.parseIntIpNum(ip3.value);
  ip4.value = proxy.$scriptMain.parseIntIpNum(ip4.value);
  const ip = `${ip1.value}.${ip2.value}.${ip3.value}.${ip4.value}/${ip5.value}`;
  const netBlock = new Netmask(ip);
  if (`${ip1.value}.${ip2.value}.${ip3.value}.${ip4.value}` == netBlock.base) {
    console.log('ok');
    subnetIp1.value = ip1.value;
    subnetIp2.value = ip2.value;
    subnetIp3.value = ip3.value;
    subnetIp4.value = ip4.value;
    subnetIp5.value = ip5.value < 24 ? 24 : ip5.value + 1;
  } else {
    ipStatus.value = true;
  }
};

watch(ip5, (newValue) => {
  changeIp();
});
watch(subnetIp5, (newValue) => {
  subnetChangeIp();
});
onMounted(() => {});
</script>

<style lang="scss" scoped>
.vpcAddPage {
}
</style>

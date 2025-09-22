<template>
  <div class="subNetAddPage h-full">
    <h5 v-if="!(drawerData && drawerData.isDrawer)" class="bg-white mb-3 px-5 pt-2 pb-2 rounded-md">
      <el-page-header :title="$t('common.backToList')" @back="goBack">
        <template #content> {{ $route.meta.title }} </template>
      </el-page-header>
    </h5>
    <el-form
      ref="addSubnetForm"
      v-loading="loading"
      :size="mainStoreData.viewSize.main"
      :model="form"
      :rules="rules"
      label-width="140px"
      :element-loading-text="$t('common.loading')"
    >
      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('network.eip.eipInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('network.eip.form.eipPool') + ':'" prop="eipPoolId">
            <el-select
              v-model="form.eipPoolId"
              class="m-2 ml-0"
              :placeholder="$t('network.eip.form.eipPoolPlaceholder')"
            >
              <el-option
                v-for="(item, index) in eipPoolsData"
                :key="index"
                :label="item.name"
                :value="item.poolId"
              ></el-option>
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('network.eip.form.addressType') + ':'" prop="addressType">
            <el-select
              v-model="form.addressType"
              class="m-2 ml-0"
              :placeholder="$t('network.eip.form.addressTypePlaceholder')"
            >
              <el-option key="IPv4" label="IPv4" :value="0"></el-option>
              <!-- <el-option key="IPv6" label="IPv6" :value="1"></el-option> -->
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('network.eip.form.startEip') + ':'">
            <el-input-number
              v-model="ip1"
              class="!w-15"
              :controls="false"
              :step="1"
              :min="0"
              :max="255"
              step-strictly
              @input="changeIp"
            />
            <span class="text-xl px-1">.</span>
            <el-input-number
              v-model="ip2"
              class="!w-15"
              :controls="false"
              :step="1"
              :min="0"
              :max="255"
              step-strictly
              @input="changeIp"
            />
            <span class="text-xl px-1">.</span>
            <el-input-number
              v-model="ip3"
              class="!w-15"
              :controls="false"
              :step="1"
              :min="0"
              :max="255"
              step-strictly
              @input="changeIp"
            />
            <span class="text-xl px-1">.</span>
            <el-input-number
              v-model="ip4"
              class="!w-15"
              :controls="false"
              :step="1"
              :min="0"
              :max="255"
              step-strictly
              @input="changeIp"
            />
          </el-form-item>
          <el-form-item :label="$t('network.eip.form.endEip') + ':'">
            <el-input-number
              v-model="ipEnd1"
              class="!w-15"
              :controls="false"
              :step="1"
              :min="0"
              :max="255"
              step-strictly
              @input="changeEndIp"
            />
            <span class="text-xl px-1">.</span>
            <el-input-number
              v-model="ipEnd2"
              class="!w-15"
              :controls="false"
              :step="1"
              :min="0"
              :max="255"
              step-strictly
              @input="changeEndIp"
            />
            <span class="text-xl px-1">.</span>
            <el-input-number
              v-model="ipEnd3"
              class="!w-15"
              :controls="false"
              :step="1"
              :min="0"
              :max="255"
              step-strictly
              @input="changeEndIp"
            />
            <span class="text-xl px-1">.</span>
            <el-input-number
              v-model="ipEnd4"
              class="!w-15"
              :controls="false"
              :step="1"
              :min="0"
              :max="255"
              step-strictly
              @input="changeEndIp"
            />
          </el-form-item>
        </div>
      </el-card>

      <el-card class="!border-none mb-3">
        <div class="text item text-center">
          <el-button type="primary" @click="toEipAdd">{{ $t('common.createNow') }}</el-button>
        </div>
      </el-card>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import netmask from 'netmask';
import mainStore from '@/store/mainStore'; // pinia 信息
import mainApi from '@/api/modules/main';

const mainStoreData = mainStore();

const { drawerData } = defineProps<{
  drawerData: any;
}>();

const { proxy }: any = getCurrentInstance();

const { Netmask } = netmask;

const loading = ref(false);
const addSubnetForm = ref<any>();
const router = useRouter();
const eipPoolsData: any = ref([]); // EIP Pool数据
const ip1 = ref(0); // IP分配
const ip2 = ref(0); // IP分配
const ip3 = ref(0); // IP分配
const ip4 = ref(0); // IP分配
const ipStatus = ref(false); // IP分配状态

const ipEnd1 = ref(0); // IP分配
const ipEnd2 = ref(0); // IP分配
const ipEnd3 = ref(0); // IP分配
const ipEnd4 = ref(0); // IP分配
const ipEndStatus = ref(false); // IP分配状态

const form = reactive({
  // 表单
  eipPoolId: '', // EIP Pool
  startIpAddress: '',
  endIpAddress: '',
  addressType: 0, // 0：IPV4 1：IPV6
});

const rules = {
  eipPoolId: [
    {
      required: true,
      message: proxy.$t('network.eip.validation.eipPoolRequired'),
      trigger: 'change',
    },
  ],
};

const goBack = () => {
  router.push('/eip');
};
const changeIp = () => {
  // IP变动
  ipStatus.value = false;
  ip1.value = proxy.$scriptMain.parseIntIpNum(ip1.value);
  ip2.value = proxy.$scriptMain.parseIntIpNum(ip2.value);
  ip3.value = proxy.$scriptMain.parseIntIpNum(ip3.value);
  ip4.value = proxy.$scriptMain.parseIntIpNum(ip4.value);
  const ip = `${ip1.value}.${ip2.value}.${ip3.value}.${ip4.value}`;
  const netBlock = new Netmask(ip);
  console.log(netBlock);

  if (`${ip1.value}.${ip2.value}.${ip3.value}.${ip4.value}` == netBlock.base) {
  } else {
    ipStatus.value = true;
  }
};
const changeEndIp = () => {
  // IP变动
  ipEndStatus.value = false;
  ipEnd1.value = proxy.$scriptMain.parseIntIpNum(ipEnd1.value);
  ipEnd2.value = proxy.$scriptMain.parseIntIpNum(ipEnd2.value);
  ipEnd3.value = proxy.$scriptMain.parseIntIpNum(ipEnd3.value);
  ipEnd4.value = proxy.$scriptMain.parseIntIpNum(ipEnd4.value);
  const ipEnd = `${ipEnd1.value}.${ipEnd2.value}.${ipEnd3.value}.${ipEnd4.value}`;

  const netBlock = new Netmask(ipEnd);
  console.log(netBlock);
  if (`${ipEnd1.value}.${ipEnd2.value}.${ipEnd3.value}.${ipEnd4.value}` == netBlock.base) {
  } else {
    ipEndStatus.value = true;
  }
};
const toEipAdd = () => {
  // 创建EIP

  if (ipStatus.value) {
    return;
  }
  loading.value = true;
  const ip = `${ip1.value}.${ip2.value}.${ip3.value}.${ip4.value}`;
  const ipEnd = `${ipEnd1.value}.${ipEnd2.value}.${ipEnd3.value}.${ipEnd4.value}`;

  const params = {
    eipPoolId: form.eipPoolId,
    startIpAddress: ip,
    endIpAddress: ipEnd,
    addressType: form.addressType,
  };
  addSubnetForm.value.validate(async (valid: any) => {
    if (valid) {
      mainApi
        .eipsAdd(params)
        .then((res: any) => {
          loading.value = false;
          ElMessage.success(proxy.$t('common.operations.success.create'));

          proxy.$emit('closeDrawer');
        })
        .catch((error: any) => {
          loading.value = false;
        });
    } else {
      loading.value = false;
    }
  });
};
const getEipList = () => {
  // EIP Pool列表

  mainApi.eipPoolsList().then((res: any) => {
    eipPoolsData.value = res.eipPools;
  });
};
onMounted(() => {
  getEipList();
});
</script>

<style lang="scss" scoped>
.subNetAddPage {
}
</style>

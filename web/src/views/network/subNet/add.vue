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
            <span>{{ $t('network.subnet.vpcInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('network.subnet.form.vpc') + ':'" prop="vpcId">
            <el-select
              v-model="form.vpcId"
              class="m-2 ml-0"
              :placeholder="$t('network.subnet.form.vpcPlaceholder')"
              @change="vpcChange"
            >
              <el-option v-for="(item, index) in tableData" :key="index" :label="item.name" :value="item.vpcId" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('network.subnet.form.vpcIpV4Cidr') + ':'" prop="vpcId">
            <el-input-number
              v-model="Vip1"
              disabled
              class="!w-15"
              :controls="false"
              :step="1"
              :min="0"
              :max="255"
              step-strictly
            />
            <span class="text-xl px-1">.</span>
            <el-input-number
              v-model="Vip2"
              disabled
              class="!w-15"
              :controls="false"
              :step="1"
              :min="0"
              :max="255"
              step-strictly
            />
            <span class="text-xl px-1">.</span>
            <el-input-number
              v-model="Vip3"
              disabled
              class="!w-15"
              :controls="false"
              :step="1"
              :min="0"
              :max="255"
              step-strictly
            />
            <span class="text-xl px-1">.</span>
            <el-input-number
              v-model="Vip4"
              disabled
              class="!w-15"
              :controls="false"
              :step="1"
              :min="0"
              :max="255"
              step-strictly
            />
            <span class="text-xl px-1">/</span>

            <el-select v-model="Vip5" disabled class="!w-20" placeholder="Select">
              <template v-for="(item, index) in maskList" :key="index">
                <el-option :label="item" :value="item">{{ item }}</el-option>
              </template>
            </el-select>
          </el-form-item>
        </div>
      </el-card>
      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('network.subnet.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('network.subnet.form.subnetName') + ':'" prop="name">
            <el-input v-model="form.name" class="!w-60" :placeholder="$t('network.subnet.form.inputSubnetName')" />
          </el-form-item>
          <el-form-item :label="$t('network.subnet.form.subnetIpV4Cidr') + ':'" prop="name">
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
              :disabled="Vip5 > 15"
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
              :disabled="Vip5 > 23"
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
              :disabled="ip5 < 25"
              @input="changeIp"
            />
            <span class="text-xl px-1">/</span>
            <el-tooltip :visible="ipStatus" placement="right">
              <template #content>
                <span class="inline-block w-4 h-4 bg-red-500 rounded-1/2 text-right leading-tight">！</span>
                {{ $t('network.subnet.form.subnetCidrMaskError') }}
              </template>
              <el-select v-model="ip5" class="!w-20" placeholder="Select">
                <template v-for="(item, index) in maskList" :key="index">
                  <el-option v-show="item > Vip5 - 1" :label="item" :value="item">{{ item }}</el-option>
                </template>
                <el-option :label="29" :value="29">29</el-option>
              </el-select>
            </el-tooltip>
          </el-form-item>
        </div>
      </el-card>

      <el-card class="!border-none mb-3">
        <div class="text item text-center">
          <el-button type="primary" @click="toSubNetAdd">
            {{ $t('common.createNow') }}
          </el-button>
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

const currentRow: any = ref(''); // 规格选择 当前行
const ip1 = ref(0); // IP分配
const ip2 = ref(0); // IP分配
const ip3 = ref(0); // IP分配
const ip4 = ref(0); // IP分配
const ip5 = ref(24); // IP分配
const ipStatus = ref(false); // IP状态

const Vip1 = ref(0); // IP分配
const Vip2 = ref(0); // IP分配
const Vip3 = ref(0); // IP分配
const Vip4 = ref(0); // IP分配
const Vip5 = ref(0); // IP分配
const maskList = ref([8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28]);

const form = reactive({
  // 表单
  name: '',
  vpcId: '',
  addressType: 0, // 0：IPV4 1：IPV6
  cidr: '',
});

const tableData: any = ref([]);
const rules = {
  name: [{ required: true, validator: proxy.$scriptMain.validateName, trigger: 'change' }],

  vpcId: {
    required: true,
    message: proxy.$t('network.subnet.validation.selectVpc'),
    trigger: 'change',
  },
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
  } else {
    ipStatus.value = true;
  }
};

watch(ip5, (newValue) => {
  changeIp();
});
const vpcChange = (val: any) => {
  const nowvpc: any = tableData.value.filter((item: any) => {
    return item.vpcId == val;
  })[0];
  Vip1.value = nowvpc.cidr.split('.')[0];
  Vip2.value = nowvpc.cidr.split('.')[1];
  Vip3.value = nowvpc.cidr.split('.')[2];
  Vip4.value = nowvpc.cidr.split('.')[3].split('/')[0];
  Vip5.value = nowvpc.cidr.split('.')[3].split('/')[1];

  ip1.value = nowvpc.cidr.split('.')[0];
  ip2.value = nowvpc.cidr.split('.')[1];
  ip3.value = nowvpc.cidr.split('.')[2];
  ip4.value = nowvpc.cidr.split('.')[3].split('/')[0];
  ip5.value = nowvpc.cidr.split('.')[3].split('/')[1] < 24 ? 24 : nowvpc.cidr.split('.')[3].split('/')[1] * 1 + 1;
};

const getVpcList = () => {
  // VPC列表
  mainApi
    .vpcList({ vpc_phase: 1 })
    .then((res: any) => {
      tableData.value = res.vpcs;
      if (res.vpcs.length > 0) {
        form.vpcId = res.vpcs[0].vpcId;
        vpcChange(res.vpcs[0].vpcId);
      }
    })
    .catch((error: any) => {});
};
const toSubNetAdd = () => {
  // 子网add
  if (ipStatus.value) {
    return;
  }
  loading.value = true;
  form.cidr = `${ip1.value}.${ip2.value}.${ip3.value}.${ip4.value}/${ip5.value}`;
  addSubnetForm.value.validate(async (valid: any) => {
    if (valid) {
      mainApi
        .subnetsAdd(form)
        .then((res: any) => {
          loading.value = false;
          ElMessage.success(proxy.$t('network.subnet.message.startCreate'));
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
const goBack = () => {
  router.push('/subNet');
};
onMounted(() => {
  getVpcList(); // 获取VPC列表
});
</script>

<style lang="scss" scoped>
.subNetAddPage {
}
</style>

<template>
  <div class="snapsAddPage h-full">
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
            <span>{{ $t('common.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('common.name') + ':'" prop="name">
            <el-input v-model="form.name" class="!w-60" :placeholder="$t('compute.snaps.form.namePlaceholder')" />
          </el-form-item>
          <el-form-item :label="$t('common.description') + ':'" prop="description">
            <el-input
              v-model="form.description"
              class="!w-100"
              maxlength="255"
              show-word-limit
              type="textarea"
              :rows="2"
              :placeholder="$t('compute.snaps.form.descriptionPlaceholder')"
            />
          </el-form-item>
          <el-form-item :label="$t('compute.snaps.form.network') + ':'" required>
            <el-select
              v-model="vpcId"
              class="m-2 ml-0"
              :placeholder="$t('compute.snaps.form.selectVpc')"
              @change="vpcIdChange()"
            >
              <el-option
                v-for="(item, index) in vpcList"
                :key="index"
                :label="item.name + ' (' + item.cidr + ')'"
                :value="item.vpcId"
              />
            </el-select>
            <el-select
              v-model="subnetId"
              class="m-2 ml-0"
              :placeholder="$t('compute.snaps.form.selectSubnet')"
              :disabled="!vpcId"
              @change="subnetChange(subnetId)"
            >
              <template v-for="(item, index) in subnetsDataList">
                <el-option
                  v-if="vpcId == item.vpcId"
                  :key="index"
                  :label="item.name + ' (' + item.cidr + ')'"
                  :value="item.subnetId"
                />
              </template>
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('compute.snaps.form.vm') + ':'" prop="vmInstanceId">
            <el-select
              v-model="form.vmInstanceId"
              class="m-2 ml-0"
              :disabled="!subnetId"
              :placeholder="$t('compute.snaps.form.selectVm')"
            >
              <el-option
                v-for="(item, index) in tableData"
                :key="index + 1"
                :label="item.name"
                :value="item.instanceId"
              />
            </el-select>
          </el-form-item>
        </div>
      </el-card>

      <el-card class="!border-none mb-3">
        <div class="text item text-center">
          <el-button type="primary" @click="toFlavorssAdd()">{{ $t('common.create') }}</el-button>
        </div>
      </el-card>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import mainApi from '@/api/modules/main';
import mainStore from '@/store/mainStore';

const { drawerData } = defineProps<{
  drawerData: any;
}>();
const { proxy }: any = getCurrentInstance();

const mainStoreData = mainStore(); // pinia 信息
const router = useRouter();
const loading = ref(false);
const addVmForm = ref<any>();
const tableData: any = ref([]);
const vpcList: any = ref([]); // 虚拟私有云列表
const subnetsDataList: any = ref([]); // 子网列表
const vpcId = ref(''); // 虚拟私有云id
const subnetId = ref(''); // 子网id

const form = reactive({
  // 表单
  name: '',
  description: '',
  vmInstanceId: '',
});

const rules = reactive({
  name: [{ required: true, validator: proxy.$scriptMain.validateName, trigger: 'change' }],
  subnetId: [
    {
      required: true,
      message: proxy.$t('compute.snaps.validation.selectVpcAndSubnet'),
      trigger: 'change',
    },
  ],
  vmInstanceId: [
    {
      required: true,
      message: proxy.$t('compute.snaps.validation.selectVm'),
      trigger: 'change',
    },
  ],
});

const goBack = () => {
  router.push('/snaps');
};
const resetForm = () => {
  // 重置
  addVmForm.value.resetFields();
};
const toFlavorssAdd = () => {
  // 快照add
  loading.value = true;
  addVmForm.value.validate(async (valid: any) => {
    if (valid) {
      mainApi
        .snapsAdd(form)
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
const getVmsInstabcesList = () => {
  // 虚拟机列表

  mainApi
    .vmsInstabcesList({ subnet_id: subnetId.value })
    .then((res: any) => {
      tableData.value = res.vmInstancesInfo;
    })
    .catch((error: any) => {});
};
const getVpcList = () => {
  // VPC列表
  mainApi
    .vpcList({ vpc_phase: 1 })
    .then((res: any) => {
      vpcList.value = res.vpcs;
    })
    .catch((error: any) => {});
};
const getSubNetList = () => {
  // 子网列表

  mainApi
    .subnetsList()
    .then((res: any) => {
      subnetsDataList.value = res.subnets;
    })
    .catch((error: any) => {});
};
const vpcIdChange = () => {
  // vpc 改变
  subnetId.value = '';
  form.vmInstanceId = '';
};
const subnetChange = (e: any) => {
  // 子网变更

  if (e != '') {
    form.vmInstanceId = '';
    tableData.value = [];
    getVmsInstabcesList();
  }
};
onMounted(() => {
  getVpcList(); // VPC列表
  getSubNetList(); // 子网列表
  // getVmsInstabcesList(); //虚拟机列表
});
</script>

<style lang="scss" scoped>
.snapsAddPage {
}
</style>

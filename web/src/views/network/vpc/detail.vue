<template>
  <div class="vpcAddPage h-full">
    <div v-if="drawerData && drawerData.isDrawer" class="overflow-hidden mb-4">
      <el-radio-group
        v-model="tabContent"
        :size="mainStoreData.viewSize.tabChange"
        class="overflow-hidden align-middle float-right"
      >
        <el-radio-button label="info">{{ $t('network.vpc.form.info') }}</el-radio-button>
        <el-radio-button label="img">{{ $t('network.vpc.form.img') }}</el-radio-button>
      </el-radio-group>
    </div>
    <h5 v-else class="bg-white mb-3 px-5 pt-2 pb-2 rounded-md">
      <el-page-header :title="$t('common.backToList')" class="float-left" @back="goBack">
        <template #content> {{ $route.meta.title }} </template>
      </el-page-header>
      <el-radio-group
        v-model="tabContent"
        :size="mainStoreData.viewSize.tabChange"
        class="overflow-hidden align-middle ml-8"
      >
        <el-radio-button label="info">{{ $t('network.vpc.form.info') }}</el-radio-button>
        <el-radio-button label="img">{{ $t('network.vpc.form.img') }}</el-radio-button>
      </el-radio-group>
    </h5>
    <el-card v-if="tabContent == 'img'" class="!border-none mb-3">
      <template #header>
        <div class="">
          <span>{{ $t('network.vpc.form.img') }}</span>
        </div>
      </template>
      <div class="text item">
        <visNetwork :id="drawerData ? drawerData.id : router.currentRoute.value.params.id"></visNetwork>
      </div>
    </el-card>

    <el-form
      v-if="tabContent == 'info'"
      v-loading="loading"
      :size="mainStoreData.viewSize.main"
      :element-loading-text="$t('common.loading')"
      :model="form"
      label-width="120px"
    >
      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('network.vpc.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('network.vpc.form.name') + ':'">
            <span>{{ form.name || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('network.vpc.form.id') + ':'">
            <span>{{ form.vpcId || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('network.vpc.form.ipV4Cidr') + ':'">
            <span>{{ form.cidr || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('common.status') + ':'">
            <el-tag :size="mainStoreData.viewSize.tagStatus" :type="filtersFun.getStatus(form.phaseStatus, 'tag')">{{
              filtersFun.getStatus(form.phaseStatus, 'status')
            }}</el-tag>
          </el-form-item>
          <el-form-item :label="$t('network.vpc.form.subnetNum') + ':'">
            <span>{{ form.count || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('common.createTime') + ':'">
            <span>{{ form.createTime || '-' }}</span>
          </el-form-item>
        </div>
      </el-card>

      <el-card v-for="(item, index) in subnetInvpcList" class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('network.vpc.form.subnet') }}{{ index + 1 }}：{{ item.name || '-' }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('network.vpc.form.subnetName') + ':'">
            <span>{{ item.name || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('network.vpc.form.id') + ':'">
            <span>
              <router-link :to="'/subNet/' + item.subnetId" class="text-blue-400">{{ item.subnetId }}</router-link>
            </span>
          </el-form-item>
          <el-form-item :label="$t('network.vpc.form.ipV4Cidr') + ':'">
            <span>{{ item.cidr || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('common.status') + ':'">
            <el-tag :size="mainStoreData.viewSize.tagStatus" :type="filtersFun.getStatus(item.phaseStatus, 'tag')">{{
              filtersFun.getStatus(item.phaseStatus, 'status')
            }}</el-tag>
          </el-form-item>
          <el-form-item :label="$t('common.createTime') + ':'">
            <span>{{ item.createTime || '-' }}</span>
          </el-form-item>
        </div>
      </el-card>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import mainStore from '@/store/mainStore'; // pinia 信息
import mainApi from '@/api/modules/main';
import filtersFun from '@/utils/statusFun';
import visNetwork from '@/components/visNetwork.vue';

const mainStoreData = mainStore();

const { drawerData } = defineProps<{
  drawerData: any;
}>();

const loading = ref(false);
const router = useRouter();
const tabContent = ref('info');
const form: any = ref({});
const subnetInvpcList: any = ref([]); // 子网列表

const goBack = () => {
  router.push('/vpc');
};
const getDetail = () => {
  // 获取详情
  loading.value = true;
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  mainApi
    .vpcDetail(id)
    .then((res: any) => {
      form.value = res;
      getSubNetListInVpc(res.vpcId);
    })
    .catch((error: any) => {
      loading.value = false;
    });
};
const getSubNetListInVpc = (id: string) => {
  // 获取子网列表
  mainApi
    .subnetInVpc({ vpc_id: id })
    .then((res: any) => {
      loading.value = false;
      subnetInvpcList.value = res.subnets;
    })
    .catch((error: any) => {
      loading.value = false;
    });
};

onMounted(() => {
  getDetail(); // 获取详情
});
</script>

<style lang="scss" scoped>
.vpcAddPage {
}
</style>

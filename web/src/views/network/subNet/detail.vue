<template>
  <div class="subNetAddPage h-full">
    <h5 v-if="!(drawerData && drawerData.isDrawer)" class="bg-white mb-3 px-5 pt-2 pb-2 rounded-md">
      <el-page-header :title="$t('common.backToList')" @back="goBack">
        <template #content> {{ $route.meta.title }} </template>
      </el-page-header>
    </h5>
    <el-form :size="mainStoreData.viewSize.main" :model="form" label-width="160px">
      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('network.subnet.vpcInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('network.subnet.form.vpc') + ':'" prop="vpcId">
            <span>{{ form.vpcName || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('network.subnet.form.vpcId') + ':'" prop="vpcId">
            <span>
              <router-link :to="'/vpc/' + form.vpcId" class="text-blue-400">{{ form.vpcId }}</router-link>
            </span>
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
            <span>{{ form.name || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('network.subnet.form.subnetId') + ':'" prop="subnetId">
            <span>{{ form.subnetId || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('network.subnet.form.subnetIpV4Cidr') + ':'" prop="subnetId">
            <span>{{ form.cidr || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('network.subnet.form.gateway') + ':'" prop="gatewayIp">
            <span>{{ form.gatewayIp || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('network.subnet.form.status') + ':'" prop="phaseStatus">
            <el-tag :size="mainStoreData.viewSize.tagStatus" :type="filtersFun.getStatus(form.phaseStatus, 'tag')">{{
              filtersFun.getStatus(form.phaseStatus, 'status')
            }}</el-tag>
          </el-form-item>
          <el-form-item :label="$t('network.subnet.form.createTime') + ':'" prop="createTime">
            <span>{{ form.createTime || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('network.subnet.form.updateTime') + ':'" prop="updateTime">
            <span>{{ form.updateTime || '-' }}</span>
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

const mainStoreData = mainStore();

const { drawerData } = defineProps<{
  drawerData: any;
}>();

const router: any = useRouter();
const form: any = ref({});

const goBack = () => {
  router.push('/subNet');
};
const getDetail = () => {
  // 获取详情
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  mainApi
    .subnetsDetail(id)
    .then((res: any) => {
      form.value = res;
    })
    .catch((error: any) => {});
};
onMounted(() => {
  getDetail(); // 获取详情
});
</script>

<style lang="scss" scoped>
.subNetAddPage {
}
</style>

<template>
  <div class="subNetAddPage h-full">
    <h5 class="bg-white mb-3 px-5 pt-2 pb-2 rounded-md">
      <el-page-header :title="$t('common.backToList')" @back="goBack">
        <template #content> {{ $route.meta.title }} </template>
      </el-page-header>
    </h5>
    <el-form :size="mainStoreData.viewSize.main" :model="form" label-width="160px">
      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('network.eip.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('network.eip.form.name') + ':'">
            <span>{{ form.name || '-' }}</span>
          </el-form-item>

          <el-form-item :label="$t('network.eip.form.description') + ':'">
            <span>
              {{ form.description || '-' }}
            </span>
          </el-form-item>
          <el-form-item :label="$t('network.eip.form.updateTime') + ':'">
            <span>
              {{ form.updateTime || '-' }}
            </span>
          </el-form-item>
          <el-form-item :label="$t('network.eip.form.createTime') + ':'">
            <span>
              {{ form.createTime || '-' }}
            </span>
          </el-form-item>
        </div>
      </el-card>
      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('network.eip.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('network.eip.form.subnetName') + ':'">
            <span>{{ form.name || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('network.eip.form.subnetId') + ':'">
            <span>{{ form.subnetId || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('network.eip.form.cidr') + ':'">
            <span>{{ form.cidr || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('network.eip.form.gatewayIp') + ':'">
            <span>{{ form.gatewayIp || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('network.eip.form.phaseStatus') + ':'">
            <el-tag :size="mainStoreData.viewSize.tagStatus" :type="filtersFun.getStatus(form.phaseStatus, 'tag')">{{
              filtersFun.getStatus(form.phaseStatus, 'status')
            }}</el-tag>
          </el-form-item>
          <el-form-item :label="$t('network.eip.form.createTime') + ':'">
            <span>{{ form.createTime || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('network.eip.form.updateTime') + ':'">
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

const router: any = useRouter();
const form: any = ref({});

const goBack = () => {
  router.push('/eip');
};
const getDetail = () => {
  // 获取详情

  mainApi
    .subnetsDetail(router.currentRoute.value.params.id)
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

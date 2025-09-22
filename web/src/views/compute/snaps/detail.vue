<template>
  <div class="snapsAddPage h-full">
    <h5 v-if="!(drawerData && drawerData.isDrawer)" class="bg-white mb-3 px-5 pt-2 pb-2 rounded-md">
      <el-page-header :title="$t('common.backToList')" @back="goBack">
        <template #content> {{ $route.meta.title }} </template>
      </el-page-header>
    </h5>
    <el-form :model="form" label-width="120px" :size="mainStoreData.viewSize.main">
      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('common.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('common.name') + ':'">
            <span>{{ form.name || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('common.id') + ':'">
            <span>{{ form.snapId || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('common.description') + ':'">
            <span>{{ form.description || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('compute.snaps.form.vm') + ':'">
            <router-link :to="'/vm/' + form.vmInstanceId" class="text-blue-400">{{
              form.vmInstanceName || '-'
            }}</router-link>
          </el-form-item>

          <el-form-item :label="$t('compute.snaps.info.usageStatus') + ':'">
            <el-tag :size="mainStoreData.viewSize.tagStatus" :type="form.current ? 'success' : 'danger'">{{
              form.current ? $t('compute.snaps.current.inUse') : $t('compute.snaps.current.notInUse')
            }}</el-tag>
          </el-form-item>
          <el-form-item :label="$t('common.status') + ':'">
            <el-tag :size="mainStoreData.viewSize.tagStatus" :type="filtersFun.getVmStatus(form.phaseStatus, 'tag')">{{
              filtersFun.getVmStatus(form.phaseStatus, 'status')
            }}</el-tag>
          </el-form-item>
          <el-form-item :label="$t('common.createTime') + ':'">
            <span>{{ form.createTime || '-' }}</span>
          </el-form-item>
        </div>
      </el-card>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import mainApi from '@/api/modules/main';
import filtersFun from '@/utils/statusFun';
import mainStore from '@/store/mainStore';

const mainStoreData = mainStore(); // pinia 信息
const router = useRouter();
const form: any = ref({});
const { drawerData } = defineProps<{
  drawerData: any;
}>();

const goBack = () => {
  router.push('/snaps');
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
    .snapsDetail(id)
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
.snapsAddPage {
}
</style>

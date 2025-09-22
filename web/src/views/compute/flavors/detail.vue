<template>
  <div class="flavorsAddPage h-full">
    <h5 v-if="!(drawerData && drawerData.isDrawer)" class="bg-white mb-3 px-5 pt-2 pb-2 rounded-md">
      <el-page-header :title="$t('common.backToList')" @back="goBack">
        <template #content> {{ $route.meta.title }} </template>
      </el-page-header>
    </h5>
    <el-form :size="mainStoreData.viewSize.main" :model="form" label-width="120px">
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
            <span>{{ form.flavorId || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('compute.flavor.search.type') + ':'">
            <span v-if="form.gpuCount">{{ $t('compute.flavor.type.gpu') }}</span>
            <span v-else>{{ $t('compute.flavor.type.general') }}</span>
          </el-form-item>
          <el-form-item :label="$t('compute.flavor.search.cpu') + ':'">
            <span>{{ form.cpu || '-' }}{{ $t('common.core') }}</span>
          </el-form-item>
          <el-form-item :label="$t('compute.flavor.search.memory') + ':'">
            <span>{{ form.mem || '-' }}GB</span>
          </el-form-item>

          <el-form-item v-if="form.gpuName" :label="$t('compute.flavor.search.gpu') + ':'">
            <span>{{ form.gpuName }}*{{ form.gpuCount }}</span>
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
import mainStore from '@/store/mainStore'; // pinia 信息
import mainApi from '@/api/modules/main';
import filtersFun from '@/utils/statusFun';

const mainStoreData = mainStore();

const { drawerData } = defineProps<{
  drawerData: any;
}>();

const router = useRouter();
const form: any = ref({});

const goBack = () => {
  router.push('/flavors');
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
    .flavorsDetail(id)
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
.flavorsAddPage {
}
</style>

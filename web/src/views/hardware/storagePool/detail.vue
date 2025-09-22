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
            <span>{{ $t('hardware.storagePool.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('hardware.storagePool.form.name') + ':'">
            <span>{{ form.name || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('hardware.storagePool.form.id') + ':'">
            <span>{{ form.poolId || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('hardware.storagePool.form.type') + ':'">
            <span v-if="form.type == 1">{{ $t('hardware.storagePool.form.fileSystem') }}</span>
            <span v-else>-</span>
          </el-form-item>
          <!-- <el-form-item label="paras：">
            <span>{{ form.paras || '-' }}</span>
          </el-form-item> -->
          <!-- <el-form-item label="sid：">
            <span>{{ form.sid || '-' }}</span>
          </el-form-item> -->
          <el-form-item :label="$t('hardware.storagePool.form.description') + ':'">
            <span>
              {{ form.description || '-' }}
            </span>
          </el-form-item>
          <el-form-item :label="$t('hardware.storagePool.form.updateTime') + ':'">
            <span>
              {{ form.updateTime || '-' }}
            </span>
          </el-form-item>
          <el-form-item :label="$t('hardware.storagePool.form.createTime') + ':'">
            <span>
              {{ form.createTime || '-' }}
            </span>
          </el-form-item>
        </div>
      </el-card>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import mainStore from '@/store/mainStore'; // pinia 信息
import mainApi from '@/api/modules/main';

const mainStoreData = mainStore();

const { drawerData } = defineProps<{
  drawerData: any;
}>();

const router: any = useRouter();

const form: any = ref({});

const goBack = () => {
  router.push('/storagePool');
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
    .storagePoolsDetail(id)
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

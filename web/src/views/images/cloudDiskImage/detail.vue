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
            <span>{{ $t('images.cloudDiskImage.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('images.cloudDiskImage.form.imageName') + ':'">
            <span>{{ form.imageName || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('images.cloudDiskImage.form.id') + ':'">
            <span>{{ form.imageId || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('images.cloudDiskImage.form.osType') + ':'">
            <span>
              <span v-if="form.imageOsType == 0">linux</span>
              <span v-else-if="form.imageOsType == 1">windows</span>
              <span v-else>-</span>
            </span>
          </el-form-item>
          <el-form-item :label="$t('images.cloudDiskImage.form.isPublic') + ':'">
            <span>
              <span v-if="form.isPublic">{{ $t('images.cloudDiskImage.form.yes') }}</span>
              <span v-else>{{ $t('images.cloudDiskImage.form.no') }}</span>
            </span>
          </el-form-item>
          <el-form-item :label="$t('images.cloudDiskImage.form.imageFormat') + ':'">
            <span>
              <span v-if="form.imageFormat == 4">{{ $t('images.cloudDiskImage.form.bareMetal') }}</span>
              <span v-else-if="form.imageFormat == 3">{{ $t('images.cloudDiskImage.form.virtualMachine') }}</span>
              <span v-else>-</span>
            </span>
          </el-form-item>

          <el-form-item :label="$t('images.cloudDiskImage.form.updateTime') + ':'">
            <span>
              {{ form.updateTime || '-' }}
            </span>
          </el-form-item>
          <el-form-item :label="$t('images.cloudDiskImage.form.createTime') + ':'">
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
  router.push('/cloudDiskImage');
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
    .imageDetail(id)
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

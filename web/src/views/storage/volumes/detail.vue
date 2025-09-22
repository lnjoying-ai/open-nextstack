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
            <span>{{ $t('storage.volumes.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('storage.volumes.form.name') + ':'">
            <span>{{ form.name || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('storage.volumes.form.id') + ':'">
            <span>{{ form.volumeId || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('storage.volumes.form.description') + ':'">
            <span>
              {{ form.description || '-' }}
            </span>
          </el-form-item>

          <el-form-item :label="$t('storage.volumes.form.size') + ':'">
            <span>{{ form.size || '-' }}GB</span>
          </el-form-item>
          <el-form-item :label="$t('storage.volumes.form.storagePool') + ':'">
            <span>{{ form.storagePoolName || '-' }}</span>
          </el-form-item>

          <el-form-item :label="$t('storage.volumes.form.updateTime') + ':'">
            <span>
              {{ form.updateTime || '-' }}
            </span>
          </el-form-item>
          <el-form-item :label="$t('storage.volumes.form.createTime') + ':'">
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
const tableData: any = ref([]);

const form: any = ref({});
const formList = reactive({
  // 搜索 筛选
  name: '',
  page_num: 1,
  page_size: mainStoreData.page_size,
  total: 0,
});
const goBack = () => {
  router.push('/volumes');
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
    .volumesDetail(id)
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

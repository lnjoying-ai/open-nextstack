<template>
  <div class="bmsAddPage h-full">
    <h5 v-if="!(drawerData && drawerData.isDrawer)" class="bg-white mb-3 px-5 pt-2 pb-2 rounded-md">
      <el-page-header :title="$t('common.backToList')" @back="goBack">
        <template #content> {{ $route.meta.title }} </template>
      </el-page-header>
    </h5>
    <el-form
      ref="editEipPoolForm"
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
            <span>{{ $t('storage.volumes.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('storage.volumes.form.name') + ':'" prop="name">
            <el-input v-model="form.name" class="!w-60" :placeholder="$t('storage.volumes.form.inputName')" />
          </el-form-item>
          <el-form-item :label="$t('storage.volumes.form.description') + ':'" prop="description">
            <el-input
              v-model="form.description"
              class="!w-100"
              maxlength="255"
              show-word-limit
              type="textarea"
              :rows="2"
              :placeholder="$t('storage.volumes.form.inputDescription')"
            />
          </el-form-item>
          <el-form-item :label="$t('storage.volumes.form.size') + ':'" prop="size">
            <el-input-number
              v-model="form.size"
              :disabled="true"
              :min="1"
              :max="2147483647"
              :precision="0"
              controls-position="right"
              class="float-left"
            />
          </el-form-item>

          <el-form-item :label="$t('storage.volumes.form.storagePool') + ':'" prop="storagePoolId">
            <el-select
              v-model="form.storagePoolId"
              :disabled="true"
              class="!w-50"
              :placeholder="$t('storage.volumes.form.inputStoragePool')"
            >
              <el-option
                v-for="(item, index) in storagePoolList"
                :key="index"
                :label="item.name"
                :value="item.poolId"
              />
            </el-select>
          </el-form-item>
        </div>
      </el-card>

      <el-card class="!border-none mb-3">
        <div class="text item text-center">
          <el-button type="primary" @click="toBmsEdit()">{{ $t('common.update') }}</el-button>
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

const mainStoreData = mainStore(); // pinia 信息
const { proxy }: any = getCurrentInstance();
const router = useRouter();
const loading = ref(false);
const editEipPoolForm: any = ref<any>();
const storagePoolList = ref<any>([]);

const form = reactive({
  // 表单
  name: '',
  description: '',
  size: 1,
  storagePoolId: 0,
});

const rules = reactive({
  name: [{ required: true, validator: proxy.$scriptMain.validateName, trigger: 'change' }],
  storagePoolId: [
    { required: true, message: proxy.$t('storage.volumes.validation.storagePoolRequired'), trigger: 'change' },
  ],
});

const goBack = () => {
  router.push('/volumes');
};

const toBmsEdit = () => {
  loading.value = true;
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  editEipPoolForm.value.validate(async (valid: any) => {
    if (valid) {
      mainApi
        .volumesEdit(form, id)
        .then((res: any) => {
          ElMessage.success(proxy.$t('common.operations.success.modify'));
          loading.value = false;
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
      form.name = res.name;
      form.description = res.description;
      form.size = res.size;
      form.storagePoolId = res.storagePoolId;
    })
    .catch((error: any) => {});
};
const getStoragePools = () => {
  mainApi.storagePoolsList().then((res: any) => {
    storagePoolList.value = res.storagePools;
  });
};
onMounted(() => {
  getStoragePools();
  getDetail(); // 获取详情
});
</script>

<style lang="scss" scoped>
.bmsAddPage {
}
</style>

<template>
  <div class="eipPoolAddPage h-full">
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
            <span>{{ $t('storage.volumesSnapshot.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('storage.volumesSnapshot.form.name') + ':'" prop="name">
            <el-input
              v-model="form.name"
              class="!w-60"
              :placeholder="$t('storage.volumesSnapshot.form.namePlaceholder')"
            />
          </el-form-item>
          <el-form-item :label="$t('storage.volumesSnapshot.form.description') + ':'" prop="description">
            <el-input
              v-model="form.description"
              class="!w-100"
              maxlength="255"
              show-word-limit
              type="textarea"
              :rows="2"
              :placeholder="$t('storage.volumesSnapshot.form.descriptionPlaceholder')"
            />
          </el-form-item>

          <el-form-item :label="$t('storage.volumesSnapshot.form.volume') + ':'" prop="volumeId">
            <el-select
              v-model="form.volumeId"
              class="!w-50"
              :placeholder="$t('storage.volumesSnapshot.form.volumePlaceholder')"
            >
              <el-option
                v-for="(item, index) in volumesList"
                :key="index"
                :label="item.name + ' (' + item.size + 'GB)'"
                :value="item.volumeId"
              />
            </el-select>
          </el-form-item>
        </div>
      </el-card>

      <el-card class="!border-none mb-3">
        <div class="text item text-center">
          <el-button type="primary" @click="toVolumesSnapsAdd()">{{ $t('common.createNow') }}</el-button>
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
const volumesList = ref<any>([]);
const form: any = reactive({
  // 表单
  name: '',
  description: '',
  volumeId: '',
});

const rules = reactive({
  name: [{ required: true, validator: proxy.$scriptMain.validateName, trigger: 'change' }],
  volumeId: [
    { required: true, message: proxy.$t('storage.volumesSnapshot.validation.volumeRequired'), trigger: 'change' },
  ],
});

const goBack = () => {
  router.push('/volumesSnap');
};
const resetForm = () => {
  // 重置
  addVmForm.value.resetFields();
};
const toVolumesSnapsAdd = () => {
  loading.value = true;
  addVmForm.value.validate(async (valid: any) => {
    if (valid) {
      mainApi
        .volumesSnapsAdd(form)
        .then((res: any) => {
          loading.value = false;
          resetForm();
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
const getVolumesList = () => {
  mainApi.volumesList({ page_size: 99999, page_num: 1 }).then((res: any) => {
    volumesList.value = res.volumes;
  });
};
onMounted(() => {
  getVolumesList();
});
</script>

<style lang="scss" scoped>
.eipPoolAddPage {
}
</style>

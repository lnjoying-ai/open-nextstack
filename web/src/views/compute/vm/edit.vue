<template>
  <div class="vmAddPage h-full">
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
            <span>{{ $t('common.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('compute.vm.form.vmName') + ':'" prop="name">
            <el-input v-model="form.name" class="!w-60" :placeholder="$t('compute.vm.form.vmNamePlaceholder')" />
          </el-form-item>
          <el-form-item :label="$t('compute.vm.form.description') + ':'" prop="description">
            <el-input
              v-model="form.description"
              class="!w-100"
              type="textarea"
              :rows="2"
              :placeholder="$t('compute.vm.form.descriptionPlaceholder')"
            />
          </el-form-item>
        </div>
      </el-card>

      <el-card class="!border-none mb-3">
        <div class="text item text-center">
          <el-button type="primary" @click="toVmsInstabcesEdit()">{{ $t('common.update') }}</el-button>
        </div>
      </el-card>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import mainStore from '@/store/mainStore';
import mainApi from '@/api/modules/main';

const mainStoreData = mainStore(); // pinia 信息
const { proxy }: any = getCurrentInstance();
const { drawerData } = defineProps<{
  drawerData: any;
}>();

const router = useRouter();
const loading = ref(false);
const addVmForm: any = ref<any>();

const form = reactive({
  // 表单
  name: '',
  description: '',
});

const rules = reactive({
  name: [{ required: true, validator: proxy.$scriptMain.validateName, trigger: 'change' }],
});

const goBack = () => {
  router.push('/vm');
};

const toVmsInstabcesEdit = () => {
  // 虚拟机修改
  loading.value = true;
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  addVmForm.value.validate(async (valid: any) => {
    if (valid) {
      mainApi
        .vmsInstabcesEdit(form, id)
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
    .vmsInstabcesDetail(id)
    .then((res: any) => {
      form.name = res.name;
      form.description = res.description;
    })
    .catch((error: any) => {});
};
onMounted(() => {
  getDetail(); // 获取详情
});
</script>

<style lang="scss" scoped>
.vmAddPage {
}
</style>

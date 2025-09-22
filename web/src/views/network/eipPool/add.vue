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
            <span>{{ $t('network.eipPool.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('network.eipPool.form.name') + ':'" prop="name">
            <el-input v-model="form.name" class="!w-60" :placeholder="$t('network.eipPool.form.inputName')" />
          </el-form-item>
          <el-form-item :label="$t('network.eipPool.form.vlanId') + ':'" prop="vlanId">
            <el-input
              v-model="form.vlanId"
              class="!w-60"
              :placeholder="$t('network.eipPool.form.inputVlanId')"
              @input="vlanIdchange()"
            />
          </el-form-item>
          <el-form-item :label="$t('network.eipPool.form.description') + ':'" prop="description">
            <el-input
              v-model="form.description"
              class="!w-100"
              maxlength="255"
              show-word-limit
              type="textarea"
              :rows="2"
              :placeholder="$t('network.eipPool.form.inputDescription')"
            />
          </el-form-item>
        </div>
      </el-card>

      <el-card class="!border-none mb-3">
        <div class="text item text-center">
          <el-button type="primary" @click="toEipPoolAdd()">{{ $t('common.createNow') }}</el-button>
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

const form: any = reactive({
  // 表单
  name: '',
  vlanId: '',
  description: '',
});

const rules = reactive({
  name: [{ required: true, validator: proxy.$scriptMain.validateName, trigger: 'change' }],
  vlanId: [{ required: true, message: proxy.$t('network.eipPool.validation.vlanIdRequired'), trigger: 'blur' }],
});

const goBack = () => {
  router.push('/eipPool');
};
const resetForm = () => {
  // 重置
  addVmForm.value.resetFields();
};
const toEipPoolAdd = () => {
  // 快照add
  loading.value = true;
  addVmForm.value.validate(async (valid: any) => {
    if (valid) {
      mainApi
        .eipPoolsAdd(form)
        .then((res: any) => {
          loading.value = false;
          resetForm();
          proxy.$emit('closeDrawer');
        })
        .catch((error: any) => {
          resetForm();
          loading.value = false;
        });
    } else {
      loading.value = false;
    }
  });
};
// 过滤掉数字以外的字符
const vlanIdchange = () => {
  form.vlanId = form.vlanId.replace(/[^\d]/g, '');

  if (form.vlanId > 4094) {
    form.vlanId = 4094;
  }
};
onMounted(() => {});
</script>

<style lang="scss" scoped>
.eipPoolAddPage {
}
</style>

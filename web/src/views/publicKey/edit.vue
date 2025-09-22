<template>
  <div class="bmsAddPage h-full">
    <h5 v-if="!(drawerData && drawerData.isDrawer)" class="bg-white mb-3 px-5 pt-2 pb-2 rounded-md">
      <el-page-header :title="$t('common.backToList')" @back="goBack">
        <template #content> {{ $route.meta.title }} </template>
      </el-page-header>
    </h5>
    <el-form
      ref="editPublicKeyForm"
      :size="mainStoreData.viewSize.main"
      :model="form"
      :rules="rules"
      label-width="120px"
    >
      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('publicKey.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('publicKey.form.name') + ':'" prop="name">
            <el-input v-model="form.name" class="!w-60" :placeholder="$t('publicKey.form.inputName')" />
          </el-form-item>
          <el-form-item :label="$t('publicKey.form.description') + ':'">
            <el-input
              v-model="form.description"
              class="!w-100"
              type="textarea"
              maxlength="255"
              show-word-limit
              :rows="2"
              :placeholder="$t('publicKey.form.inputDescription')"
            />
          </el-form-item>
          <el-form-item :label="$t('publicKey.form.createTime') + ':'">
            <span>{{ form.createTime || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('publicKey.form.updateTime') + ':'">
            <span>{{ form.updateTime || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('publicKey.form.pubkeyId') + ':'">
            <span>{{ form.pubkeyId || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('publicKey.form.publicKey') + ':'">
            <span class="w-100 block break-all">{{ form.pubkey || '-' }}</span>
          </el-form-item>
          <el-form-item label="">
            <el-button type="primary" @click="toPubkeysEdit">{{ $t('common.update') }}</el-button>
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
const { proxy }: any = getCurrentInstance();

const { drawerData } = defineProps<{
  drawerData: any;
}>();
const editPublicKeyForm = ref<any>();

const loading = ref(false);
const router = useRouter();

const form: any = ref({});
const rules = {
  name: [{ required: true, validator: proxy.$scriptMain.validateName, trigger: 'change' }],
};
const goBack = () => {
  router.push('/publicKey');
};
const toPubkeysEdit = () => {
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  editPublicKeyForm.value.validate(async (valid: any) => {
    if (valid) {
      mainApi
        .pubkeysModify(form.value, id)
        .then((res: any) => {
          loading.value = false;
          ElMessage.success(proxy.$t('common.operations.success.update'));
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
  loading.value = true;

  mainApi
    .pubkeysDetail(id)
    .then((res: any) => {
      form.value = res;
    })
    .catch((error: any) => {
      loading.value = false;
    });
};

onMounted(() => {
  getDetail(); // 获取详情
});
</script>

<style lang="scss" scoped>
.bmsAddPage {
}
</style>

<template>
  <div class="bmsAddPage h-full">
    <h5 v-if="!(drawerData && drawerData.isDrawer)" class="bg-white mb-3 px-5 pt-2 pb-2 rounded-md">
      <el-page-header :title="$t('common.backToList')" @back="goBack">
        <template #content> {{ $route.meta.title }} </template>
      </el-page-header>
    </h5>
    <el-form
      ref="addPublicKeyForm"
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
            <span>{{ $t('publicKey.title') }}</span>
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
          <el-form-item :label="$t('publicKey.form.publicKey') + ':'">
            <el-input
              v-model="form.pubKey"
              class="!w-100"
              :rows="8"
              type="textarea"
              :placeholder="$t('publicKey.form.inputPublicKey')"
            />
          </el-form-item>
          <el-alert
            :closable="false"
            class="!w-100 !ml-30 !py-1"
            :title="$t('publicKey.tips.uploadPublicKey')"
            type="warning"
            show-icon
          />
        </div>
      </el-card>
      <el-card class="!border-none mb-3">
        <div class="text item text-center">
          <el-button type="primary" @click="topubkeysUpload()">{{ $t('publicKey.form.uploadPublicKey') }}</el-button>
        </div>
      </el-card>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import mainStore from '@/store/mainStore';
import mainApi from '@/api/modules/main';

const { proxy }: any = getCurrentInstance();

const { drawerData } = defineProps<{
  drawerData: any;
}>();

const mainStoreData = mainStore(); // pinia 信息
const loading = ref(false);

const router = useRouter();
const addPublicKeyForm = ref<any>();

const form = reactive({
  // 表单
  name: '',
  description: '',
  pubKey: '',
});
const rules = {
  name: [{ required: true, validator: proxy.$scriptMain.validateName, trigger: 'change' }],
};
const goBack = () => {
  router.push('/publicKey');
};
const topubkeysUpload = () => {
  // 公钥 上传
  loading.value = true;
  addPublicKeyForm.value.validate(async (valid: any) => {
    if (valid) {
      mainApi
        .pubkeysUpload(form)
        .then((res: any) => {
          loading.value = false;
          ElMessage.success(proxy.$t('publicKey.message.uploadSuccess'));
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
onMounted(() => {});
</script>

<style lang="scss" scoped>
.bmsAddPage {
}
</style>

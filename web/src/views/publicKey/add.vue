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
            <el-input
              v-model="form.name"
              class="!w-60"
              :disabled="pubkeysInfo != ''"
              :placeholder="$t('publicKey.form.inputName')"
            />
          </el-form-item>
          <el-form-item :label="$t('publicKey.form.description') + ':'">
            <el-input
              v-model="form.description"
              class="!w-100"
              type="textarea"
              :disabled="pubkeysInfo != ''"
              maxlength="255"
              show-word-limit
              :rows="2"
              :placeholder="$t('publicKey.form.inputDescription')"
            />
          </el-form-item>
          <el-form-item label="">
            <el-button type="primary" :disabled="pubkeysInfo != ''" @click="toPubkeysAdd">
              {{ $t('common.createNow') }}
            </el-button>
          </el-form-item>
        </div>
      </el-card>
      <el-card v-if="pubkeysInfo" class="!border-none mb-3">
        <div class="text item">
          <el-form-item :label="$t('publicKey.form.pubkeyId') + ':'">
            <span>{{ pubkeysInfo.pubkeyId }}</span>
          </el-form-item>

          <el-form-item :label="$t('publicKey.form.privateKey') + ':'">
            <div>
              <el-alert
                :closable="false"
                class="!w-120 !py-1"
                :title="$t('publicKey.tips.privateKey')"
                type="warning"
                show-icon
              />
              <el-input
                v-model="pubkeysInfo.privateKey"
                class="!w-150 break-all overflow-auto !pr-2"
                type="textarea"
                :rows="12"
                disabled
              />
            </div>
          </el-form-item>
          <el-form-item label="">
            <el-button type="primary" class="float-left !ml-30" @click="copy(pubkeysInfo.privateKey)">{{
              $t('publicKey.form.copyPrivateKey')
            }}</el-button>
            <el-button type="success" class="float-left" @click="toDownload(pubkeysInfo.privateKey)">{{
              $t('publicKey.form.downloadPrivateKey')
            }}</el-button>
          </el-form-item>
        </div>
      </el-card>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import useClipboard from 'vue-clipboard3';
import mainStore from '@/store/mainStore';
import mainApi from '@/api/modules/main';

const { drawerData } = defineProps<{
  drawerData: any;
}>();

const { toClipboard } = useClipboard();
const mainStoreData = mainStore(); // pinia 信息
const { proxy }: any = getCurrentInstance();

const loading = ref(false);
const addPublicKeyForm = ref<any>();
const router = useRouter();

const form = reactive({
  // 表单
  name: '',
  description: '',
});
const pubkeysInfo: any = ref('');
const rules = {
  name: [{ required: true, validator: proxy.$scriptMain.validateName, trigger: 'change' }],
};
const goBack = () => {
  router.push('/publicKey');
};

const toPubkeysAdd = () => {
  // 公钥add
  loading.value = true;
  addPublicKeyForm.value.validate(async (valid: any) => {
    if (valid) {
      mainApi
        .pubkeysAdd(form)
        .then((res: any) => {
          loading.value = false;
          pubkeysInfo.value = res;
          toDownload(res.privateKey);
        })
        .catch((error: any) => {
          loading.value = false;
        });
    } else {
      loading.value = false;
    }
  });
};
const toDownload = (privateKey: string) => {
  // 下载私钥
  const blob = new Blob([privateKey]); // 处理文档流
  const fileName = `id_rsa(${form.name}).pem`;
  const down = document.createElement('a');
  down.download = fileName;
  down.style.display = 'none'; // 隐藏,没必要展示出来
  down.href = URL.createObjectURL(blob);
  document.body.appendChild(down);
  down.click();
  URL.revokeObjectURL(down.href); // 释放URL 对象
  document.body.removeChild(down); // 下载完成移除
};
const copy = async (Msg: any) => {
  try {
    // 复制
    await toClipboard(Msg);
    ElMessage.success(proxy.$t('publicKey.message.copySuccess'));

    // 下面可以设置复制成功的提示框等操作
    // ...
  } catch (e) {
    // 复制失败
    console.error(e);
    ElMessage.warning(proxy.$t('publicKey.message.copyFailed'));
  }
};
onMounted(() => {});
</script>

<style lang="scss" scoped>
.bmsAddPage {
}
</style>

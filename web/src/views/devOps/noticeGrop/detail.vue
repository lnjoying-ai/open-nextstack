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
      :element-loading-text="proxy.$t('common.loading')"
    >
      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('devOps.noticeGrop.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('devOps.noticeGrop.form.name') + ':'" prop="name">
            <span>{{ form.name }}</span>
          </el-form-item>
          <el-form-item :label="$t('devOps.noticeGrop.form.id') + ':'">
            <span>{{ form.receiverId || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('devOps.noticeGrop.form.description') + ':'" prop="description">
            <span>{{ form.description }}</span>
          </el-form-item>
          <el-form-item :label="$t('devOps.noticeGrop.form.type') + ':'" prop="type">
            <span v-if="form.type == 0">{{ $t('devOps.noticeGrop.form.email') }}</span>
            <span v-if="form.type == 1">{{ $t('devOps.noticeGrop.form.sms') }}</span>
            <span v-if="form.type == 2">{{ $t('devOps.noticeGrop.form.phone') }}</span>
          </el-form-item>
        </div>
      </el-card>
      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('devOps.noticeGrop.config') }}</span>
          </div>
        </template>
        <div class="text item">
          <div v-for="(item, index) in form.contactInfos" :key="index">
            <el-form-item
              :label="
                index == 0
                  ? form.type == 0
                    ? $t('devOps.noticeGrop.form.emailAddress') + ':'
                    : $t('devOps.noticeGrop.form.phoneNumber') + ':'
                  : ''
              "
              class="!mb-0"
              :rules="[
                {
                  required: true,
                  validator: form.type == 0 ? proxy.$scriptMain.validateEmail : proxy.$scriptMain.validatePhone,
                  trigger: 'change',
                },
              ]"
              :prop="'contactInfos.' + index"
            >
              <span class="block">{{ form.contactInfos[index] }}</span>
            </el-form-item>
          </div>

          <!-- <el-form-item label="邮箱服务器"
                        prop="type">
            <span>123456@qq.com</span>
            <el-button size="small"
                       type="primary"
                       @click="sendTest()">
              测试
            </el-button>
          </el-form-item> -->
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
  receiverId: '',
  type: 0,
  description: '',
  contactInfos: [''],
});

const rules = reactive({
  name: [{ required: true, validator: proxy.$scriptMain.validateName, trigger: 'change' }],
});

const goBack = () => {
  router.push('/devOps/noticeGrop');
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
    .operationReceiversDetail(id)
    .then((res: any) => {
      form.name = res.name;
      form.receiverId = res.receiverId;
      form.type = res.type;
      form.description = res.description;
      form.contactInfos = res.contactInfos;
    })
    .catch((error: any) => {});
};
onMounted(() => {
  getDetail();
});
</script>

<style lang="scss" scoped>
.eipPoolAddPage {
}
</style>

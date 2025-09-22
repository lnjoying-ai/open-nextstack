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
            <el-input v-model="form.name" class="!w-60" :placeholder="$t('devOps.noticeGrop.form.inputName')" />
          </el-form-item>
          <el-form-item :label="$t('devOps.noticeGrop.form.description') + ':'" prop="description">
            <el-input
              v-model="form.description"
              class="!w-100"
              maxlength="255"
              show-word-limit
              type="textarea"
              :rows="2"
              :placeholder="$t('devOps.noticeGrop.form.inputDescription')"
            />
          </el-form-item>
          <el-form-item :label="$t('devOps.noticeGrop.form.type') + ':'" prop="type">
            <el-row>
              <el-col :span="12">
                <el-radio-group v-model="form.type" @change="changeType">
                  <el-radio :label="0" :value="0">{{ $t('devOps.noticeGrop.form.email') }}</el-radio>
                  <el-radio :label="1" :value="1">{{ $t('devOps.noticeGrop.form.sms') }}</el-radio>
                  <!-- <el-radio label="2"
                            :value="2">电话</el-radio> -->
                </el-radio-group>
              </el-col>
              <el-col :span="12">
                <el-input
                  class="!w-100"
                  maxlength="255"
                  show-word-limit
                  type="textarea"
                  :rows="6"
                  :disabled="true"
                  :model-value="form.type == 0 ? description0 : description1"
                  :placeholder="$t('devOps.noticeGrop.form.inputDescription')"
                />
              </el-col>
            </el-row>
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
              :rules="[{ required: true, validator: validateContact, trigger: 'change' }]"
              :prop="'contactInfos.' + index"
            >
              <el-input
                v-model="form.contactInfos[index]"
                class="!w-60"
                :placeholder="
                  form.type == 0
                    ? $t('devOps.noticeGrop.form.emailAddressPlaceholder')
                    : $t('devOps.noticeGrop.form.phoneNumberPlaceholder')
                "
              />
              <div class="overflow-hidden block text-right mt-2 ml-2">
                <div class="overflow-hidden float-left h-4 mx-2">
                  <el-button
                    v-if="index != 0"
                    size="small"
                    circle
                    class="align-top p-0 !w-4 !h-4"
                    type="danger"
                    @click="removeNoticeData(index)"
                  >
                    <i-ic-baseline-remove></i-ic-baseline-remove>
                  </el-button>
                </div>
              </div>
            </el-form-item>
          </div>
          <el-form-item>
            <el-button size="small" class="" type="text" @click="addNotice()">
              <i-ic-baseline-add></i-ic-baseline-add
              >{{
                $t('devOps.noticeGrop.form.add') +
                (form.type == 0 ? $t('devOps.noticeGrop.form.emailAddress') : $t('devOps.noticeGrop.form.phoneNumber'))
              }}
            </el-button>
          </el-form-item>
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

      <el-card class="!border-none mb-3">
        <div class="text item text-center">
          <el-button type="primary" @click="toNoticeGroupAdd()">{{ $t('common.updateNow') }}</el-button>
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
const description0 = proxy.$t('devOps.noticeGrop.form.tips.email');
const description1 = proxy.$t('devOps.noticeGrop.form.tips.phone');
const form: any = reactive({
  // 表单
  name: '',
  type: 0,
  description: '',
  contactInfos: [''],
});
const validateContact = (rule: any, value: any, callback: any) => {
  const count = form.contactInfos.filter((item: any) => item == value).length;
  if (form.type == 0) {
    if (value === '') {
      callback(new Error(proxy.$t('devOps.noticeGrop.form.validate.email')));
    } else if (!/^(\w-*\.*)+@(\w-?)+(\.\w{2,})+$/.test(value)) {
      callback(new Error(proxy.$t('devOps.noticeGrop.form.validate.emailFormat')));
    } else if (count > 1) {
      callback(new Error(proxy.$t('devOps.noticeGrop.form.validate.emailDuplicate')));
    } else {
      callback();
    }
  } else if (value === '') {
    callback(new Error(proxy.$t('devOps.noticeGrop.form.validate.phone')));
  } else if (!/^1[3456789]\d{9}$/.test(value)) {
    callback(new Error(proxy.$t('devOps.noticeGrop.form.validate.phoneFormat')));
  } else if (count > 1) {
    callback(new Error(proxy.$t('devOps.noticeGrop.form.validate.phoneDuplicate')));
  } else {
    callback();
  }
};
const rules = reactive({
  name: [{ required: true, validator: proxy.$scriptMain.validateName, trigger: 'change' }],
});

const goBack = () => {
  router.push('/devOps/noticeGrop');
};
const resetForm = () => {
  // 重置
  addVmForm.value.resetFields();
  form.contactInfos = [''];
};
const toNoticeGroupAdd = () => {
  // 通知组add
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
        .operationReceiversEdit(form, id)
        .then((res: any) => {
          loading.value = false;
          resetForm();
          proxy.$emit('closeDrawer');
        })
        .catch((error: any) => {
          // resetForm();
          loading.value = false;
        });
    } else {
      loading.value = false;
    }
  });
};
const addNotice = () => {
  // 添加地址
  form.contactInfos.push('');
};
const removeNoticeData = (index: number) => {
  // 删除地址
  form.contactInfos.splice(index, 1);
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
      form.type = res.type;
      form.description = res.description;
      form.contactInfos = res.contactInfos;
    })
    .catch((error: any) => {});
};
const changeType = (val: any) => {
  form.contactInfos = [''];
};
onMounted(() => {
  getDetail();
});
</script>

<style lang="scss" scoped>
.eipPoolAddPage {
}
</style>

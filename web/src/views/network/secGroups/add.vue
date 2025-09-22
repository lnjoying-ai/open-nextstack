<template>
  <div class="sgsAddPage h-full">
    <el-dialog
      v-model="addStatus"
      :title="addType == 'edit' ? $t('network.secGroups.edit') : $t('network.secGroups.create')"
      width="500px"
      :before-close="handleClose"
      :close-on-click-modal="false"
    >
      <el-form
        ref="addSecGroupsForm"
        v-loading="loading"
        :size="mainStoreData.viewSize.main"
        :model="form"
        :rules="rules"
        label-width="120px"
        :element-loading-text="$t('common.loading')"
      >
        <div class="text item">
          <el-form-item :label="$t('network.secGroups.form.name') + ':'" prop="name">
            <el-input
              v-model="form.name"
              class="!w-60"
              :placeholder="$t('network.secGroups.form.inputSecurityGroupName')"
            />
          </el-form-item>
          <el-form-item :label="$t('network.secGroups.form.description') + ':'" prop="description">
            <el-input
              v-model="form.description"
              class="!w-60"
              maxlength="255"
              show-word-limit
              type="textarea"
              :rows="4"
              :placeholder="$t('network.secGroups.form.inputSecurityGroupDescription')"
            />
          </el-form-item>
        </div>

        <div class="text item text-center pt-20">
          <el-button v-if="addType == 'edit'" type="primary" @click="toSgsUpdate">
            {{ $t('common.update') }}
          </el-button>
          <el-button v-else type="primary" @click="toSgsAdd">
            {{ $t('common.createNow') }}
          </el-button>
        </div>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import mainStore from '@/store/mainStore';
import mainApi from '@/api/modules/main';

const mainStoreData = mainStore(); // pinia 信息

const { proxy }: any = getCurrentInstance();

const props = defineProps({
  addStatus: {
    type: Boolean,
    default: false,
  },
  addType: {
    type: String,
    default: 'add',
  },
  addForm: {
    type: Object,
    default: {
      name: '',
      description: '',
    },
  },
  sgId: {
    type: String,
    default: '',
  },
});
const router = useRouter();
const loading = ref(false);
const addSecGroupsForm = ref<any>();

const form = reactive({
  // 表单
  name: `sg-${proxy.$scriptMain.createRandomStr(4, 'Aa0')}`,
  description: '',
});
const sgId = ref('');

const rules = {
  name: [{ required: true, validator: proxy.$scriptMain.validateName, trigger: 'change' }],
};
const handleClose = () => {
  proxy.$emit('closeAdd');
  resetForm();
};
const resetForm = () => {
  addSecGroupsForm.value.resetFields();
};
const toSgsAdd = () => {
  // 安全组add
  loading.value = true;
  addSecGroupsForm.value.validate(async (valid: any) => {
    if (valid) {
      mainApi
        .sgsAdd(form)
        .then((res: any) => {
          loading.value = false;
          ElMessage.success(proxy.$t('common.operations.success.create'));
          handleClose();
          proxy.$emit('getSgsList');
        })
        .catch((error: any) => {
          loading.value = false;
        });
    } else {
      loading.value = false;
    }
  });
};
const toSgsUpdate = () => {
  // 安全组add
  loading.value = true;
  mainApi
    .sgsEdit(form, sgId.value)
    .then((res: any) => {
      loading.value = false;
      ElMessage.success(proxy.$t('common.operations.success.update'));
      handleClose();
      proxy.$emit('getSgsList');
    })
    .catch((error: any) => {
      loading.value = false;
    });
};
watch(props, (newValue) => {
  if (newValue.addType == 'edit') {
    sgId.value = newValue.sgId;
    form.name = newValue.addForm.name;
    form.description = newValue.addForm.description;
  } else {
    form.name = `sg-${proxy.$scriptMain.createRandomStr(4, 'Aa0')}`;
  }
});
onMounted(() => {});
</script>

<style lang="scss" scoped>
.sgsAddPage {
}
</style>

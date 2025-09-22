<template>
  <div class="flavorsAddPage h-full">
    <h5 v-if="!(drawerData && drawerData.isDrawer)" class="bg-white mb-3 px-5 pt-2 pb-2 rounded-md">
      <el-page-header :title="$t('common.backToList')" @back="goBack">
        <template #content>{{ $route.meta.title }}</template>
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
        <div v-if="maxInfo" class="text item">
          <el-form-item :label="$t('compute.flavor.search.type') + ':'" prop="type">
            <el-radio-group v-model="form.addType" size="small">
              <el-radio-button label="info" value="info">{{ $t('compute.flavor.type.general') }}</el-radio-button>
              <el-radio-button label="gpu" value="gpu">{{ $t('compute.flavor.type.gpu') }}</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item :label="$t('common.name') + ':'" prop="name">
            <el-input v-model="form.name" class="!w-60" :placeholder="$t('common.name')" />
          </el-form-item>

          <el-form-item :label="$t('common.cpu') + ':'" prop="cpu">
            <el-input-number
              v-model="form.cpu"
              class="!w-60"
              :min="0"
              :step="1"
              step-strictly
              :max="maxInfo.maxVcpu || ''"
              controls-position="right"
            />
            <span class="pl-2">{{ $t('common.core') }}</span>
          </el-form-item>

          <el-form-item :label="$t('common.memory') + ':'" prop="mem">
            <el-input-number
              v-model="form.mem"
              class="!w-60"
              :min="0"
              :step="1"
              step-strictly
              :max="maxInfo.maxMemory || ''"
              controls-position="right"
            />
            <span class="pl-2">GB</span>
          </el-form-item>

          <el-form-item v-if="form.addType === 'gpu'" :label="$t('compute.flavor.gpuName') + ':'" prop="gpuName">
            <el-select
              v-model="form.gpuName"
              class="!w-60"
              :placeholder="$t('common.select.typeSelect')"
              @change="form.gpuCount = 0"
            >
              <el-option v-for="(item, index) in GPUInfo" :key="index" :label="item.gpuName" :value="item.gpuName" />
            </el-select>
          </el-form-item>

          <el-form-item v-if="form.addType === 'gpu'" :label="$t('compute.flavor.gpuCount') + ':'" prop="gpuCount">
            <el-input-number
              v-model="form.gpuCount"
              class="!w-60"
              :min="0"
              :step="1"
              :disabled="form.gpuName === ''"
              step-strictly
              :max="form.gpuName ? GPUInfo.filter((item: any) => item.gpuName === form.gpuName)[0].gpuCount : 0"
              controls-position="right"
            />
            <span v-if="form.gpuName" class="pl-2">
              {{ $t('compute.flavor.availableGpu', { count: GPUInfo.filter((item: any) => item.gpuName === form.gpuName)[0].gpuCount }) }}
            </span>
          </el-form-item>
        </div>
      </el-card>

      <el-card class="!border-none mb-3">
        <div class="text item text-center">
          <el-button type="primary" @click="toFlavorssAdd()">{{ $t('common.createNow') }}</el-button>
        </div>
      </el-card>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import mainStore from '@/store/mainStore';
import mainApi from '@/api/modules/main';

const { drawerData } = defineProps<{
  drawerData: any;
}>();

const mainStoreData = mainStore();
const { proxy }: any = getCurrentInstance();
const router = useRouter();

const loading = ref(false);
const addVmForm = ref<any>();
const maxInfo = ref<any>();
const GPUInfo = ref<any>();

// 表单数据
const form = reactive({
  addType: 'info',
  name: '',
  type: '1',
  cpu: 0,
  mem: 0,
  gpuCount: 0,
  gpuName: '',
});

// 表单校验规则
const rules = reactive({
  name: [{ required: true, validator: proxy.$scriptMain.validateName, trigger: 'change' }],
  type: [{ required: true, message: proxy.$t('common.select.typeSelect'), trigger: 'change' }],
  cpu: [
    { required: true, message: proxy.$t('common.pleaseInput'), trigger: 'change' },
    {
      validator: (rule: any, value: any, callback: any) => {
        if (value > maxInfo.value.maxVcpu) {
          callback(new Error(`${proxy.$t('compute.flavor.validation.cpuMax', { max: maxInfo.value.maxVcpu })}`));
        } else if (value < 1) {
          callback(new Error(proxy.$t('compute.flavor.validation.cpuMin')));
        } else {
          callback();
        }
      },
      trigger: 'change',
    },
  ],
  mem: [
    { required: true, message: proxy.$t('compute.flavor.validation.memoryRequired'), trigger: 'change' },
    {
      validator: (rule: any, value: any, callback: any) => {
        if (value > maxInfo.value.maxMemory) {
          callback(new Error(`${proxy.$t('compute.flavor.validation.memoryMax', { max: maxInfo.value.maxMemory })}`));
        } else if (value < 1) {
          callback(new Error(proxy.$t('compute.flavor.validation.memoryMin')));
        } else {
          callback();
        }
      },
      trigger: 'change',
    },
  ],
  gpuName: [{ required: true, message: proxy.$t('compute.flavor.validation.gpuTypeRequired'), trigger: 'change' }],
  gpuCount: [
    { required: true, message: proxy.$t('compute.flavor.validation.gpuCountRequired'), trigger: 'change' },
    {
      validator: (rule: any, value: any, callback: any) => {
        if (form.gpuName === '') {
          callback(new Error(proxy.$t('compute.flavor.validation.gpuTypeRequired')));
        } else if (value > GPUInfo.value.filter((item: any) => item.gpuName === form.gpuName)[0].gpuCount) {
          callback(
            new Error(
              `${proxy.$t('compute.flavor.validation.gpuCountMax', {
                max: GPUInfo.value.filter((item: any) => item.gpuName === form.gpuName)[0].gpuCount,
              })}`,
            ),
          );
        } else if (value < 1) {
          callback(new Error(proxy.$t('compute.flavor.validation.gpuCountMin')));
        } else {
          callback();
        }
      },
      trigger: 'change',
    },
  ],
});

const goBack = () => {
  router.push('/flavors');
};

const resetForm = () => {
  addVmForm.value.resetFields();
};

const toFlavorssAdd = () => {
  addVmForm.value.validate(async (valid: any) => {
    if (valid) {
      loading.value = true;
      const data = {
        name: form.name,
        type: form.type,
        cpu: form.cpu,
        mem: form.mem,
        gpuName: form.addType === 'info' ? '' : form.gpuName,
        gpuCount: form.addType === 'info' ? 0 : form.gpuCount,
      };

      mainApi
        .flavorsAdd(data)
        .then(() => {
          ElMessage.success(proxy.$t('common.operations.success.create'));
          loading.value = false;
          proxy.$emit('closeDrawer');
          resetForm();
        })
        .catch(() => {
          loading.value = false;
        });
    } else {
      loading.value = false;
    }
  });
};

const getflavorsMaxInfo = () => {
  mainApi
    .flavorsMaxInfo()
    .then((res: any) => {
      maxInfo.value = res;
    })
    .catch(() => {});
};

const getflavorGPUInfo = () => {
  mainApi
    .flavorGPUInfo()
    .then((res: any) => {
      GPUInfo.value = res;
    })
    .catch(() => {});
};

onMounted(() => {
  getflavorsMaxInfo();
  getflavorGPUInfo();
});
</script>

<style lang="scss" scoped>
.flavorsAddPage {
}
</style>

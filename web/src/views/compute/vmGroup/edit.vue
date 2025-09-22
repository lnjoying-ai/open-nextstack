<template>
  <div class="snapsAddPage h-full">
    <h5 v-if="!(drawerData && drawerData.isDrawer)" class="bg-white mb-3 px-5 pt-2 pb-2 rounded-md">
      <el-page-header :title="$t('common.back')" @back="goBack">
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
          <el-form-item :label="$t('common.name') + ':'" prop="name">
            <el-input v-model="form.name" class="!w-60" :placeholder="$t('common.name')" />
          </el-form-item>
          <el-form-item :label="$t('common.description') + ':'" prop="description">
            <el-input
              v-model="form.description"
              class="!w-100"
              maxlength="255"
              show-word-limit
              type="textarea"
              :rows="2"
              :placeholder="$t('common.description')"
            />
          </el-form-item>
          <el-form-item :label="$t('compute.vmGroup.form.vm') + ':'" prop="description">
            <div class="text-right block w-full">
              <el-button type="primary" class="float-right" :size="mainStoreData.viewSize.main" @click="addInstance()">
                {{ $t('compute.vmGroup.addVm') }}
              </el-button>
            </div>
            <el-table
              :size="mainStoreData.viewSize.main"
              :data="vmTableData"
              class="!overflow-y-auto"
              stripe
              :scrollbar-always-on="false"
            >
              <el-table-column prop="date" :label="$t('common.name')">
                <template #default="scope">
                  <!-- 新窗口打开 -->
                  <router-link :to="'/vm/' + scope.row.instanceId" target="_blank">
                    <span class="text-blue-400 cursor-pointer">{{ scope.row.name }}</span>
                  </router-link>
                </template>
              </el-table-column>

              <el-table-column prop="hostname" :label="$t('compute.vmGroup.form.hostname')" />
              <el-table-column prop="portInfo.ipAddress" :label="$t('compute.vmGroup.form.ip')">
                <template #default="scope">
                  <span>{{ scope.row.portInfo.ipAddress || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="vpcInfo.cidr" :label="$t('compute.vmGroup.form.networkAddress')">
                <template #default="scope">
                  <span>{{ scope.row.subnetInfo.cidr || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="eip" :label="$t('compute.vmGroup.form.eip')">
                <template #default="scope">
                  <span v-if="!scope.row.boundPhaseStatus">{{ '-' }}</span>
                  <span v-if="scope.row.boundType && scope.row.boundType === 'port'">
                    <span v-if="scope.row.boundPhaseStatus == 82">{{ scope.row.eip || '-' }}</span>
                    <el-tag
                      v-else
                      :size="mainStoreData.viewSize.tagStatus"
                      :type="filtersFun.getVmToEipStatus(scope.row.boundPhaseStatus, 'tag')"
                      >{{ filtersFun.getVmToEipStatus(scope.row.boundPhaseStatus, 'status') }}</el-tag
                    >
                  </span>
                  <span v-if="scope.row.boundType && scope.row.boundType === 'nat'">
                    <span v-if="scope.row.boundPhaseStatus == 7">
                      <span v-if="scope.row.eip">
                        <el-tag :size="mainStoreData.viewSize.tagStatus">{{ $t('compute.vmGroup.form.nat') }}</el-tag>
                        {{ scope.row.eip }}
                      </span>
                      <span v-else>-</span>
                    </span>
                    <el-tag
                      v-else
                      :size="mainStoreData.viewSize.tagStatus"
                      :type="filtersFun.getNatStatus(scope.row.boundPhaseStatus, 'tag')"
                      >{{ filtersFun.getNatStatus(scope.row.boundPhaseStatus, 'status') }}</el-tag
                    >
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="phaseStatus" :label="$t('common.status')">
                <template #default="scope">
                  <el-tag
                    :size="mainStoreData.viewSize.tagStatus"
                    :type="filtersFun.getVmStatus(scope.row.phaseStatus, 'tag')"
                    >{{ filtersFun.getVmStatus(scope.row.phaseStatus, 'status') }}</el-tag
                  >
                </template>
              </el-table-column>
              <el-table-column prop="imageInfo.name" :label="$t('compute.vmGroup.form.system')" />
              <el-table-column prop="createTime" :label="$t('common.createTime')" />
              <el-table-column :label="$t('common.operation')" width="120">
                <template #default="scope">
                  <el-popconfirm
                    :confirm-button-text="$t('common.remove')"
                    :cancel-button-text="$t('common.cancel')"
                    icon-color="#626AEF"
                    :title="$t('compute.vmGroup.confirm.remove')"
                    @confirm="toDelete(scope.row, scope.$index)"
                  >
                    <template #reference>
                      <span class="listDelBtn text-blue-400"
                        ><img src="@/assets/img/btn/delete.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                          $t('common.remove')
                        }}</span
                      >
                    </template>
                  </el-popconfirm>
                </template>
              </el-table-column>
            </el-table>
          </el-form-item>
        </div>
      </el-card>

      <el-card class="!border-none mb-3">
        <div class="text item text-center">
          <el-button type="primary" @click="toVmGroupAdd()">{{ $t('common.update') }}</el-button>
        </div>
      </el-card>
    </el-form>
    <el-dialog
      v-model="dialogVm"
      v-loading="vmLoading"
      :close-on-click-modal="false"
      width="1200px"
      destroy-on-close
      :element-loading-text="$t('common.loading')"
      :before-close="vmHandleClose"
      :title="$t('compute.vmGroup.addVm')"
    >
      <div class="block overflow-hidden">
        <el-row :gutter="10">
          <el-col :span="24">
            <el-table
              ref="multipleTableRef"
              v-loading="vmLoading"
              :size="mainStoreData.viewSize.main"
              :element-loading-text="$t('common.loading')"
              :data="vmTableList"
              max-height="calc(100% - 3rem)"
              class="!overflow-y-auto hypervisorNodesDialog"
              stripe
              :scrollbar-always-on="false"
            >
              <el-table-column label="" width="40px">
                <template #default="scope">
                  <span v-if="JSON.stringify(vmTableData).includes(scope.row.instanceId)">
                    <span class="w-3 h-3 block border rounded-sm border-gray-300 bg-gray-300 text-base text-center">
                      <i-dashicons:yes class="text-white w-3.5 h-3.5 -m-0.5 leading-none table"></i-dashicons:yes>
                    </span>
                  </span>
                  <span v-else>
                    <span
                      v-if="!nowCheckVmIncludes(scope.row)"
                      class="w-3 h-3 block border rounded-sm border-gray-300"
                      @click="handleCheckChange(scope.row, true)"
                    ></span>
                    <span
                      v-else
                      class="w-3 h-3 block border rounded-sm border-blue-500 bg-blue-500 text-base text-center"
                      @click="handleCheckChange(scope.row, false)"
                    >
                      <i-dashicons:yes class="text-white w-3.5 h-3.5 -m-0.5 leading-none table"></i-dashicons:yes>
                    </span>
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="date" :label="$t('common.name')">
                <template #default="scope">
                  <router-link :to="'/vm/' + scope.row.instanceId" target="_blank">
                    <span class="text-blue-400 cursor-pointer">{{ scope.row.name }}</span>
                  </router-link>
                </template>
              </el-table-column>
              <el-table-column prop="hostname" :label="$t('compute.vmGroup.form.hostname')" />
              <el-table-column prop="portInfo.ipAddress" :label="$t('compute.vmGroup.form.ip')">
                <template #default="scope">
                  <span>{{ scope.row.portInfo.ipAddress || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="vpcInfo.cidr" :label="$t('compute.vmGroup.form.networkAddress')">
                <template #default="scope">
                  <span>{{ scope.row.subnetInfo.cidr || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="eip" :label="$t('compute.vmGroup.form.eip')">
                <template #default="scope">
                  <span v-if="!scope.row.boundPhaseStatus">{{ '-' }}</span>
                  <span v-if="scope.row.boundType && scope.row.boundType === 'port'">
                    <span v-if="scope.row.boundPhaseStatus == 82">{{ scope.row.eip || '-' }}</span>
                    <el-tag
                      v-else
                      :size="mainStoreData.viewSize.tagStatus"
                      :type="filtersFun.getVmToEipStatus(scope.row.boundPhaseStatus, 'tag')"
                      >{{ filtersFun.getVmToEipStatus(scope.row.boundPhaseStatus, 'status') }}</el-tag
                    >
                  </span>
                  <span v-if="scope.row.boundType && scope.row.boundType === 'nat'">
                    <span v-if="scope.row.boundPhaseStatus == 7">
                      <span v-if="scope.row.eip">
                        <el-tag :size="mainStoreData.viewSize.tagStatus">{{ $t('compute.vmGroup.form.nat') }}</el-tag>
                        {{ scope.row.eip }}
                      </span>
                      <span v-else>-</span>
                    </span>
                    <el-tag
                      v-else
                      :size="mainStoreData.viewSize.tagStatus"
                      :type="filtersFun.getNatStatus(scope.row.boundPhaseStatus, 'tag')"
                      >{{ filtersFun.getNatStatus(scope.row.boundPhaseStatus, 'status') }}</el-tag
                    >
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="phaseStatus" :label="$t('common.status')">
                <template #default="scope">
                  <el-tag
                    :size="mainStoreData.viewSize.tagStatus"
                    :type="filtersFun.getVmStatus(scope.row.phaseStatus, 'tag')"
                    >{{ filtersFun.getVmStatus(scope.row.phaseStatus, 'status') }}</el-tag
                  >
                </template>
              </el-table-column>
              <el-table-column prop="imageInfo.name" :label="$t('compute.vmGroup.form.system')" />
              <el-table-column prop="createTime" :label="$t('common.createTime')" />
            </el-table>
            <el-pagination
              v-model:page_num="vmForm.page_num"
              v-model:page-size="vmForm.page_size"
              class="!pt-4 !pr-8 float-right"
              :page-sizes="[10]"
              :current-page="vmForm.page_num"
              :small="true"
              layout="total, prev, pager, next, jumper"
              :total="vmForm.total"
              @size-change="transferHandleSizeChange"
              @current-change="transferHandleCurrentChange"
            />
          </el-col>
        </el-row>
      </div>
      <div class="dialog-footer text-center">
        <el-button type="text" @click="vmHandleClose()">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="toAddVm()">{{ $t('common.add') }}</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import mainApi from '@/api/modules/main';
import mainStore from '@/store/mainStore';
import filtersFun from '@/utils/statusFun';

const { drawerData } = defineProps<{
  drawerData: any;
}>();
const { proxy }: any = getCurrentInstance();

const mainStoreData = mainStore(); // pinia 信息
const router = useRouter();
const loading = ref(false);
const addVmForm = ref<any>();
const tableData: any = ref([]);
const vpcList: any = ref([]); // 虚拟私有云列表
const subnetsDataList: any = ref([]); // 子网列表
const vpcId = ref(''); // 虚拟私有云id
const subnetId = ref(''); // 子网id

const form = reactive({
  // 表单
  name: '',
  description: '',
});

const rules = reactive({
  name: [{ required: true, validator: proxy.$scriptMain.validateName, trigger: 'change' }],
});

const goBack = () => {
  router.push('/vmGroup');
};
const resetForm = () => {
  // 重置
  addVmForm.value.resetFields();
};
const toVmGroupAdd = () => {
  // 虚机组add
  loading.value = true;
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  const data = {
    name: form.name,
    description: form.description,
    vmInstanceIds: vmTableData.value.map((item: any) => item.instanceId),
  };
  addVmForm.value.validate(async (valid: any) => {
    if (valid) {
      mainApi
        .vmInstanceGroupsEdit(data, id)
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

const vmTableData: any = ref([]); // 已选虚拟机列表
const nowCheckVm: any = ref([]); // 已选虚拟机列表
const vmTableList: any = ref([]); // 虚拟机列表
const dialogVm = ref(false); // 虚拟机弹窗
const vmLoading = ref(false); // 虚拟机弹窗loading
const vmForm = reactive({
  // 搜索 筛选
  name: '',
  page_num: 1,
  page_size: 10,
  total: 0,
});
const vmHandleClose = () => {
  nowCheckVm.value = [];
  vmForm.name = '';
  vmForm.page_num = 1;
  dialogVm.value = false;
};
const toAddVm = () => {
  // 添加虚拟机

  if (nowCheckVm.value.length === 0) {
    ElMessage.warning(proxy.$t('compute.vmGroup.message.selectVm'));
    return;
  }
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }

  addVmForm.value.validate(async (valid: any) => {
    if (valid) {
      vmTableData.value = [...vmTableData.value, ...nowCheckVm.value];

      const data = {
        name: form.name,
        description: form.description,
        vmInstanceIds: vmTableData.value.map((item: any) => item.instanceId),
      };
      mainApi.vmInstanceGroupsEdit(data, id).then((res: any) => {
        vmHandleClose();
        getDetail();
      });
    } else {
      vmHandleClose();

      loading.value = false;
    }
  });
};
const toDelete = (item: any, index: any) => {
  // 删除虚拟机
  if (item.isAdd) {
    vmTableData.value.splice(index, 1);
    return true;
  }
  // vmTableData.value.splice(index, 1);
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  mainApi
    .vmInstanceGroupsDelVm(id, item.instanceId)
    .then((res: any) => {
      ElMessage.success(proxy.$t('compute.vmGroup.message.removeSuccess'));
      getVmsInstabcesDetail(id);
    })
    .catch((error: any) => {});
  return true;
};
const nowCheckVmIncludes = (val: any) => {
  const data: any = nowCheckVm.value.filter((item: any) => item.instanceId === val.instanceId);
  return data.length > 0;
};
const handleCheckChange = (val: any, type: boolean) => {
  if (type) {
    nowCheckVm.value.push(val);
  } else {
    nowCheckVm.value = nowCheckVm.value.filter((item: any) => item !== val);
  }
};
const getVmsInstabcesList = () => {
  // 虚拟机列表
  vmLoading.value = true;

  const params: any = {
    name: vmForm.name,
    page_num: vmForm.page_num,
    page_size: vmForm.page_size,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }
  mainApi
    .vmsInstabcesList({ instance_group_id_is_null: true, ...params })
    .then((res: any) => {
      vmLoading.value = false;

      vmTableList.value = res.vmInstancesInfo;
      vmForm.total = res.totalNum;
    })
    .catch((error: any) => {
      vmLoading.value = false;
    });
};
const transferHandleSizeChange = (val: any) => {
  // 改变每页显示数量
  vmForm.page_size = val;
  getVmsInstabcesList();
};
const transferHandleCurrentChange = (val: any) => {
  // 改变页码
  vmForm.page_num = val;
  getVmsInstabcesList();
};
const addInstance = () => {
  // 点击添加虚拟机
  dialogVm.value = true;
  getVmsInstabcesList();
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
    .vmInstanceGroupsDetail(id)
    .then((res: any) => {
      form.name = res.name;
      form.description = res.description;
      getVmsInstabcesDetail(res.instanceGroupId);
    })
    .catch((error: any) => {});
};
const getVmsInstabcesDetail = (id: any) => {
  mainApi
    .vmsInstabcesList({ instance_group_id: id })
    .then((res: any) => {
      vmTableData.value = res.vmInstancesInfo ? res.vmInstancesInfo : [];
    })
    .catch((error: any) => {});
};
onMounted(() => {
  // 详情
  getDetail();
});
</script>

<style lang="scss" scoped>
.snapsAddPage {
}
</style>

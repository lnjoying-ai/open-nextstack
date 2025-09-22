<template>
  <div class="subNetPage h-full">
    <!-- <h5 class="mb-3 px-5 pt-2">
      <span class="text-lg">{{ $route.meta.title }}</span>
      <el-divider class="!my-2"></el-divider>
    </h5> -->
    <div class="py-3 px-4 mb-4 bg-white rounded-lg tableTop">
      <el-form :model="form" :inline="true" :size="mainStoreData.viewSize.main">
        <el-form-item :label="$t('storage.volumes.form.volumeName') + ':'">
          <el-input v-model="form.name" class="!w-50" :placeholder="$t('storage.volumes.form.inputVolumeName')" />
        </el-form-item>
        <el-form-item :label="$t('storage.volumes.form.storagePool') + ':'">
          <el-select
            v-model="form.storage_pool_id"
            class="!w-50"
            :placeholder="$t('storage.volumes.form.inputStoragePool')"
            @change="onSubmit"
          >
            <el-option :label="$t('storage.volumes.form.all')" :value="''" />
            <el-option v-for="(item, index) in storagePoolsList" :key="index" :label="item.name" :value="item.poolId" />
          </el-select>
        </el-form-item>
        <el-form-item class="float-right !mr-0">
          <el-button class="resetBtn w-24" @click="onReset">{{ $t('common.reset') }}</el-button>
          <el-button type="primary" class="w-24" @click="onSubmit">{{ $t('common.search') }}</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="py-3 px-4 bg-white rounded-lg h-table">
      <div class="table-top-style overflow-hidden mb-3">
        <el-radio-group v-model="tabContent" class="overflow-hidden align-middle mr-4">
          <el-radio-button label="other">{{ $t('storage.volumes.form.availableResources') }}</el-radio-button>
          <el-radio-button label="detached">{{ $t('storage.volumes.form.detached') }}</el-radio-button>
          <el-radio-button label="recycle">{{ $t('storage.volumes.form.recycle') }}</el-radio-button>
        </el-radio-group>
        <el-button type="primary" class="float-right" @click="addVolumes">{{
          $t('storage.volumes.form.createDisk')
        }}</el-button>
      </div>

      <el-table
        v-loading="loading"
        :size="mainStoreData.viewSize.main"
        :element-loading-text="$t('common.loading')"
        :data="tableData"
        max-height="calc(100vh - 250px)"
        class="!overflow-y-auto"
        stripe
        :scrollbar-always-on="false"
      >
        <el-table-column prop="date" :label="$t('storage.volumes.form.name')">
          <template #default="scope">
            <span class="text-blue-400 cursor-pointer" @click="toDetail(scope.row)">{{ scope.row.name }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="type" :label="$t('storage.volumes.form.type')">
          <template #default="scope">
            <span v-if="scope.row.type == 2">{{ $t('storage.volumes.form.fileSystem') }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="size" :label="$t('storage.volumes.form.size')">
          <template #default="scope"> {{ scope.row.size }}GB </template>
        </el-table-column>
        <el-table-column prop="phaseStatus" :label="$t('storage.volumes.form.status')">
          <template #default="scope">
            <el-tag
              :size="mainStoreData.viewSize.tagStatus"
              :type="filtersFun.getVolumeStatus(scope.row.phaseStatus, 'tag')"
              >{{ filtersFun.getVolumeStatus(scope.row.phaseStatus, 'status') }}</el-tag
            >
          </template>
        </el-table-column>

        <el-table-column v-if="tabContent != 'detached'" prop="vmName" :label="$t('storage.volumes.form.instance')">
          <template #default="scope">
            <router-link :to="'/vm/' + scope.row.vmInstanceId" class="text-blue-400 mr-2">
              <span>{{ scope.row.vmName || '-' }}</span>
            </router-link>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" :label="$t('storage.volumes.form.createTime')" />
        <el-table-column :label="$t('common.operation')" width="120">
          <template #default="scope">
            <el-dropdown trigger="click" :size="mainStoreData.viewSize.listSet">
              <el-button type="text" :size="mainStoreData.viewSize.listSet">
                {{ $t('common.operation') }}<i-ic:baseline-keyboard-arrow-down></i-ic:baseline-keyboard-arrow-down>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu v-if="tabContent != 'recycle'">
                  <el-dropdown-item v-if="tabContent == 'detached'">
                    <span class="w-full" @click="openVolumesAttach(scope.row)"
                      ><img src="@/assets/img/btn/qy.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                        $t('common.attach')
                      }}</span
                    >
                  </el-dropdown-item>
                  <el-popconfirm
                    v-if="scope.row.phaseStatus == '26'"
                    :confirm-button-text="$t('common.detach')"
                    :cancel-button-text="$t('common.cancel')"
                    icon-color="#626AEF"
                    :title="$t('storage.volumes.message.confirmDetach')"
                    @confirm="toVolumesDetach(scope.row)"
                  >
                    <template #reference>
                      <span class="listDelBtn"
                        ><img src="@/assets/img/btn/delete.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                          $t('common.detach')
                        }}</span
                      >
                    </template>
                  </el-popconfirm>

                  <el-dropdown-item>
                    <span class="w-full" @click="toEdit(scope.row)"
                      ><img src="@/assets/img/btn/edit.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                        $t('common.edit')
                      }}</span
                    ></el-dropdown-item
                  >
                  <el-dropdown-item>
                    <span class="w-full" @click="openSnaps(scope.row)"
                      ><img src="@/assets/img/btn/snaps.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                        $t('common.snapshots')
                      }}</span
                    >
                  </el-dropdown-item>
                </el-dropdown-menu>
                <el-dropdown-menu v-else>
                  <el-dropdown-item>
                    <span class="w-full" @click="addVm(scope.row)"
                      ><img src="@/assets/img/btn/rollBack.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                        $t('common.rollback')
                      }}</span
                    >
                  </el-dropdown-item>
                </el-dropdown-menu>
                <el-popconfirm
                  :confirm-button-text="$t('common.delete')"
                  :cancel-button-text="$t('common.cancel')"
                  icon-color="#626AEF"
                  :title="$t('storage.volumes.message.confirmDelete')"
                  @confirm="toDelete(scope.row)"
                >
                  <template #reference>
                    <span class="listDelBtn"
                      ><img src="@/assets/img/btn/delete.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                        $t('common.delete')
                      }}</span
                    >
                  </template>
                </el-popconfirm>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <el-pagination
      v-model:page_num="form.page_num"
      v-model:page-size="form.page_size"
      class="!py-4 !pr-8 float-right"
      :page-sizes="mainStoreData.page_sizes"
      :current-page="form.page_num"
      :small="true"
      layout="total, sizes, prev, pager, next, jumper"
      :total="form.total"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />

    <el-dialog
      v-model="dialogVm"
      v-loading="vmLoading"
      :close-on-click-modal="false"
      width="1200px"
      destroy-on-close
      :element-loading-text="$t('common.loading')"
      :before-close="vmHandleClose"
      :title="$t('common.attach')"
    >
      <div class="block overflow-hidden">
        <el-row :gutter="10">
          <el-col :span="24">
            <el-table
              ref="multipleTableRef"
              v-loading="vmLoading"
              :size="mainStoreData.viewSize.main"
              :element-loading-text="$t('common.loading')"
              :data="vmTableData"
              max-height="calc(100% - 3rem)"
              class="!overflow-y-auto hypervisorNodesDialog"
              stripe
              :scrollbar-always-on="false"
              @current-change="handleCheckChange"
            >
              <el-table-column label="" width="40px">
                <template #default="scope">
                  <span
                    v-if="scope.row.instanceId != nowCheckVm.instanceId"
                    class="w-3 h-3 block border rounded-sm border-gray-300"
                  ></span>
                  <span
                    v-else
                    class="w-3 h-3 block border rounded-sm border-blue-500 bg-blue-500 text-base text-center"
                  >
                    <i-dashicons:yes class="text-white w-3.5 h-3.5 -m-0.5 leading-none table"></i-dashicons:yes>
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="date" :label="$t('storage.volumes.form.name')">
                <template #default="scope">
                  <router-link :to="'/vm/' + scope.row.instanceId">
                    <span class="text-blue-400 cursor-pointer">{{ scope.row.name }}</span>
                  </router-link>
                </template>
              </el-table-column>
              <el-table-column prop="hostname" :label="$t('storage.volumes.form.hostname')" />
              <el-table-column prop="portInfo.ipAddress" :label="$t('storage.volumes.form.ip')">
                <template #default="scope">
                  <span>{{ scope.row.portInfo.ipAddress || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="vpcInfo.cidr" :label="$t('storage.volumes.form.networkAddress')">
                <template #default="scope">
                  <span>{{ scope.row.subnetInfo.cidr || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="eip" :label="$t('storage.volumes.form.eip')">
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
                        <el-tag :size="mainStoreData.viewSize.tagStatus">{{ $t('storage.volumes.form.nat') }}</el-tag>
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
              <el-table-column prop="phaseStatus" :label="$t('storage.volumes.form.status')">
                <template #default="scope">
                  <el-tag
                    :size="mainStoreData.viewSize.tagStatus"
                    :type="filtersFun.getVmStatus(scope.row.phaseStatus, 'tag')"
                    >{{ filtersFun.getVmStatus(scope.row.phaseStatus, 'status') }}</el-tag
                  >
                </template>
              </el-table-column>
              <el-table-column prop="imageInfo.name" :label="$t('storage.volumes.form.system')" />
              <el-table-column prop="createTime" :label="$t('storage.volumes.form.createTime')" />
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
        <el-button type="primary" @click="toVolumesAttach()">{{ $t('common.attach') }}</el-button>
      </div>
    </el-dialog>
    <el-dialog
      v-model="dialogFormVisible"
      :close-on-click-modal="false"
      width="600px"
      destroy-on-close
      :before-close="snapsHandleClose"
      :title="$t('storage.volumes.form.addSnaps.title')"
    >
      <el-form
        ref="snapsformRef"
        v-loading="loadingDialog"
        :rules="rules"
        :model="snapsform"
        label-width="80px"
        :element-loading-text="$t('common.loading')"
      >
        <el-form-item :label="$t('storage.volumes.form.addSnaps.volume') + ':'">
          <el-input v-model="nowVolumesData.name" :disabled="true" autocomplete="off" placeholder="-" />
        </el-form-item>
        <el-form-item :label="$t('storage.volumes.form.addSnaps.name') + ':'" prop="name">
          <el-input
            v-model="snapsform.name"
            autocomplete="off"
            :placeholder="$t('storage.volumes.form.addSnaps.namePlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="$t('storage.volumes.form.addSnaps.description') + ':'" prop="description">
          <el-input
            v-model="snapsform.description"
            type="textarea"
            :rows="2"
            autocomplete="off"
            :placeholder="$t('storage.volumes.form.addSnaps.descriptionPlaceholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button type="primary" @click="toSnaps()">{{ $t('common.createNow') }}</el-button>
        </span>
      </template>
    </el-dialog>
    <el-drawer
      v-model="drawer"
      :title="drawerData.title"
      :direction="drawerData.direction"
      :size="drawerData.size"
      :before-close="handleClose"
    >
      <component
        :is="currentView"
        :drawer-data="drawerData"
        class="table w-full"
        @closeDrawer="closeDrawer"
      ></component>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import mainApi from '@/api/modules/main';
import filtersFun from '@/utils/statusFun';
import mainStore from '@/store/mainStore';

import volumesadd from './add.vue';
import vmAdd from './vmAdd.vue';
import volumesedit from './edit.vue';
import volumesdetail from './detail.vue';

const { proxy }: any = getCurrentInstance();

const mainStoreData = mainStore(); // pinia 信息
const router = useRouter();
const dialogFormVisible = ref(false);
const nowVolumesData: any = ref('');
const snapsformRef = ref<any>();
const loadingDialog = ref(false);

const snapsform = reactive({
  name: '',
  description: '',
  volumeId: '',
});
const rules = reactive({
  name: [{ required: true, validator: proxy.$scriptMain.validateName, trigger: 'change' }],
});

const timer: any = ref('');
const tabContent = ref('detached');
const loading = ref(false);
const vmLoading = ref(false);
const dialogVm = ref(false);
const vmTableData: any = ref([]);
const nowCheckVm: any = ref('');
const nowCheckVolumes: any = ref('');
const vmForm = reactive({
  // 搜索 筛选
  name: '',
  page_num: 1,
  page_size: 10,
  total: 0,
});

const drawer = ref(false);
const drawerData: any = ref('');
const currentView: any = ref(volumesdetail);
const closeDrawer = () => {
  drawer.value = false;
  currentView.value = '';
  drawerData.value = '';
  getVolumesList();
};
const addVolumes = () => {
  drawerData.value = {
    title: proxy.$t('storage.volumes.page.createVolume'),
    closeText: proxy.$t('storage.volumes.page.closeCreateVolume'),
    direction: 'rtl',
    size: '80%',
    close: true,
    isDrawer: true,
    link: '/volumesAdd',
    linkName: proxy.$t('storage.volumes.page.openCreateVolume'),
    id: '',
  };
  currentView.value = volumesadd;
  drawer.value = true;
};
const addVm = (item: any) => {
  drawerData.value = {
    title: proxy.$t('storage.volumes.page.restoreSystem'),
    closeText: proxy.$t('storage.volumes.page.closeRestoreSystem'),
    direction: 'rtl',
    size: '80%',
    close: true,
    isDrawer: true,
    link: `/vmAdd/${item.volumeId}`,
    linkName: proxy.$t('storage.volumes.page.openRestoreSystem'),
    id: item.volumeId,
    item,
  };
  currentView.value = vmAdd;
  drawer.value = true;
};
const toEdit = (item: any) => {
  // 编辑
  drawerData.value = {
    title: proxy.$t('storage.volumes.page.editVolume'),
    closeText: proxy.$t('storage.volumes.page.closeEditVolume'),
    direction: 'rtl',
    size: '80%',
    close: true,
    isDrawer: true,
    link: `/volumesEdit/${item.volumeId}`,
    linkName: proxy.$t('storage.volumes.page.openEditVolume'),
    id: item.volumeId,
  };
  currentView.value = volumesedit;
  drawer.value = true;
};
const toDetail = (item: any) => {
  // 详情
  drawerData.value = {
    title: proxy.$t('storage.volumes.page.volumeDetail'),
    closeText: proxy.$t('storage.volumes.page.closeVolumeDetail'),
    direction: 'rtl',
    size: '80%',
    close: false,
    isDrawer: true,
    link: `/volumes/${item.volumeId}`,
    linkName: proxy.$t('storage.volumes.page.openVolumeDetail'),
    id: item.volumeId,
  };
  currentView.value = volumesdetail;

  drawer.value = true;
};

const handleClose = (done: () => void) => {
  if (drawerData.value.close) {
    ElMessageBox.confirm(drawerData.value.closeText)
      .then(() => {
        currentView.value = '';
        done();
      })
      .catch(() => {
        // catch error
      });
  } else {
    currentView.value = '';

    done();
  }
};

const form = reactive({
  // 搜索 筛选
  name: '',
  storage_pool_id: '',
  page_num: 1,
  page_size: mainStoreData.page_size,
  total: 0,
});
const storagePoolsList: any = ref([]);
const tableData: any = ref([]);
const handleSizeChange = (val: any) => {
  // 改变每页显示数量
  localStorage.setItem('page_size', val);
  mainStoreData.page_size = val;
  form.page_size = val;
  getVolumesList();
};
const handleCurrentChange = (val: any) => {
  // 改变页码
  form.page_num = val;
  getVolumesList();
};
const handleCheckChange = (val: any) => {
  // 选中
  nowCheckVm.value = val;
};
const openVolumesAttach = (item: any) => {
  // 挂载
  dialogVm.value = true;
  nowCheckVolumes.value = item;
};
const toVolumesAttach = () => {
  if (!nowCheckVm.value) {
    ElMessage.warning(proxy.$t('storage.volumes.message.selectVirtualMachine'));
    return false;
  }
  // 挂载
  mainApi
    .volumesAttach({ vmId: nowCheckVm.value.instanceId }, nowCheckVolumes.value.volumeId)
    .then((res: any) => {
      ElMessage.success(proxy.$t('storage.volumes.message.startMount'));
      getVolumesList();
      vmHandleClose();
    })
    .catch((error: any) => {});
  return true;
};
const vmHandleClose = () => {
  nowCheckVm.value = '';
  vmForm.name = '';
  vmForm.page_num = 1;
  dialogVm.value = false;
};
const toVolumesDetach = (item: any) => {
  // 卸载
  mainApi
    .volumesDetach(item.volumeId)
    .then((res: any) => {
      ElMessage.success(proxy.$t('storage.volumes.message.startDetach'));
      getVolumesList();
    })
    .catch((error: any) => {});
  return true;
};
const toDelete = (item: any) => {
  // 删除
  mainApi
    .volumesDel(item.volumeId)
    .then((res: any) => {
      loading.value = false;
      ElMessage.success(proxy.$t('storage.volumes.message.startDelete'));
      getVolumesList();
    })
    .catch((error: any) => {
      loading.value = false;
    });
  return true;
};
const onSubmit = () => {
  // 提交查询
  form.page_num = 1;
  getVolumesList();
};
const onReset = () => {
  // 重置查询
  form.name = '';
  form.storage_pool_id = '';
  form.page_num = 1;
  getVolumesList();
};
const getVolumesList = () => {
  // 云盘列表
  loading.value = true;
  const params: any = {
    name: form.name,
    storage_pool_id: form.storage_pool_id,
    page_num: form.page_num,
    page_size: form.page_size,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }
  if (tabContent.value === 'detached') {
    params.detached = true;
  }
  if (tabContent.value === 'other') {
    params.detached = false;
  }
  if (tabContent.value === 'recycle') {
    mainApi
      .volumesRecycleList(params)
      .then((res: any) => {
        loading.value = false;
        tableData.value = res.volumes;
        form.total = res.totalNum;
      })
      .catch((error: any) => {
        loading.value = false;
      });
    return;
  }
  mainApi
    .volumesList(params)
    .then((res: any) => {
      loading.value = false;
      tableData.value = res.volumes;
      form.total = res.totalNum;
    })
    .catch((error: any) => {
      loading.value = false;
    });
};
const getVolumesListTime = () => {
  // 云盘列表
  const params: any = {
    name: form.name,
    storage_pool_id: form.storage_pool_id,
    page_num: form.page_num,
    page_size: form.page_size,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }
  if (tabContent.value === 'detached') {
    params.detached = true;
  }
  if (tabContent.value === 'other') {
    params.detached = false;
  }
  if (tabContent.value === 'recycle') {
    mainApi
      .volumesRecycleList(params)
      .then((res: any) => {
        loading.value = false;
        tableData.value = res.volumes;
        form.total = res.totalNum;
      })
      .catch((error: any) => {
        loading.value = false;
      });
    return;
  }
  mainApi.volumesList(params).then((res: any) => {
    tableData.value = res.volumes;
    form.total = res.totalNum;
  });
};
const getVmsInstabcesList = () => {
  // 虚机列表
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
    .vmsInstabcesList(params)
    .then((res: any) => {
      vmLoading.value = false;
      vmTableData.value = res.vmInstancesInfo;
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
const getStoragePools = () => {
  // 存储池列表
  mainApi.storagePoolsList().then((res: any) => {
    storagePoolsList.value = res.storagePools;
  });
};
const openSnaps = (item: any) => {
  dialogFormVisible.value = true;
  nowVolumesData.value = item;
  // 快照弹窗
};
const snapsHandleClose = (done: () => void) => {
  resetForm();
  done();
};
const resetForm = () => {
  // 重置
  snapsformRef.value.resetFields();
};
const toSnaps = () => {
  // 创建快照
  // 快照add
  loadingDialog.value = true;
  snapsformRef.value.validate(async (valid: any) => {
    if (valid) {
      snapsform.volumeId = nowVolumesData.value.volumeId;

      mainApi
        .volumesSnapsAdd(snapsform)
        .then((res: any) => {
          ElMessage.success(proxy.$t('storage.volumes.message.startCreate'));
          loadingDialog.value = false;
          dialogFormVisible.value = false;
          resetForm();
        })
        .catch((error: any) => {
          loadingDialog.value = false;
        });
    } else {
      loadingDialog.value = false;
    }
  });
};
watch(tabContent, (newValue) => {
  onReset();
});
onMounted(() => {
  getStoragePools(); // 存储池列表
  getVolumesList(); // 云盘列表
  getVmsInstabcesList();
  timer.value = setInterval(async () => {
    getVolumesListTime(); // 请求EIP循环列表
  }, mainStoreData.listRefreshTime);
});
onUnmounted(() => {
  clearInterval(timer.value);
});
</script>

<style lang="scss" scoped>
.subNetPage {
}
</style>

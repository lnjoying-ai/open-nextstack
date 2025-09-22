<template>
  <div class="subNetPage h-full">
    <!-- <h5 class="mb-3 px-5 pt-2">
      <span class="text-lg">{{ $route.meta.title }}</span>
      <el-divider class="!my-2"></el-divider>
    </h5> -->
    <div class="py-3 px-4 mb-4 bg-white rounded-lg tableTop">
      <el-form :model="form" :inline="true" :size="mainStoreData.viewSize.main">
        <el-form-item :label="$t('network.eip.form.eip') + ':'">
          <el-input v-model="form.name" class="!w-50" :placeholder="$t('network.eip.form.inputEip')" />
        </el-form-item>

        <el-form-item class="float-right !mr-0">
          <el-button class="resetBtn w-24" @click="onReset">{{ $t('common.reset') }}</el-button>

          <el-button type="primary" class="w-24" @click="onSubmit">{{ $t('common.search') }}</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="py-3 px-4 bg-white rounded-lg h-table">
      <div class="table-top-style overflow-hidden mb-3">
        <div
          class="table-total float-left px-3 py-0.8 text-sm text-gray-400 border rounded-sm border-gray-200 border-solid"
        >
          <i18n-t keypath="common.totalNum" tag="span">
            <template #count>
              <span class="text-blue-400 font-bold">{{ form.total || 0 }}</span>
            </template>
          </i18n-t>
        </div>

        <el-button type="primary" class="float-right" @click="addEip">{{ $t('network.eip.form.createEip') }}</el-button>
      </div>

      <el-table
        v-loading="loading"
        :size="mainStoreData.viewSize.main"
        :element-loading-text="$t('common.loading')"
        :data="tableData"
        max-height="calc(100% - 3rem)"
        class="!overflow-y-auto"
        stripe
        :scrollbar-always-on="false"
      >
        <el-table-column prop="date" :label="$t('network.eip.form.eip')">
          <template #default="scope">
            {{ scope.row.ipAddress }}
          </template>
        </el-table-column>
        <el-table-column prop="addressType" :label="$t('network.eip.form.ipv4Address')">
          <template #default="scope">
            <span>{{ scope.row.addressType === 0 ? 'IPv4' : 'Ipv6' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="boundType" :label="$t('network.eip.form.boundType')">
          <template #default="scope">
            <span v-if="scope.row.boundType">
              <span>
                <span v-if="scope.row.boundType === 'port'">{{ $t('network.eip.form.port') }}</span>
                <span v-if="scope.row.boundType === 'nat'">{{ $t('network.eip.form.nat') }}</span>
              </span>
            </span>
            <span v-else> - </span>
          </template>
        </el-table-column>
        <el-table-column prop="boundName" :label="$t('network.eip.form.boundName')">
          <template #default="scope">
            <span v-if="scope.row.boundType">
              <span v-if="scope.row.boundName">
                <el-tag v-for="(item, index) in getBoundName(scope.row.boundName)" :key="index" class="mr-1 mb-1">{{
                  item
                }}</el-tag>
              </span>
            </span>
            <span v-else> - </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="$t('network.eip.form.createTime')" />
        <el-table-column :label="$t('common.operation')" width="120">
          <template #default="scope">
            <el-dropdown trigger="click" :size="mainStoreData.viewSize.listSet">
              <el-button type="text" :size="mainStoreData.viewSize.listSet">
                {{ $t('common.operation') }}<i-ic:baseline-keyboard-arrow-down></i-ic:baseline-keyboard-arrow-down>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="!scope.row.boundId">
                    <span class="w-full" @click="toOpenVm(scope.row)"
                      ><img src="@/assets/img/btn/attach.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                        $t('network.eip.form.bindVm')
                      }}</span
                    ></el-dropdown-item
                  >
                  <el-dropdown-item v-else>
                    <span>
                      <span class="w-full" @click="toDetach(scope.row)"
                        ><img src="@/assets/img/btn/detach.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                          $t('common.unbind')
                        }}</span
                      >
                    </span>
                  </el-dropdown-item>
                  <el-popconfirm
                    :confirm-button-text="$t('common.delete')"
                    :cancel-button-text="$t('common.cancel')"
                    icon-color="#626AEF"
                    :title="$t('network.eip.message.confirmDelete')"
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
                </el-dropdown-menu>
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
      :title="$t('network.eip.form.vm.title')"
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
              <el-table-column prop="date" :label="$t('network.eip.form.vm.name')">
                <template #default="scope">
                  <router-link :to="'/vm/' + scope.row.instanceId">
                    <span class="text-blue-400 cursor-pointer">{{ scope.row.name }}</span>
                  </router-link>
                </template>
              </el-table-column>
              <el-table-column prop="hostname" :label="$t('network.eip.form.vm.hostname')" />
              <el-table-column prop="portInfo.ipAddress" :label="$t('network.eip.form.vm.ip')">
                <template #default="scope">
                  <span>{{ scope.row.portInfo.ipAddress || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="vpcInfo.cidr" :label="$t('network.eip.form.vm.networkAddress')">
                <template #default="scope">
                  <span>{{ scope.row.subnetInfo.cidr || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="eip" :label="$t('network.eip.form.vm.eip')">
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
                        <el-tag :size="mainStoreData.viewSize.tagStatus">{{ $t('network.eip.form.vm.nat') }}</el-tag>
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
              <el-table-column prop="phaseStatus" :label="$t('network.eip.form.vm.status')">
                <template #default="scope">
                  <el-tag
                    :size="mainStoreData.viewSize.tagStatus"
                    :type="filtersFun.getVmStatus(scope.row.phaseStatus, 'tag')"
                    >{{ filtersFun.getVmStatus(scope.row.phaseStatus, 'status') }}</el-tag
                  >
                </template>
              </el-table-column>
              <el-table-column prop="imageInfo.name" :label="$t('network.eip.form.vm.system')" />
              <el-table-column prop="createTime" :label="$t('common.createTime')" />
            </el-table>
            <el-pagination
              v-model:page_num="vmForm.page_num"
              v-model:page-size="vmForm.page_size"
              class="!pt-4 !pr-8 float-right"
              :current-page="vmForm.page_num"
              :page-sizes="[10]"
              :small="true"
              layout="total, prev, pager, next, jumper"
              :total="vmForm.total"
              @size-change="vmHandleSizeChange"
              @current-change="vmHandleCurrentChange"
            />
          </el-col>
        </el-row>
      </div>
      <div class="dialog-footer text-center">
        <el-button type="text" @click="vmHandleClose()">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="toEip()">{{ $t('common.confirm') }}</el-button>
      </div>
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
import mainStore from '@/store/mainStore';
import filtersFun from '@/utils/statusFun';

import eipadd from './add.vue';

const { proxy }: any = getCurrentInstance();
const router = useRouter();
const mainStoreData = mainStore(); // pinia 信息
const timer: any = ref('');

const loading = ref(false);

const drawer = ref(false);
const drawerData: any = ref('');
const currentView: any = ref(eipadd);
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
  page_num: 1,
  page_size: mainStoreData.page_size,
  total: 0,
});
const tableData: any = ref([]);

const vmLoading = ref(false);
const dialogVm = ref(false);
const vmTableData: any = ref([]);
const nowCheckEip: any = ref('');
const nowCheckVm: any = ref('');
const vmForm = reactive({
  // 搜索 筛选
  name: '',
  page_num: 1,
  page_size: 10,
  total: 0,
});

const closeDrawer = () => {
  drawer.value = false;
  currentView.value = '';
  drawerData.value = '';
  getEipList();
};
const addEip = () => {
  drawerData.value = {
    title: proxy.$t('network.eip.page.createEip'),
    closeText: proxy.$t('network.eip.page.closeCreateEip'),
    direction: 'rtl',
    size: '80%',
    close: true,
    isDrawer: true,
    link: '/eipAdd',
    linkName: proxy.$t('network.eip.page.openCreateEip'),
    id: '',
  };
  currentView.value = eipadd;
  drawer.value = true;
};

const handleSizeChange = (val: any) => {
  // 改变每页显示数量
  localStorage.setItem('page_size', val);
  mainStoreData.page_size = val;
  form.page_size = val;
  getEipList();
};
const handleCurrentChange = (val: any) => {
  // 改变页码
  console.log(`current page: ${val}`);
  form.page_num = val;
  getEipList();
};

const toDelete = (item: any) => {
  // 删除
  mainApi
    .eipsDel(item.eipId)
    .then((res: any) => {
      loading.value = false;
      ElMessage.success(proxy.$t('common.operations.success.delete'));
      getEipList();
    })
    .catch((error: any) => {
      loading.value = false;
    });
  return true;
};
const onSubmit = () => {
  // 提交查询
  form.page_num = 1;
  getEipList();
};
const onReset = () => {
  // 重置查询
  form.name = '';
  form.page_num = 1;
  getEipList();
};
const getEipList = () => {
  // eip列表
  loading.value = true;
  const params: any = {
    name: form.name,
    page_num: form.page_num,
    page_size: form.page_size,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }
  mainApi
    .eipsList(params)
    .then((res: any) => {
      loading.value = false;
      tableData.value = res.eips;
      form.total = res.totalNum;
    })
    .catch((error: any) => {
      loading.value = false;
    });
};
const getEipListTime = () => {
  // eip列表
  const params: any = {
    name: form.name,
    page_num: form.page_num,
    page_size: form.page_size,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }
  mainApi.eipsList(params).then((res: any) => {
    tableData.value = res.eips;
    form.total = res.totalNum;
  });
};
const toDetach = (item: any) => {
  // 解绑
  // v-if="scope.row.boundType === 'port'"
  if (item.boundType === 'port') {
    router.push({
      name: 'vm',
      params: {
        eipId: item.eipId,
        vmName: item.boundName,
        autoDetachEip: 1,
      },
    });
  } else if (item.boundType === 'nat') {
    router.push({
      name: 'Nat',
      params: {
        eipId: item.eipId,
      },
    });
  }
};
const toOpenVm = (item: any) => {
  // 关联虚拟机
  dialogVm.value = true;
  nowCheckEip.value = item;
  getVmsInstabcesList();
};
const vmHandleClose = () => {
  nowCheckVm.value = '';
  vmForm.name = '';
  vmForm.page_num = 1;
  dialogVm.value = false;
};
const handleCheckChange = (val: any) => {
  // 选中
  nowCheckVm.value = val;
};
const vmHandleSizeChange = (val: any) => {
  // 改变每页显示数量
  vmForm.page_size = val;
  getVmsInstabcesList();
};
const vmHandleCurrentChange = (val: any) => {
  // 改变页码
  vmForm.page_num = val;
  getVmsInstabcesList();
};
const getVmsInstabcesList = () => {
  // 虚机列表
  vmLoading.value = true;

  const params: any = {
    name: vmForm.name,
    page_num: vmForm.page_num,
    page_size: vmForm.page_size,
    eip_id_is_null: true,
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
// 绑定EIP
const toEip = () => {
  if (nowCheckVm.value === '') {
    ElMessage.warning(proxy.$t('network.eip.message.selectVirtualMachine'));
    return false;
  }

  mainApi.eipsAttach(nowCheckEip.value.eipId, { portId: nowCheckVm.value.portInfo.portId }).then((res: any) => {
    ElMessage.success(proxy.$t('network.eip.message.startBind'));
    vmHandleClose();
  });
};
const getBoundName = (name: any) => {
  // 获取绑定名称
  return name.split(',');
};
onMounted(() => {
  getEipList(); // eip列表
  timer.value = setInterval(async () => {
    getEipListTime(); // 请求eip循环列表
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

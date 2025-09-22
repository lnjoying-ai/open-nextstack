<template>
  <div class="nodeDetailPage h-full">
    <h5 v-if="!(drawerData && drawerData.isDrawer)" class="bg-white mb-3 px-5 pt-2 pb-2 rounded-md">
      <el-page-header :title="$t('common.backToList')" @back="goBack">
        <template #content> {{ $route.meta.title }} </template>
      </el-page-header>
    </h5>
    <el-tabs v-model="activeName" class="demo-tabs bg-white nodeDetailTabs">
      <el-tab-pane :label="$t('hardware.hypervisorNodes.basicInfo')" name="first"></el-tab-pane>
      <el-tab-pane :label="$t('hardware.hypervisorNodes.virtualMachine')" name="second"></el-tab-pane>
      <el-tab-pane :label="$t('hardware.hypervisorNodes.pciDevices')" name="third"></el-tab-pane>
      <el-tab-pane :label="$t('hardware.hypervisorNodes.advancedConfig')" name="fourth"></el-tab-pane>
    </el-tabs>
    <el-form :model="form" label-width="120px" :size="mainStoreData.viewSize.main">
      <el-card v-if="activeName == 'first'" class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('hardware.hypervisorNodes.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('hardware.hypervisorNodes.form.name') + ':'">
            <span>{{ form.name || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('hardware.hypervisorNodes.form.id') + ':'">
            <span>{{ form.nodeId || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('hardware.hypervisorNodes.form.description') + ':'">
            <span>{{ form.description || '-' }}</span>
          </el-form-item>

          <el-form-item :label="$t('hardware.hypervisorNodes.form.manageIp') + ':'">
            <span>{{ form.manageIp || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('hardware.hypervisorNodes.form.cpuName') + ':'">
            <div v-if="form.cpuLogCount">
              <span
                >{{ form.cpuLogCount - form.usedCpuSum }}{{ $t('common.core') }}/{{ form.cpuLogCount
                }}{{ $t('common.core') }}</span
              >
              <small> {{ $t('hardware.hypervisorNodes.form.availableTotal') }}</small>
            </div>
            <div v-else>-</div>
          </el-form-item>
          <el-form-item :label="$t('hardware.hypervisorNodes.form.cpuModel') + ':'">
            <span>{{ form.cpuModel ? form.cpuModel : '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('hardware.hypervisorNodes.form.memoryName') + ':'">
            <div v-if="form.memTotal">
              <span>{{ form.memTotal - form.usedMemSum }}GB/{{ form.memTotal }}GB</span>
              <small> {{ $t('hardware.hypervisorNodes.form.availableTotal') }}</small>
            </div>
            <div v-else>-</div>
          </el-form-item>
          <el-form-item :label="$t('hardware.hypervisorNodes.form.ibNetworkName') + ':'">
            <div v-if="form.ibTotal">
              <span>{{ form.availableIbCount }}/{{ form.ibTotal }}</span>
              <small> {{ $t('hardware.hypervisorNodes.form.availableTotal') }}</small>
            </div>
            <div v-else>-</div>
          </el-form-item>

          <el-form-item :label="$t('hardware.hypervisorNodes.form.createTime') + ':'">
            <span>{{ form.createTime || '-' }}</span>
          </el-form-item>
        </div>
      </el-card>
      <el-card v-if="activeName == 'second'" class="!border-none mb-3">
        <template #header>
          <div class="">
            <span
              >{{ $t('hardware.hypervisorNodes.form.virtualMachine')
              }}<small
                >（{{ nodeVmTableTotal }}{{ $t('hardware.hypervisorNodes.form.virtualMachineUnit') }}）</small
              ></span
            >
            <el-button class="float-right" type="primary" @click="addVm">{{
              $t('hardware.hypervisorNodes.form.addVirtualMachine')
            }}</el-button>
          </div>
        </template>
        <div class="text item">
          <el-table
            :size="mainStoreData.viewSize.main"
            :data="nodeVmTableData"
            max-height="calc(100% - 3rem)"
            class="!overflow-y-auto"
            stripe
            :scrollbar-always-on="false"
          >
            <el-table-column prop="date" :label="$t('hardware.hypervisorNodes.form.name')">
              <template #default="scope">
                <router-link :to="'/vm/' + scope.row.instanceId" class="text-blue-400 mr-2">
                  <span>{{ scope.row.name }}</span>
                </router-link>
              </template>
            </el-table-column>

            <el-table-column prop="hostname" :label="$t('hardware.hypervisorNodes.form.hostname')" />
            <el-table-column prop="portInfo.ipAddress" :label="$t('hardware.hypervisorNodes.form.ip')">
              <template #default="scope">
                <span>{{ scope.row.portInfo.ipAddress || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="vpcInfo.cidr" :label="$t('hardware.hypervisorNodes.form.networkAddress')">
              <template #default="scope">
                <span>{{ scope.row.subnetInfo.cidr || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="eip" :label="$t('hardware.hypervisorNodes.form.eip')">
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
                      <el-tag :size="mainStoreData.viewSize.tagStatus">{{
                        $t('hardware.hypervisorNodes.form.nat')
                      }}</el-tag>
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
            <el-table-column prop="phaseStatus" :label="$t('hardware.hypervisorNodes.form.status')">
              <template #default="scope">
                <el-tag
                  :size="mainStoreData.viewSize.tagStatus"
                  :type="filtersFun.getVmStatus(scope.row.phaseStatus, 'tag')"
                  >{{ filtersFun.getVmStatus(scope.row.phaseStatus, 'status') }}</el-tag
                >
              </template>
            </el-table-column>
            <el-table-column prop="imageInfo.name" :label="$t('hardware.hypervisorNodes.form.system')" />
            <el-table-column prop="createTime" :label="$t('hardware.hypervisorNodes.form.createTime')" />
            <el-table-column :label="$t('common.operation')" width="120">
              <template #default="scope">
                <operate
                  :key="scope.row.instanceId"
                  :prop-vm-detail="scope.row"
                  :prop-show-type="1"
                  :prop-show-btn="[
                    'poweron',
                    'poweroff',
                    'reboot',
                    'delete',
                    'edit',
                    'resetPassword',
                    'eip',
                    'snaps',
                    'flavor',
                    'images',
                    'transfer',
                    'secGroup',
                    'vnc',
                  ]"
                  @initVmList="onReset"
                />
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-card>
      <el-card v-if="activeName == 'third'" class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('hardware.hypervisorNodes.pciDevices') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-table
            :size="mainStoreData.viewSize.main"
            :data="pciDeviceTableData"
            max-height="calc(100% - 3rem)"
            class="!overflow-y-auto"
            stripe
            :scrollbar-always-on="false"
          >
            <el-table-column
              prop="pciDeviceName"
              :label="$t('hardware.hypervisorNodes.form.pciDeviceName')"
              width="400"
            >
              <template #default="scope">
                <span>{{ scope.row.pciDeviceName }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="pciDeviceGroupId" :label="$t('hardware.hypervisorNodes.form.group')">
              <template #default="scope">
                <span>{{ scope.row.pciDeviceGroupId }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="pciDeviceType" :label="$t('hardware.hypervisorNodes.form.type')">
              <template #default="scope">
                <span>{{ scope.row.pciDeviceType }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="pciDeviceVendor" :label="$t('hardware.hypervisorNodes.form.virtualMachine')">
              <template #default="scope">
                <router-link
                  v-if="scope.row.vmInstanceName && scope.row.vmInstanceId"
                  :to="'/vm/' + scope.row.vmInstanceId"
                  class="text-blue-400 mr-2"
                >
                  <span>{{ scope.row.vmInstanceName }}</span>
                </router-link>

                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="phaseStatus" :label="$t('hardware.hypervisorNodes.form.status')">
              <template #default="scope">
                <el-tag
                  :size="mainStoreData.viewSize.tagStatus"
                  :type="filtersFun.getPciStatus(scope.row.phaseStatus, 'tag')"
                  >{{ filtersFun.getPciStatus(scope.row.phaseStatus, 'status') }}</el-tag
                >
              </template>
            </el-table-column>

            <el-table-column prop="createTime" :label="$t('hardware.hypervisorNodes.form.createTime')" />
            <!-- <el-table-column label="操作"
                             width="120">
              <template #default="scope">
                <el-dropdown trigger="click"
                             v-if="scope.row.phaseStatus==86"
                             :size="mainStoreData.viewSize.listSet">
                  <el-button type="text"
                             :size="mainStoreData.viewSize.listSet">
                    操作<i-ic:baseline-keyboard-arrow-down></i-ic:baseline-keyboard-arrow-down>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item>
                        <span class="w-full"
                              @click="openPciAttach(scope.row)">
                          <img src="@/assets/img/btn/qy.png"
                               class="w-3 float-left mt-3px mr-1"
                               alt="">挂载</span>
                      </el-dropdown-item>
                      <el-popconfirm confirm-button-text="卸载"
                                     cancel-button-text="取消"
                                     icon-color="#626AEF"
                                     title="确认卸载该设备吗？"
                                     @confirm="toPciDetach(scope.row)">
                        <template #reference>
                          <span class="listDelBtn"><img src="@/assets/img/btn/delete.png"
                                 class="w-3 float-left mt-3px mr-1"
                                 alt="">卸载</span>
                        </template>
                      </el-popconfirm>

                    </el-dropdown-menu>

                  </template>
                </el-dropdown>
              </template>
            </el-table-column> -->
          </el-table>
        </div>
      </el-card>
      <el-card v-if="activeName == 'fourth'" class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('hardware.hypervisorNodes.advancedConfig') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('hardware.hypervisorNodes.form.hostname') + ':'">
            <span>{{ form.hostname || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('hardware.hypervisorNodes.form.sysUsername') + ':'">
            <span>{{ form.sysUsername || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('hardware.hypervisorNodes.form.loginType') + ':'">
            <span>{{
              form.pubkeyId
                ? $t('hardware.hypervisorNodes.form.pubkeyId')
                : $t('hardware.hypervisorNodes.form.password')
            }}</span>
          </el-form-item>

          <el-form-item v-if="form.pubkeyId" :label="$t('hardware.hypervisorNodes.form.pubkeyId') + ':'">
            <span>
              <router-link :to="'/publicKey/' + form.pubkeyId" class="text-blue-400">{{ form.pubkeyId }}</router-link>
            </span>
          </el-form-item>
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
      :title="$t('hardware.hypervisorNodes.form.mount.title')"
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
              <el-table-column prop="date" :label="$t('hardware.hypervisorNodes.form.mount.name')">
                <template #default="scope">
                  <router-link :to="'/vm/' + scope.row.instanceId">
                    <span class="text-blue-400 cursor-pointer">{{ scope.row.name }}</span>
                  </router-link>
                </template>
              </el-table-column>
              <el-table-column prop="hostname" :label="$t('hardware.hypervisorNodes.form.mount.hostname')" />
              <el-table-column prop="portInfo.ipAddress" :label="$t('hardware.hypervisorNodes.form.mount.ip')">
                <template #default="scope">
                  <span>{{ scope.row.portInfo.ipAddress || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="vpcInfo.cidr" :label="$t('hardware.hypervisorNodes.form.mount.networkAddress')">
                <template #default="scope">
                  <span>{{ scope.row.subnetInfo.cidr || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="eip" :label="$t('hardware.hypervisorNodes.form.mount.eip')">
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
                        <el-tag :size="mainStoreData.viewSize.tagStatus">
                          {{ $t('hardware.hypervisorNodes.form.mount.nat') }}
                        </el-tag>
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
              <el-table-column prop="phaseStatus" :label="$t('hardware.hypervisorNodes.form.mount.status')">
                <template #default="scope">
                  <el-tag
                    :size="mainStoreData.viewSize.tagStatus"
                    :type="filtersFun.getVmStatus(scope.row.phaseStatus, 'tag')"
                    >{{ filtersFun.getVmStatus(scope.row.phaseStatus, 'status') }}</el-tag
                  >
                </template>
              </el-table-column>
              <el-table-column prop="imageInfo.name" :label="$t('hardware.hypervisorNodes.form.mount.system')" />
              <el-table-column prop="createTime" :label="$t('hardware.hypervisorNodes.form.mount.createTime')" />
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
              @size-change="pciHandleSizeChange"
              @current-change="pciHandleCurrentChange"
            />
          </el-col>
        </el-row>
      </div>
      <div class="dialog-footer text-center">
        <el-button type="text" @click="vmHandleClose()">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="toPciAttach()">{{
          $t('hardware.hypervisorNodes.form.mount.mount')
        }}</el-button>
      </div>
    </el-dialog>

    <el-drawer
      v-model="vmDrawer"
      :title="vmDrawerData.title"
      :direction="vmDrawerData.direction"
      :size="vmDrawerData.size"
      :before-close="vmDrawerHandleClose"
    >
      <component :is="vmCurrentView" :drawer-data="vmDrawerData" class="table w-full" @closeDrawer="vmCloseDrawer">
      </component>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import mainStore from '@/store/mainStore'; // pinia 信息
import mainApi from '@/api/modules/main';

import filtersFun from '@/utils/statusFun';
import vmadd from '../../compute/vm/add.vue';
import operate from '@/views/compute/vm/operate.vue';

const mainStoreData = mainStore();
const { proxy }: any = getCurrentInstance();

const { drawerData } = defineProps<{
  drawerData: any;
}>();

const router = useRouter();
const form: any = ref({});
const activeName = ref('first');
const nodeVmTableData = ref([]);
const nodeVmTableTotal = ref(0);
const pciDeviceTableData: any = ref([]);

const vmLoading = ref(false);
const dialogVm = ref(false);
const vmTableData: any = ref([]);
const nowCheckVm: any = ref('');
const nowCheckPci: any = ref('');
const vmForm = reactive({
  // 搜索 筛选
  name: '',
  page_num: 1,
  page_size: 10,
  total: 0,
});

const multipleTableRef: any = ref();

const goBack = () => {
  router.push('/hypervisorNodes');
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
    .vmsHypervisorNodesDetail(id)
    .then((res: any) => {
      form.value = res;
    })
    .catch((error: any) => {});
};
const getNodeVmList = () => {
  // 虚拟机列表
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  const data = {
    node_id: id,
  };
  mainApi
    .vmsInstabcesList(data)
    .then((res: any) => {
      nodeVmTableData.value = res.vmInstancesInfo;
      nodeVmTableTotal.value = res.totalNum;
    })
    .catch((error: any) => {});
};
const getPciDeviceList = () => {
  // GPU列表
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  const data = {
    node_id: id,
  };
  mainApi
    .pciDeviceList(data)
    .then((res: any) => {
      pciDeviceTableData.value = res;
    })
    .catch((error: any) => {});
};
const handleCheckChange = (val: any) => {
  // 选中
  nowCheckVm.value = val;
};
const openPciAttach = (item: any) => {
  // 挂载
  dialogVm.value = true;
  nowCheckPci.value = item;
};
const toPciAttach = () => {
  // 挂载
  mainApi
    .pciAttach(nowCheckPci.value.deviceId, { vmInstanceId: nowCheckVm.value.instanceId })
    .then((res: any) => {
      ElMessage.success(proxy.$t('hardware.hypervisorNodes.message.startMount'));
      getPciDeviceList(); // 请求GPU列表
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
const toPciDetach = (item: any) => {
  // 卸载
  mainApi
    .pciDetach(item.deviceId, { vmInstanceId: item.vmInstanceId })
    .then((res: any) => {
      ElMessage.success(proxy.$t('hardware.hypervisorNodes.message.startDetach'));
      getPciDeviceList(); // 请求GPU列表
    })
    .catch((error: any) => {});
  return true;
};
const pciHandleSizeChange = (val: any) => {
  // 改变每页显示数量
  vmForm.page_size = val;
  getVmsInstabcesList();
};
const pciHandleCurrentChange = (val: any) => {
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

const onReset = () => {
  // 重置查询
  form.name = '';
  form.page_num = 1;
  getVmsInstabcesList();
};

// 虚拟机创建 start
const vmDrawer = ref(false);
const vmDrawerData: any = ref('');
const vmCurrentView: any = ref(vmadd);

const addVm = () => {
  let gpuName = '';
  let availableGpuCount = 0;
  if (pciDeviceTableData.value && pciDeviceTableData.value.length > 0) {
    gpuName = pciDeviceTableData.value[0].pciDeviceName;
    availableGpuCount = pciDeviceTableData.value.filter((item: any) => item.phaseStatus == 87).length;
  }
  const data = {
    ...form.value,
    gpuName,
    availableGpuCount,
  };
  vmDrawerData.value = {
    title: proxy.$t('hardware.hypervisorNodes.page.addVirtualMachine'),
    closeText: proxy.$t('hardware.hypervisorNodes.page.closeAddVirtualMachine'),
    direction: 'rtl',
    size: '80%',
    close: true,
    isDrawer: true,
    link: `/vmAdd?nodeId=${form.value.nodeId}`,
    linkName: proxy.$t('hardware.hypervisorNodes.page.openAddVirtualMachine'),
    id: form.value.nodeId,
    item: data,
  };
  vmCurrentView.value = vmadd;
  vmDrawer.value = true;
};
const vmDrawerHandleClose = (done: () => void) => {
  if (vmDrawerData.value.close) {
    ElMessageBox.confirm(vmDrawerData.value.closeText)
      .then(() => {
        vmCurrentView.value = '';
        done();
      })
      .catch(() => {
        // catch error
      });
  } else {
    vmCurrentView.value = '';

    done();
  }
};
const vmCloseDrawer = () => {
  vmDrawer.value = false;
  vmCurrentView.value = '';
  vmDrawerData.value = '';
  getNodeVmList();
};
onMounted(() => {
  getDetail(); // 获取详情
  getNodeVmList(); // 请求节点下虚拟机列表
  getVmsInstabcesList(); // 请求虚拟机列表
  getPciDeviceList(); // 请求GPU列表
});
</script>

<style lang="scss" scoped>
.nodeDetailPage {
  .nodeDetailTabs {
    border-top-left-radius: 0.4rem;
    border-top-right-radius: 0.4rem;

    ::v-deep .el-tabs__header {
      padding: 0 0 0 1rem;
      margin-bottom: 0;
    }
  }
}
</style>

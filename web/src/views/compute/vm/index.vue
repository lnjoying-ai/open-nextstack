<template>
  <div class="vmPage h-full">
    <div class="py-3 px-4 mb-4 bg-white rounded-lg tableTop">
      <el-form :model="form" :inline="true" :size="mainStoreData.viewSize.main">
        <el-form-item :label="$t('compute.vm.form.vmName') + ':'">
          <el-input v-model="form.name" class="!w-50" :placeholder="$t('compute.vm.form.vmNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('compute.vm.form.eip') + ':'">
          <el-select
            v-model="form.eip_id"
            class="!w-50"
            :placeholder="$t('compute.vm.form.selectEip')"
            @change="onSubmit"
          >
            <el-option :label="$t('compute.vm.form.all')" value="" />
            <el-option v-for="(item, index) in eipTableData" :key="index" :label="item.ipAddress" :value="item.eipId" />
          </el-select>
        </el-form-item>
        <el-form-item class="float-right !mr-0">
          <el-button class="resetBtn w-24" @click="onReset"> {{ $t('common.reset') }} </el-button>
          <el-button type="primary" class="w-24" @click="onSubmit"> {{ $t('common.search') }} </el-button>
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
        <el-button type="primary" class="float-right" @click="addVm"> {{ $t('compute.vm.form.createVm') }} </el-button>
      </div>

      <el-table
        v-loading="vmLoading"
        :size="mainStoreData.viewSize.main"
        :element-loading-text="$t('common.loading')"
        :data="tableData"
        max-height="calc(100% - 3rem)"
        class="!overflow-y-auto"
        stripe
        :scrollbar-always-on="false"
      >
        <el-table-column prop="date" :label="$t('compute.vm.table.name')">
          <template #default="scope">
            <span class="text-blue-400 cursor-pointer" @click="toDetail(scope.row)">
              {{ scope.row.name }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="hostname" :label="$t('compute.vm.table.hostname')">
          <template #default="scope">
            <span>{{ scope.row.hostname || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="portInfo.ipAddress" :label="$t('compute.vm.table.ip')">
          <template #default="scope">
            <span>{{ scope.row.portInfo.ipAddress || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="vpcInfo.cidr" :label="$t('compute.vm.table.networkAddress')">
          <template #default="scope">
            <span>{{ scope.row.subnetInfo.cidr || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="eip" :label="$t('compute.vm.table.eip')">
          <template #default="scope">
            <span v-if="!scope.row.boundPhaseStatus">{{ '-' }}</span>
            <span v-if="scope.row.boundType && scope.row.boundType === 'port'">
              <span v-if="scope.row.boundPhaseStatus == 82">
                {{ scope.row.eip || '-' }}
              </span>
              <el-tag
                v-else
                :size="mainStoreData.viewSize.tagStatus"
                :type="filtersFun.getVmToEipStatus(scope.row.boundPhaseStatus, 'tag')"
              >
                {{ filtersFun.getVmToEipStatus(scope.row.boundPhaseStatus, 'status') }}
              </el-tag>
            </span>
            <span v-if="scope.row.boundType && scope.row.boundType === 'nat'">
              <span v-if="scope.row.boundPhaseStatus == 7">
                <span v-if="scope.row.eip">
                  <el-tag :size="mainStoreData.viewSize.tagStatus">Nat</el-tag>
                  {{ scope.row.eip }}
                </span>
                <span v-else>-</span>
              </span>
              <el-tag
                v-else
                :size="mainStoreData.viewSize.tagStatus"
                :type="filtersFun.getNatStatus(scope.row.boundPhaseStatus, 'tag')"
              >
                {{ filtersFun.getNatStatus(scope.row.boundPhaseStatus, 'status') }}
              </el-tag>
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="phaseStatus" :label="$t('compute.vm.table.status')">
          <template #default="scope">
            <el-tag
              :size="mainStoreData.viewSize.tagStatus"
              :type="filtersFun.getVmStatus(scope.row.phaseStatus, 'tag')"
            >
              {{ filtersFun.getVmStatus(scope.row.phaseStatus, 'status') }}
            </el-tag>
            <span v-if="scope.row.phaseStatus == 66">
              <el-tooltip
                class="box-item"
                effect="dark"
                :content="$t('compute.vm.message.insufficientComputeResources')"
                placement="top"
              >
                <span class="inline-block ml-1">
                  <i-bi:info-square class="inline-block" />
                </span>
              </el-tooltip>
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="imageInfo.name" :label="$t('compute.vm.table.system')">
          <template #default="scope">
            <span v-if="scope.row.imageInfo">{{ scope.row.imageInfo.name || '-' }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" :label="$t('common.createTime')" />

        <el-table-column :label="$t('compute.vm.table.action')" width="120">
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

    <el-drawer
      v-model="drawer"
      :title="drawerData.title"
      :direction="drawerData.direction"
      :size="drawerData.size"
      :before-close="handleClose"
    >
      <component :is="currentView" :drawer-data="drawerData" class="table w-full" @closeDrawer="closeDrawer" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import mainApi from '@/api/modules/main';
import filtersFun from '@/utils/statusFun';
import mainStore from '@/store/mainStore';
import vmadd from './add.vue';
import vmdetail from './detail.vue';
import operate from './operate.vue';

const { proxy }: any = getCurrentInstance();

const mainStoreData = mainStore();
const timer: any = ref('');
const router = useRouter();

const vmLoading = ref(false);

const drawer = ref(false);
const drawerData: any = ref('');
const currentView: any = ref(vmdetail);

const handleClose = (done: () => void) => {
  if (drawerData.value.close) {
    ElMessageBox.confirm(drawerData.value.closeText)
      .then(() => {
        currentView.value = '';
        done();
      })
      .catch(() => {});
  } else {
    currentView.value = '';
    done();
  }
};

const form: any = reactive({
  name: '',
  eip_id: '',
  page_num: 1,
  page_size: mainStoreData.page_size,
  total: 0,
});

const tableData: any = ref([]);
const eipTableData: any = ref([]);

const handleSizeChange = (val: any) => {
  localStorage.setItem('page_size', val);
  mainStoreData.page_size = val;
  form.page_size = val;
  getVmsInstabcesList();
};

const handleCurrentChange = (val: any) => {
  form.page_num = val;
  getVmsInstabcesList();
};

const onSubmit = () => {
  form.page_num = 1;
  getVmsInstabcesList();
};

const onReset = () => {
  form.name = '';
  form.eip_id = '';
  form.page_num = 1;
  getVmsInstabcesList();
};

const getVmsInstabcesList = () => {
  vmLoading.value = true;

  const params: any = {
    name: form.name,
    eip_id: form.eip_id,
    page_num: form.page_num,
    page_size: form.page_size,
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
      tableData.value = res.vmInstancesInfo;
      form.total = res.totalNum;
    })
    .catch((error: any) => {
      vmLoading.value = false;
    });
};

const getVmsInstabcesListTime = () => {
  const params: any = {
    name: form.name,
    eip_id: form.eip_id,
    page_num: form.page_num,
    page_size: form.page_size,
  };

  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }

  mainApi.vmsInstabcesList(params).then((res: any) => {
    tableData.value = res.vmInstancesInfo;
    form.total = res.totalNum;
  });
};

const closeDrawer = () => {
  drawer.value = false;
  currentView.value = '';
  drawerData.value = '';
  getVmsInstabcesList();
};

const addVm = () => {
  drawerData.value = {
    title: proxy.$t('compute.vm.page.addVm'),
    closeText: proxy.$t('compute.vm.page.closeAddVm'),
    direction: 'rtl',
    size: '80%',
    close: true,
    isDrawer: true,
    link: '/vmAdd',
    linkName: proxy.$t('compute.vm.page.openAddVm'),
    id: '',
  };
  currentView.value = vmadd;
  drawer.value = true;
};

const toDetail = (item: any) => {
  drawerData.value = {
    title: proxy.$t('compute.vm.page.vmDetail'),
    closeText: proxy.$t('compute.vm.page.closeDetail'),
    direction: 'rtl',
    size: '1200px',
    close: false,
    isDrawer: true,
    link: `/vm/${item.instanceId}`,
    linkName: proxy.$t('compute.vm.page.openDetail'),
    id: item.instanceId,
  };
  currentView.value = vmdetail;
  drawer.value = true;
};

const getEipList = () => {
  const params: any = {
    page_num: 1,
    page_size: 99999,
  };

  mainApi.eipsList(params).then((res: any) => {
    eipTableData.value = res.eips;
  });
};

onMounted(() => {
  getEipList();
  if (router.currentRoute.value.params && router.currentRoute.value.params.eipId) {
    form.eip_id = router.currentRoute.value.params.eipId;
  }
  getVmsInstabcesList();
  timer.value = setInterval(async () => {
    getVmsInstabcesListTime();
  }, mainStoreData.listRefreshTime);
});

onUnmounted(() => {
  clearInterval(timer.value);
});
</script>

<style lang="scss" scoped>
.vmPage {
  ::v-deep .el-table {
    .success-row {
      background-color: rgba(149, 212, 117, 0.3);
    }
  }
}
</style>

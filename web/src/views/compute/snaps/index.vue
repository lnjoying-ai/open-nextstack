<template>
  <div class="snapsPage h-full">
    <!-- <h5 class="mb-3 px-5 pt-2">
      <span class="text-lg">{{ $route.meta.title }}</span>
      <el-divider class="!my-2"></el-divider>
    </h5> -->
    <div class="py-3 px-4 mb-4 bg-white rounded-lg tableTop">
      <el-form :model="form" :inline="true" :size="mainStoreData.viewSize.main">
        <el-form-item :label="$t('compute.snaps.name') + ':'">
          <el-input v-model="form.name" class="!w-50" :placeholder="$t('compute.snaps.validation.nameRequired')" />
        </el-form-item>
        <el-form-item :label="$t('compute.snaps.form.vm') + ':'">
          <el-select
            v-model="vmId"
            class="!w-50"
            :placeholder="$t('compute.snaps.form.searchVm')"
            :remote-method="remoteMethod"
            :loading="vmLoading"
            filterable
            remote
            reserve-keyword
            @change="onVmChange"
          >
            <el-option v-for="(item, index) in vmListData" :key="index" :label="item.name" :value="item.instanceId">{{
              item.name
            }}</el-option>
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
        <div
          class="table-total float-left px-3 py-0.8 text-sm text-gray-400 border rounded-sm border-gray-200 border-solid"
        >
          <i18n-t keypath="common.totalNum" tag="span">
            <template #count>
              <span class="text-blue-400 font-bold">{{ form.total || 0 }}</span>
            </template>
          </i18n-t>
        </div>

        <el-button type="primary" class="float-right" @click="addSnaps">{{ $t('compute.snaps.create') }}</el-button>
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
        <el-table-column prop="date" :label="$t('common.name')">
          <template #default="scope">
            <span class="text-blue-400 cursor-pointer" @click="toDetail(scope.row)">{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="date" :label="$t('compute.snaps.form.vm')">
          <template #default="scope">
            <router-link :to="'/vm/' + scope.row.vmInstanceId" class="text-blue-400">{{
              scope.row.vmInstanceName || '-'
            }}</router-link>
          </template>
        </el-table-column>
        <el-table-column prop="current" :label="$t('compute.snaps.form.current')">
          <template #default="scope">
            <span>
              <el-tag :size="mainStoreData.viewSize.tagStatus" :type="scope.row.current ? 'success' : 'danger'">{{
                scope.row.current ? $t('common.yes') : $t('common.no')
              }}</el-tag>
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
        <el-table-column prop="createTime" :label="$t('common.createTime')" />
        <el-table-column :label="$t('common.operation')" width="120">
          <template #default="scope">
            <el-dropdown trigger="click" :size="mainStoreData.viewSize.listSet">
              <el-button type="text" :size="mainStoreData.viewSize.listSet">
                {{ $t('common.operation') }}<i-ic:baseline-keyboard-arrow-down></i-ic:baseline-keyboard-arrow-down>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item>
                    <span class="w-full" @click="toEdit(scope.row)"
                      ><img src="@/assets/img/btn/edit.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                        $t('common.edit')
                      }}</span
                    ></el-dropdown-item
                  >
                  <el-popconfirm
                    :confirm-button-text="$t('common.delete')"
                    :cancel-button-text="$t('common.cancel')"
                    icon-color="#626AEF"
                    :title="$t('compute.snaps.confirm.delete')"
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
                  <el-popconfirm
                    :confirm-button-text="$t('common.confirm')"
                    :cancel-button-text="$t('common.cancel')"
                    icon-color="#626AEF"
                    :title="$t('compute.snaps.confirm.restore')"
                    @confirm="toRestore(scope.row)"
                  >
                    <template #reference>
                      <span class="listDelBtn"
                        ><img src="@/assets/img/btn/rollBack.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                          $t('common.restore')
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

import snapsadd from './add.vue';
import snapsdetail from './detail.vue';
import snapsedit from './edit.vue';

const { proxy }: any = getCurrentInstance();
const drawer = ref(false);
const drawerData: any = ref('');
const currentView: any = ref(snapsdetail);
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

const mainStoreData = mainStore(); // pinia 信息
const router = useRouter();
const timer: any = ref('');
const vmListData: any = ref([]);
const vmId: any = ref('');

const loading = ref(false);
const vmLoading = ref(false);
const form = reactive({
  // 搜索 筛选
  name: '',
  page_num: 1,
  page_size: mainStoreData.page_size,
  total: 0,
});

const tableData: any = ref([]);
const closeDrawer = () => {
  drawer.value = false;
  currentView.value = '';
  drawerData.value = '';
  getsnapsList();
};
const addSnaps = () => {
  drawerData.value = {
    title: proxy.$t('compute.snaps.create'),
    closeText: proxy.$t('compute.snaps.confirm.cancelAdd'),
    direction: 'rtl',
    size: '80%',
    close: true,
    isDrawer: true,
    link: '/snapsAdd',
    linkName: proxy.$t('compute.snaps.page.openCreate'),
    id: '',
  };
  currentView.value = snapsadd;
  drawer.value = true;
};
const toEdit = (item: any) => {
  // 编辑
  drawerData.value = {
    title: proxy.$t('compute.snaps.edit'),
    closeText: proxy.$t('compute.snaps.confirm.cancelEdit'),
    direction: 'rtl',
    size: '80%',
    close: true,
    isDrawer: true,
    link: `/snapsEdit/${item.snapId}`,
    linkName: proxy.$t('compute.snaps.page.openEdit'),
    id: item.snapId,
  };
  currentView.value = snapsedit;
  drawer.value = true;
};
const toDetail = (item: any) => {
  // 详情
  drawerData.value = {
    title: proxy.$t('compute.snaps.detail'),
    closeText: proxy.$t('compute.snaps.confirm.closeDetail'),
    direction: 'rtl',
    size: '80%',
    close: false,
    isDrawer: true,
    link: `/snaps/${item.snapId}`,
    linkName: proxy.$t('compute.snaps.page.openDetail'),
    id: item.snapId,
  };
  currentView.value = snapsdetail;

  drawer.value = true;
};

const handleSizeChange = (val: any) => {
  // 改变每页显示数量
  console.log(`${val} items per page`);
  localStorage.setItem('page_size', val);
  mainStoreData.page_size = val;
  form.page_size = val;
  getsnapsList();
};
const handleCurrentChange = (val: any) => {
  // 改变页码
  console.log(`current page: ${val}`);
  form.page_num = val;
  getsnapsList();
};
const toRestore = (row: any) => {
  // 回滚
  mainApi.snapsSwitch(row.snapId).then((res: any) => {
    ElMessage.success(proxy.$t('compute.snaps.message.startRollback'));
    getsnapsList();
  });
  return true;
};
const toDelete = (item: any) => {
  // 删除
  mainApi
    .snapsDel(item.snapId)
    .then((res: any) => {
      ElMessage.success(proxy.$t('compute.snaps.message.startDelete'));
      getsnapsList();
    })
    .catch((error: any) => {
      loading.value = false;
    });
  return true;
};
const onSubmit = () => {
  // 提交查询
  form.page_num = 1;
  getsnapsList();
};
const onReset = () => {
  // 重置查询
  form.name = '';
  form.page_num = 1;
  vmId.value = ''; // 虚拟机id
  vmListData.value = []; // 虚拟机列表
  getsnapsList();
};
const getsnapsList = () => {
  // 快照列表
  loading.value = true;

  const params: any = {
    name: form.name,
    instance_id: vmId.value,
    page_num: form.page_num,
    page_size: form.page_size,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }

  mainApi
    .snapsList(params)
    .then((res: any) => {
      loading.value = false;
      tableData.value = res.snaps;
      form.total = res.totalNum;
    })
    .catch((error: any) => {
      loading.value = false;
    });
};

const getsnapsListTime = () => {
  // 快照循环列表

  const params: any = {
    name: form.name,
    instance_id: vmId.value,
    page_num: form.page_num,
    page_size: form.page_size,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }

  mainApi.snapsList(params).then((res: any) => {
    tableData.value = res.snaps;
    form.total = res.totalNum;
  });
};
const onVmChange = (val: any) => {
  // 虚拟机列表
  vmId.value = val;
  getsnapsList();
};
const remoteMethod = (query: any) => {
  // 虚拟机搜索
  if (query) {
    getVmsInstabcesList(query);
  } else {
    vmListData.value = [];
  }
};
const getVmsInstabcesList = (query: any) => {
  // 虚拟机列表
  vmLoading.value = true;
  const params: any = {
    name: query,
    page_num: 1,
    page_size: 10,
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
      vmListData.value = res.vmInstancesInfo;
    })
    .catch((error: any) => {
      vmLoading.value = false;
    });
};
onMounted(() => {
  getsnapsList(); // 请求快照列表
  timer.value = setInterval(async () => {
    getsnapsListTime(); // 请求快照循环列表
  }, mainStoreData.listRefreshTime);
});
onUnmounted(() => {
  clearInterval(timer.value);
});
</script>

<style lang="scss" scoped>
.snapsPage {
}
</style>

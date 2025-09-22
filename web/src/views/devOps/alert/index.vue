<template>
  <div class="subNetPage h-full">
    <!-- <h5 class="mb-3 px-5 pt-2">
      <span class="text-lg">{{ $route.meta.title }}</span>
      <el-divider class="!my-2"></el-divider>
    </h5> -->
    <div class="py-3 px-4 mb-4 bg-white rounded-lg tableTop">
      <el-form :model="form" :inline="true" label-width="100px" :size="mainStoreData.viewSize.main">
        <el-form-item label="">
          <el-page-header :title="$t('devOps.alert.backToList')" @back="goBack"> </el-page-header>
        </el-form-item>
        <el-form-item label="">
          <el-input v-model="form.name" class="!w-50" :placeholder="$t('devOps.alert.alertName')" />
        </el-form-item>
        <el-form-item label="">
          <el-select
            v-model="form.resource_type"
            class="!w-50"
            :placeholder="$t('devOps.alert.selectResourceType')"
            @change="onSubmit"
          >
            <el-option :label="$t('devOps.alert.all')" :value="''" />
            <el-option :label="$t('devOps.alert.virtualMachineInstance')" :value="0" />
            <el-option :label="$t('devOps.alert.virtualMachineInstanceGroup')" :value="1" />
            <el-option :label="$t('devOps.alert.computeNode')" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item class="float-right !mr-0">
          <el-button class="resetBtn w-24" @click="onReset">{{ $t('common.reset') }}</el-button>
          <el-button type="primary" class="w-24" @click="onSubmit">{{ $t('common.search') }}</el-button>
        </el-form-item>
      </el-form>
    </div>
    <el-tabs v-model="activeName" class="demo-tabs bg-white devOpsTabs" @tab-click="devOpsTabsHandleClick">
      <el-tab-pane :label="$t('devOps.alert.alert')" name="first"></el-tab-pane>
      <el-tab-pane :label="$t('devOps.alert.noticeGrop')" name="second"></el-tab-pane>
    </el-tabs>
    <div class="py-3 px-4 bg-white rounded-lg h-table-2">
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

        <el-button type="primary" class="float-right" @click="addEipPool">{{
          $t('devOps.alert.createAlert')
        }}</el-button>
      </div>

      <el-table
        v-loading="loading"
        :element-loading-text="$t('common.loading')"
        :size="mainStoreData.viewSize.main"
        :data="tableData"
        max-height="calc(100vh - 290px - 0.75rem)"
        class="!overflow-y-auto"
        stripe
        :scrollbar-always-on="false"
      >
        <el-table-column prop="name" :label="$t('devOps.alert.alertName')">
          <template #default="scope">
            <span class="text-blue-400 cursor-pointer" @click="toDetail(scope.row)">{{ scope.row.name }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="expr" :label="$t('devOps.alert.alarmElement')">
          <template #default="scope">
            <span v-if="scope.row.alarmElement == 0">{{ $t('devOps.alert.cpuUsage') }}</span>
            <span v-if="scope.row.alarmElement == 1">{{ $t('devOps.alert.memoryUsage') }}</span>
            <span v-if="scope.row.alarmElement == 3">{{ $t('devOps.alert.filesystemUsage') }}</span>
            <span v-if="scope.row.alarmElement == 4">{{ $t('devOps.alert.networkThroughput') }}</span>
            <span v-if="scope.row.alarmElement == 5">{{ $t('devOps.alert.diskIops') }}</span>
            <span v-if="scope.row.alarmElement == 6">{{ $t('devOps.alert.diskThroughput') }}</span>
            <span v-if="scope.row.alarmElement == 7">{{ $t('devOps.alert.instanceOnline') }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="expr" :label="$t('devOps.alert.triggerRule')">
          <template #default="scope">
            {{ scope.row.expr }}
          </template>
        </el-table-column>
        <el-table-column prop="level" :label="$t('devOps.alert.alertLevel')">
          <template #default="scope">
            <el-tag v-if="scope.row.level == 0" class="ml-2" type="info">{{ $t('devOps.alert.warning') }}</el-tag>
            <el-tag v-if="scope.row.level == 1" class="ml-2" type="warning">{{ $t('devOps.alert.serious') }}</el-tag>
            <el-tag v-if="scope.row.level == 2" class="ml-2" type="danger">{{ $t('devOps.alert.critical') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resourceType" :label="$t('devOps.alert.resourceType')">
          <template #default="scope">
            {{
              scope.row.resourceType == 0
                ? $t('devOps.alert.virtualMachineInstance')
                : scope.row.resourceType == 1
                ? $t('devOps.alert.virtualMachineInstanceGroup')
                : scope.row.resourceType == 2
                ? $t('devOps.alert.computeNode')
                : '-'
            }}
          </template>
        </el-table-column>

        <el-table-column prop="interval" width="120" :label="$t('devOps.alert.alertInterval')">
          <template #default="scope">
            <span>{{
              scope.row.interval > 59
                ? scope.row.interval / 60 + $t('devOps.alert.hour')
                : scope.row.interval + $t('devOps.alert.minute')
            }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="contactInfo" width="100" :label="$t('devOps.alert.contactInfo')">
          <template #default="scope">
            {{ scope.row.contactInfoList ? scope.row.contactInfoList.length : 0 }}
          </template>
        </el-table-column>

        <el-table-column prop="createTime" :label="$t('devOps.alert.createTime')" />
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
                    :title="$t('devOps.alert.deleteAlert')"
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

import alertadd from './add.vue';
import alertedit from './edit.vue';
import alertdetail from './detail.vue';

const { proxy }: any = getCurrentInstance();
const mainStoreData = mainStore(); // pinia 信息
const router = useRouter();

const timer: any = ref('');

const loading = ref(false);
const activeName = ref('first');
const devOpsTabsHandleClick = (tab: any) => {
  if (activeName.value == 'first') {
    router.push('/devOps/alert');
  } else if (activeName.value == 'second') {
    router.push('/devOps/noticeGrop');
  }
};
const drawer = ref(false);
const drawerData: any = ref('');
const currentView: any = ref(alertdetail);
const closeDrawer = () => {
  drawer.value = false;
  currentView.value = '';
  drawerData.value = '';
  getOperationAlarmRules();
};
const addEipPool = () => {
  drawerData.value = {
    title: proxy.$t('devOps.alert.page.createAlert'),
    closeText: proxy.$t('devOps.alert.page.cancelCreateAlert'),
    direction: 'rtl',
    size: '80%',
    close: true,
    isDrawer: true,
    link: '/devOps/alertAdd',
    linkName: proxy.$t('devOps.alert.page.openCreateAlert'),
    id: '',
  };
  currentView.value = alertadd;
  drawer.value = true;
};
const toEdit = (item: any) => {
  // 编辑
  drawerData.value = {
    title: proxy.$t('devOps.alert.page.editAlert'),
    closeText: proxy.$t('devOps.alert.page.cancelEditAlert'),
    direction: 'rtl',
    size: '80%',
    close: true,
    isDrawer: true,
    link: `/devOps/alertEdit/${item.ruleId}`,
    linkName: proxy.$t('devOps.alert.page.openEditAlert'),
    id: item.ruleId,
  };
  currentView.value = alertedit;
  drawer.value = true;
};
const toDetail = (item: any) => {
  // 详情
  drawerData.value = {
    title: proxy.$t('devOps.alert.page.detailAlert'),
    closeText: proxy.$t('devOps.alert.page.cancelDetailAlert'),
    direction: 'rtl',
    size: '80%',
    close: false,
    isDrawer: true,
    link: `/devOps/alert/${item.ruleId}`,
    linkName: proxy.$t('devOps.alert.page.openDetailAlert'),
    id: item.ruleId,
  };
  currentView.value = alertdetail;

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
const goBack = () => {
  router.push('/devOps/alertHome');
};
const form = reactive({
  // 搜索 筛选
  name: '',
  resource_type: '',
  page_num: 1,
  page_size: mainStoreData.page_size,
  total: 0,
});
const tableData: any = ref([]);
const handleSizeChange = (val: any) => {
  // 改变每页显示数量
  console.log(`${val} items per page`);
  localStorage.setItem('page_size', val);
  mainStoreData.page_size = val;
  form.page_size = val;
  getOperationAlarmRules();
};
const handleCurrentChange = (val: any) => {
  // 改变页码
  console.log(`current page: ${val}`);
  form.page_num = val;
  getOperationAlarmRules();
};

const toDelete = (item: any) => {
  // 删除
  mainApi
    .operationAlarmRulesDel(item.ruleId)
    .then((res: any) => {
      loading.value = false;
      ElMessage.success(proxy.$t('common.operations.success.delete'));
      getOperationAlarmRules();
    })
    .catch((error: any) => {
      loading.value = false;
    });
  return true;
};
const onSubmit = () => {
  // 提交查询
  form.page_num = 1;
  getOperationAlarmRules();
};
const onReset = () => {
  // 重置查询
  form.name = '';
  form.page_num = 1;
  getOperationAlarmRules();
};
const getOperationAlarmRules = () => {
  // 报警器列表
  loading.value = true;
  const params: any = {
    name: form.name,
    resource_type: form.resource_type,
    page_num: form.page_num,
    page_size: form.page_size,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }
  mainApi
    .operationAlarmRules(params)
    .then((res: any) => {
      loading.value = false;
      tableData.value = res.alarmRules;
      form.total = res.totalNum;
    })
    .catch((error: any) => {
      loading.value = false;
    });
};
const getOperationAlarmRulesTime = () => {
  // 报警器列表
  const params: any = {
    name: form.name,
    resource_type: form.resource_type,
    page_num: form.page_num,
    page_size: form.page_size,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }
  mainApi.operationAlarmRules(params).then((res: any) => {
    tableData.value = res.alarmRules;
    form.total = res.totalNum;
  });
};

onMounted(() => {
  getOperationAlarmRules(); // 报警器列表
  timer.value = setInterval(async () => {
    getOperationAlarmRulesTime(); // 请求报警器循环列表
  }, mainStoreData.listRefreshTime);
});
onUnmounted(() => {
  clearInterval(timer.value);
});
</script>

<style lang="scss" scoped>
.subNetPage {
}
.devOpsTabs {
  border-top-left-radius: 0.4rem;
  border-top-right-radius: 0.4rem;
  ::v-deep .el-tabs__header {
    padding: 0 0 0 1rem;
    margin-bottom: 0;
  }
}
</style>

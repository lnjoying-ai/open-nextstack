<template>
  <div class="subNetPage h-full">
    <div class="py-3 px-4 mb-4 bg-white rounded-lg tableTop">
      <el-form :model="form" :inline="true" :size="mainStoreData.viewSize.main">
        <el-form-item label="">
          <el-page-header :title="$t('devOps.noticeGrop.backToList')" @back="goBack"> </el-page-header>
        </el-form-item>
        <el-form-item :label="$t('devOps.noticeGrop.form.name') + ':'">
          <el-input v-model="form.name" class="!w-50" :placeholder="$t('devOps.noticeGrop.form.inputName')" />
        </el-form-item>

        <el-form-item class="float-right !mr-0">
          <el-button class="resetBtn w-24" @click="onReset">{{ $t('common.reset') }}</el-button>
          <el-button type="primary" class="w-24" @click="onSubmit">{{ $t('common.search') }}</el-button>
        </el-form-item>
      </el-form>
    </div>
    <el-tabs v-model="activeName" class="demo-tabs bg-white devOpsTabs" @tab-click="devOpsTabsHandleClick">
      <el-tab-pane :label="$t('devOps.noticeGrop.alert')" name="first"></el-tab-pane>
      <el-tab-pane :label="$t('devOps.noticeGrop.notice')" name="second"></el-tab-pane>
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
          $t('devOps.noticeGrop.addNotice')
        }}</el-button>
      </div>

      <el-table
        v-loading="loading"
        :size="mainStoreData.viewSize.main"
        :element-loading-text="$t('common.loading')"
        :data="tableData"
        max-height="calc(100vh - 290px - 0.75rem)"
        class="!overflow-y-auto"
        stripe
        :scrollbar-always-on="false"
      >
        <el-table-column prop="name" :label="$t('devOps.noticeGrop.form.name')">
          <template #default="scope">
            <span class="text-blue-400 cursor-pointer" @click="toDetail(scope.row)">{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" :label="$t('devOps.noticeGrop.form.description')">
          <template #default="scope">
            {{ scope.row.description }}
          </template>
        </el-table-column>
        <el-table-column prop="type" :label="$t('devOps.noticeGrop.form.type')">
          <template #default="scope">
            <span v-if="scope.row.type == 0">{{ $t('devOps.noticeGrop.form.emailNotice') }}</span>
            <span v-if="scope.row.type == 1">{{ $t('devOps.noticeGrop.form.smsNotice') }}</span>
            <span v-if="scope.row.type == 2">{{ $t('devOps.noticeGrop.form.phoneNotice') }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="contactCount" :label="$t('devOps.noticeGrop.form.contactCount')">
          <template #default="scope">
            {{ scope.row.contactCount }}
          </template>
        </el-table-column>

        <el-table-column prop="createTime" :label="$t('devOps.noticeGrop.form.createTime')" />
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
                    :title="$t('devOps.noticeGrop.form.deleteConfirm')"
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
      <component :is="currentView" :drawer-data="drawerData" class="table w-full" @closeDrawer="closeDrawer">
      </component>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import mainApi from '@/api/modules/main';
import filtersFun from '@/utils/statusFun';
import mainStore from '@/store/mainStore';

import operationReceiversadd from './add.vue';
import operationReceiversedit from './edit.vue';
import operationReceiversdetail from './detail.vue';

const { proxy }: any = getCurrentInstance();
const mainStoreData = mainStore(); // pinia 信息
const router = useRouter();

const timer: any = ref('');

const loading = ref(false);
const activeName = ref('second');
const devOpsTabsHandleClick = (tab: any) => {
  if (activeName.value == 'first') {
    router.push('/devOps/alert');
  } else if (activeName.value == 'second') {
    router.push('/devOps/noticeGrop');
  }
};
const drawer = ref(false);
const drawerData: any = ref('');
const currentView: any = ref(operationReceiversdetail);
const closeDrawer = () => {
  drawer.value = false;
  currentView.value = '';
  drawerData.value = '';
  getOperationReceivers();
};
const addEipPool = () => {
  drawerData.value = {
    title: proxy.$t('devOps.noticeGrop.page.createNotice'),
    closeText: proxy.$t('devOps.noticeGrop.page.cancelCreateNotice'),
    direction: 'rtl',
    size: '80%',
    close: true,
    isDrawer: true,
    link: '/operationReceiversAdd',
    linkName: proxy.$t('devOps.noticeGrop.page.openCreateNotice'),
    id: '',
  };
  currentView.value = operationReceiversadd;
  drawer.value = true;
};
const toEdit = (item: any) => {
  // 编辑
  drawerData.value = {
    title: proxy.$t('devOps.noticeGrop.page.editNotice'),
    closeText: proxy.$t('devOps.noticeGrop.page.cancelEditNotice'),
    direction: 'rtl',
    size: '80%',
    close: true,
    isDrawer: true,
    link: `/operationReceiversEdit/${item.receiverId}`,
    linkName: proxy.$t('devOps.noticeGrop.page.openEditNotice'),
    id: item.receiverId,
  };
  currentView.value = operationReceiversedit;
  drawer.value = true;
};
const toDetail = (item: any) => {
  // 详情
  drawerData.value = {
    title: proxy.$t('devOps.noticeGrop.page.detailNotice'),
    closeText: proxy.$t('devOps.noticeGrop.page.cancelDetailNotice'),
    direction: 'rtl',
    size: '80%',
    close: false,
    isDrawer: true,
    link: `/operationReceivers/${item.receiverId}`,
    linkName: proxy.$t('devOps.noticeGrop.page.openDetailNotice'),
    id: item.receiverId,
  };
  currentView.value = operationReceiversdetail;

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
  getOperationReceivers();
};
const handleCurrentChange = (val: any) => {
  // 改变页码
  console.log(`current page: ${val}`);
  form.page_num = val;
  getOperationReceivers();
};

const toDelete = (item: any) => {
  // 删除
  mainApi
    .operationReceiversDel(item.receiverId)
    .then((res: any) => {
      loading.value = false;
      ElMessage.success(proxy.$t('common.operations.success.delete'));
      getOperationReceivers();
    })
    .catch((error: any) => {
      loading.value = false;
    });
  return true;
};
const onSubmit = () => {
  // 提交查询
  form.page_num = 1;
  getOperationReceivers();
};
const onReset = () => {
  // 重置查询
  form.name = '';
  form.page_num = 1;
  getOperationReceivers();
};
const getOperationReceivers = () => {
  // 通知对象列表
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
    .operationReceivers(params)
    .then((res: any) => {
      loading.value = false;
      tableData.value = res.alarmRules;
      form.total = res.totalNum;
    })
    .catch((error: any) => {
      loading.value = false;
    });
};
const getOperationReceiversTime = () => {
  // 通知对象列表
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
  mainApi.operationReceivers(params).then((res: any) => {
    tableData.value = res.alarmRules;
    form.total = res.totalNum;
  });
};
onMounted(() => {
  getOperationReceivers(); // 通知对象列表
  timer.value = setInterval(async () => {
    getOperationReceiversTime(); // 请求通知对象循环列表
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

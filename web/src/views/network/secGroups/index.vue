<template>
  <div class="sgsPage h-full">
    <!-- <h5 class="mb-3 px-5 pt-2">
      <span class="text-lg">{{ $route.meta.title }}</span>
      <el-divider class="!my-2"></el-divider>
    </h5> -->
    <div class="py-3 px-4 mb-4 bg-white rounded-lg tableTop">
      <el-form :model="form" :inline="true" :size="mainStoreData.viewSize.main">
        <el-form-item :label="$t('network.secGroups.form.name') + ':'">
          <el-input
            v-model="form.name"
            class="!w-50"
            :placeholder="$t('network.secGroups.form.inputSecurityGroupName')"
          />
        </el-form-item>

        <el-form-item class="float-right !mr-0">
          <el-button class="resetBtn w-24" @click="onReset">{{ $t('common.reset') }}</el-button>
          <el-button type="primary" class="resetBtn w-24" @click="onSubmit">{{ $t('common.search') }}</el-button>
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

        <el-button type="primary" class="float-right" @click="addSecGroups">{{
          $t('network.secGroups.form.createSecurityGroup')
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
        <el-table-column prop="name" :label="$t('network.secGroups.form.name')">
          <template #default="scope">
            <span class="text-blue-400 cursor-pointer" @click="toDetail(scope.row, 'info')">{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" :label="$t('network.secGroups.form.securityGroupRules')">
          <template #default="scope">
            <!-- <router-link :to="'/secGroups/' + scope.row.sgId + '?type=ingress'"
                         class="text-blue-400">{{
              scope.row.ruleCount
            }}</router-link> -->

            <span class="text-blue-400 cursor-pointer" @click="toDetail(scope.row, 'ingress')">{{
              scope.row.ruleCount
            }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" :label="$t('network.secGroups.instances')">
          <template #default="scope">
            <!-- <router-link :to="'/secGroups/' + scope.row.sgId + '?type=instances'"
                         class="text-blue-400">{{
              scope.row.computeInstanceCount
            }}</router-link> -->

            <span class="text-blue-400 cursor-pointer" @click="toDetail(scope.row, 'instances')">{{
              scope.row.computeInstanceCount
            }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="phaseStatus" :label="$t('network.secGroups.form.status')">
          <template #default="scope">
            <el-tag
              :size="mainStoreData.viewSize.tagStatus"
              :type="filtersFun.getSecGroupsStatus(scope.row.phaseStatus, 'tag')"
              >{{ filtersFun.getSecGroupsStatus(scope.row.phaseStatus, 'status') }}</el-tag
            >
          </template>
        </el-table-column>
        <el-table-column prop="description" :label="$t('network.secGroups.form.description')" />

        <el-table-column :label="$t('common.operation')" width="120">
          <template #default="scope">
            <el-dropdown v-if="scope.row.name != 'default'" trigger="click" :size="mainStoreData.viewSize.listSet">
              <el-button type="text" :size="mainStoreData.viewSize.listSet">
                {{ $t('common.operation') }}<i-ic:baseline-keyboard-arrow-down></i-ic:baseline-keyboard-arrow-down>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="toEdit(scope.row)"
                    ><img src="@/assets/img/btn/edit.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                      $t('common.edit')
                    }}</el-dropdown-item
                  >
                  <el-popconfirm
                    :confirm-button-text="$t('common.delete')"
                    :cancel-button-text="$t('common.cancel')"
                    icon-color="#626AEF"
                    :title="$t('network.secGroups.message.confirmDeleteSecurityGroup')"
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
    <add
      class="w-0 h-0"
      :add-status="addSecGroupsStatus"
      :add-type="addType"
      :add-form="addForm"
      :sg-id="sgId"
      @closeAdd="onCloseAdd"
      @getSgsList="getSgsList"
    ></add>
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
import add from './add.vue';
import filtersFun from '@/utils/statusFun';
import mainStore from '@/store/mainStore';

import secGroupsdetail from './detail.vue';

const { proxy }: any = getCurrentInstance();
const drawer = ref(false);
const drawerData: any = ref('');
const currentView: any = ref(secGroupsdetail);
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
const timer: any = ref('');

const loading = ref(false);
const addType = ref('add'); // 添加还是编辑 add-edit
const addForm = reactive({
  // 编辑安全组
  name: '',
  description: '',
});
const sgId = ref('');
const addSecGroupsStatus = ref(false);
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
  getSgsList();
};
const toDetail = (item: any, type: string) => {
  // 详情
  drawerData.value = {
    title: proxy.$t('network.secGroups.page.secGroupDetail'),
    closeText: proxy.$t('network.secGroups.page.closeSecGroupDetail'),
    direction: 'rtl',
    size: '80%',
    close: false,
    isDrawer: true,
    link: `/secGroups/${item.sgId}?type=${type}`,
    pageType: type,
    linkName: proxy.$t('network.secGroups.page.openSecGroupDetail'),
    id: item.sgId,
  };
  currentView.value = secGroupsdetail;

  drawer.value = true;
};
const handleSizeChange = (val: any) => {
  // 改变每页显示数量
  form.page_size = val;
  localStorage.setItem('page_size', val);
  mainStoreData.page_size = val;
  getSgsList();
};
const handleCurrentChange = (val: any) => {
  // 改变页码
  form.page_num = val;
  getSgsList();
};
const addSecGroups = () => {
  // 打开添加安全组
  addSecGroupsStatus.value = true;
};
const onCloseAdd = () => {
  // 关闭添加安全组弹窗
  addSecGroupsStatus.value = false;
  addType.value = 'add'; // 添加还是编辑 add-edit
  addForm.name = '';
  addForm.description = '';
  sgId.value = '';
};

const toEdit = (item: any) => {
  // 编辑
  addSecGroups();
  addType.value = 'edit';
  addForm.name = item.name;
  addForm.description = item.description;
  sgId.value = item.sgId;
};
const toDelete = (item: any) => {
  // 删除
  mainApi
    .sgsDel(item.sgId)
    .then((res: any) => {
      loading.value = false;
      ElMessage.success(proxy.$t('network.secGroups.message.startDelete'));
      getSgsList();
    })
    .catch((error: any) => {
      loading.value = false;
    });
  return true;
};
const onSubmit = () => {
  // 提交查询
  form.page_num = 1;
  getSgsList();
};
const onReset = () => {
  // 重置查询
  form.name = '';
  form.page_num = 1;
  getSgsList();
};
const getSgsList = () => {
  // sgs列表
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
    .sgsList(params)
    .then((res: any) => {
      loading.value = false;
      tableData.value = res.securityGroups;
      form.total = res.totalNum;
    })
    .catch((error: any) => {
      loading.value = false;
    });
};
const getSgsListTime = () => {
  // sgs列表
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

  mainApi.sgsList(params).then((res: any) => {
    tableData.value = res.securityGroups;
    form.total = res.totalNum;
  });
};
onMounted(() => {
  getSgsList(); // sgs列表
  timer.value = setInterval(async () => {
    getSgsListTime(); // 请求sgs循环列表
  }, mainStoreData.listRefreshTime);
});
onUnmounted(() => {
  clearInterval(timer.value);
});
</script>

<style lang="scss" scoped>
.sgsPage {
}
</style>

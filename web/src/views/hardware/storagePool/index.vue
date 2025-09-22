<template>
  <div class="subNetPage h-full">
    <!-- <h5 class="mb-3 px-5 pt-2">
      <span class="text-lg">{{ $route.meta.title }}</span>
      <el-divider class="!my-2"></el-divider>
    </h5> -->
    <div class="py-3 px-4 mb-4 bg-white rounded-lg tableTop">
      <el-form :model="form" :inline="true" :size="mainStoreData.viewSize.main">
        <el-form-item :label="$t('hardware.storagePool.form.storagePoolName') + ':'">
          <el-input
            v-model="form.name"
            class="!w-50"
            :placeholder="$t('hardware.storagePool.form.inputStoragePoolName')"
          />
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
        <el-table-column prop="date" :label="$t('hardware.storagePool.form.name')">
          <template #default="scope">
            <span class="text-blue-400 cursor-pointer" @click="toDetail(scope.row)">{{ scope.row.name }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="type" :label="$t('hardware.storagePool.form.type')">
          <template #default="scope">
            <span v-if="scope.row.type == 1">{{ $t('hardware.storagePool.form.fileSystem') }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <!-- <el-table-column prop="paras"
                         label="paras">
          <template #default="scope">
            {{ scope.row.paras }}
          </template>
        </el-table-column> -->
        <el-table-column prop="description" :label="$t('hardware.storagePool.form.description')">
          <template #default="scope">
            <div class="text-ellipsis-2">
              {{ scope.row.description }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="$t('hardware.storagePool.form.createTime')" />
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

import storagePooladd from './add.vue';
import storagePooledit from './edit.vue';
import storagePooldetail from './detail.vue';

const { proxy }: any = getCurrentInstance();
const mainStoreData = mainStore(); // pinia 信息
const router = useRouter();

const timer: any = ref('');

const loading = ref(false);

const drawer = ref(false);
const drawerData: any = ref('');
const currentView: any = ref(storagePooldetail);
const closeDrawer = () => {
  drawer.value = false;
  currentView.value = '';
  drawerData.value = '';
  getStoragePools();
};

const toEdit = (item: any) => {
  // 编辑
  drawerData.value = {
    title: proxy.$t('hardware.storagePool.page.editStoragePool'),
    closeText: proxy.$t('hardware.storagePool.page.closeEdit'),
    direction: 'rtl',
    size: '80%',
    close: true,
    isDrawer: true,
    link: `/storagePoolEdit/${item.poolId}`,
    linkName: proxy.$t('hardware.storagePool.page.openEdit'),
    id: item.poolId,
  };
  currentView.value = storagePooledit;
  drawer.value = true;
};
const toDetail = (item: any) => {
  // 详情
  drawerData.value = {
    title: proxy.$t('hardware.storagePool.page.detailStoragePool'),
    closeText: proxy.$t('hardware.storagePool.page.closeDetail'),
    direction: 'rtl',
    size: '80%',
    close: false,
    isDrawer: true,
    link: `/storagePool/${item.poolId}`,
    linkName: proxy.$t('hardware.storagePool.page.openDetail'),
    id: item.poolId,
  };
  currentView.value = storagePooldetail;

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
  getStoragePools();
};
const handleCurrentChange = (val: any) => {
  // 改变页码
  console.log(`current page: ${val}`);
  form.page_num = val;
  getStoragePools();
};

const toDelete = (item: any) => {
  // 删除
  mainApi
    .storagePoolsDel(item.poolId)
    .then((res: any) => {
      loading.value = false;
      ElMessage.success(proxy.$t('common.operations.success.delete'));
      getStoragePools();
    })
    .catch((error: any) => {
      loading.value = false;
    });
  return true;
};
const onSubmit = () => {
  // 提交查询
  form.page_num = 1;
  getStoragePools();
};
const onReset = () => {
  // 重置查询
  form.name = '';
  form.page_num = 1;
  getStoragePools();
};
const getStoragePools = () => {
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
    .storagePoolsList(params)
    .then((res: any) => {
      loading.value = false;
      tableData.value = res.storagePools;
      form.total = res.totalNum;
    })
    .catch((error: any) => {
      loading.value = false;
    });
};
const getStoragePoolsTime = () => {
  mainApi.storagePoolsList().then((res: any) => {
    tableData.value = res.storagePools;
    form.total = res.totalNum;
  });
};
onMounted(() => {
  getStoragePools(); // 存储池列表
  timer.value = setInterval(async () => {
    getStoragePoolsTime(); // 请求存储池循环列表
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

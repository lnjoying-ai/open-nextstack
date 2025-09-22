<template>
  <div class="flavorsPage h-full">
    <!-- <h5 class="mb-3 px-5 pt-2">
      <span class="text-lg">{{ $route.meta.title }}</span>
      <el-divider class="!my-2"></el-divider>
    </h5> -->
    <div class="py-3 px-4 mb-4 bg-white rounded-lg tableTop">
      <el-form :model="form" :inline="true" :size="mainStoreData.viewSize.main">
        <el-form-item :label="$t('common.name') + ':'">
          <el-input v-model="form.name" class="!w-50" :placeholder="$t('common.name')" />
        </el-form-item>
        <el-form-item :label="$t('common.type') + ':'">
          <el-select v-model="form.type" class="!w-50" :placeholder="$t('common.type')" @change="onSubmit">
            <el-option :label="$t('common.all')" value="0" />
            <el-option :label="$t('compute.flavor.type.general')" value="1" />
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

        <el-button type="primary" class="float-right" @click="addFlavors">
          {{ $t('compute.flavor.create') }}
        </el-button>
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
        <el-table-column prop="type" :label="$t('compute.flavor.search.type')">
          <template #default="scope">
            <span v-if="scope.row.gpuCount">
              {{ $t('compute.flavor.type.gpu') }}
            </span>
            <span v-else>
              {{ $t('compute.flavor.type.general') }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="cpu" :label="$t('compute.flavor.search.cpu')">
          <template #default="scope"> {{ scope.row.cpu || '-' }}{{ $t('common.core') }} </template>
        </el-table-column>
        <el-table-column prop="mem" :label="$t('compute.flavor.search.memory')">
          <template #default="scope"> {{ scope.row.mem || '-' }}GB </template>
        </el-table-column>

        <el-table-column prop="gpuCount" label="GPU">
          <template #default="scope">
            <span v-if="scope.row.gpuCount && scope.row.gpuCount > 0">
              {{ scope.row.gpuName }}*{{ scope.row.gpuCount }}
            </span>
            <span v-else>-</span>
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
                  <el-popconfirm
                    :confirm-button-text="$t('common.delete')"
                    :cancel-button-text="$t('common.cancel')"
                    icon-color="#626AEF"
                    :title="$t('compute.flavor.confirm.delete')"
                    @confirm="toDelete(scope.row)"
                  >
                    <template #reference>
                      <span class="listDelBtn">
                        <img src="@/assets/img/btn/delete.png" class="w-3 float-left mt-3px mr-1" alt="" />
                        {{ $t('common.delete') }}
                      </span>
                    </template>
                  </el-popconfirm>
                  <el-dropdown-item>
                    <span class="w-full" @click="toEdit(scope.row)">
                      <img src="@/assets/img/btn/edit.png" class="w-3 float-left mt-3px mr-1" alt="" />
                      {{ $t('common.edit') }}
                    </span>
                  </el-dropdown-item>
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
import flavorsadd from './add.vue';
import flavorsedit from './edit.vue';
import flavorsdetail from './detail.vue';

const { proxy }: any = getCurrentInstance();
const mainStoreData = mainStore(); // pinia 信息
const router = useRouter();
const timer: any = ref('');

const loading = ref(false);

const drawer = ref(false);
const drawerData: any = ref('');
const currentView: any = ref(flavorsdetail);
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
  type: '0',
  page_num: 1,
  page_size: mainStoreData.page_size,
  total: 0,
});

const tableData: any = ref([]);
const closeDrawer = () => {
  drawer.value = false;
  currentView.value = '';
  drawerData.value = '';
  getflavorsList();
};
const addFlavors = () => {
  drawerData.value = {
    title: proxy.$t('compute.flavor.create'),
    closeText: proxy.$t('compute.flavor.confirm.cancelAdd'),
    direction: 'rtl',
    size: '80%',
    close: true,
    isDrawer: true,
    link: '/flavorsAdd',
    linkName: proxy.$t('compute.flavor.confirm.openAdd'),
    id: '',
  };
  currentView.value = flavorsadd;
  drawer.value = true;
};
const toEdit = (item: any) => {
  // 编辑
  drawerData.value = {
    title: proxy.$t('compute.flavor.edit'),
    closeText: proxy.$t('compute.flavor.confirm.cancelEdit'),
    direction: 'rtl',
    size: '80%',
    close: true,
    isDrawer: true,
    link: `/flavorsEdit/${item.flavorId}`,
    linkName: proxy.$t('compute.flavor.confirm.openEdit'),
    id: item.flavorId,
  };
  currentView.value = flavorsedit;
  drawer.value = true;
};
const toDetail = (item: any) => {
  // 详情
  drawerData.value = {
    title: proxy.$t('compute.flavor.detail'),
    closeText: proxy.$t('compute.flavor.confirm.closeDetail'),
    direction: 'rtl',
    size: '80%',
    close: false,
    isDrawer: true,
    link: `/flavors/${item.flavorId}`,
    linkName: proxy.$t('compute.flavor.confirm.openDetail'),
    id: item.flavorId,
  };
  currentView.value = flavorsdetail;

  drawer.value = true;
};
const toDelete = (item: any) => {
  // 删除
  mainApi
    .flavorsDel(item.flavorId)
    .then((res: any) => {
      loading.value = false;
      ElMessage.success(proxy.$t('common.startDelete'));
      getflavorsList();
    })
    .catch((error: any) => {
      loading.value = false;
    });
  return true;
};
const handleSizeChange = (val: any) => {
  // 改变每页显示数量
  console.log(`${val} items per page`);
  localStorage.setItem('page_size', val);
  mainStoreData.page_size = val;
  form.page_size = val;
  getflavorsList();
};
const handleCurrentChange = (val: any) => {
  // 改变页码
  console.log(`current page: ${val}`);
  form.page_num = val;
  getflavorsList();
};

const onSubmit = () => {
  // 提交查询
  form.page_num = 1;
  getflavorsList();
};
const onReset = () => {
  // 重置查询
  form.name = '';
  form.type = '0';
  form.page_num = 1;
  getflavorsList();
};
const getflavorsList = () => {
  // 规格列表
  loading.value = true;

  const params: any = {
    name: form.name,
    type: form.type,
    page_num: form.page_num,
    page_size: form.page_size,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }

  mainApi
    .flavorsList(params)
    .then((res: any) => {
      loading.value = false;
      tableData.value = res.flavors;
      form.total = res.totalNum;
    })
    .catch((error: any) => {
      loading.value = false;
    });
};
const getflavorsListTime = () => {
  // 规格循环列表

  const params: any = {
    name: form.name,
    type: form.type,
    page_num: form.page_num,
    page_size: form.page_size,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }

  mainApi.flavorsList(params).then((res: any) => {
    tableData.value = res.flavors;
    form.total = res.totalNum;
  });
};
onMounted(() => {
  getflavorsList(); // 请求规格列表
  timer.value = setInterval(async () => {
    getflavorsListTime(); // 请求规格循环列表
  }, mainStoreData.listRefreshTime);
});
onUnmounted(() => {
  clearInterval(timer.value);
});
</script>

<style lang="scss" scoped>
.flavorsPage {
}
</style>

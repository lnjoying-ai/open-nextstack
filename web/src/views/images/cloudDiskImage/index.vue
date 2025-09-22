<template>
  <div class="subNetPage h-full">
    <!-- <h5 class="mb-3 px-5 pt-2">
      <span class="text-lg">{{ $route.meta.title }}</span>
      <el-divider class="!my-2"></el-divider>
    </h5> -->
    <div class="py-3 px-4 mb-4 bg-white rounded-lg tableTop">
      <el-form :model="form" :inline="true" :size="mainStoreData.viewSize.main">
        <el-form-item :label="$t('images.cloudDiskImage.form.imageName') + ':'">
          <el-input v-model="form.name" class="!w-50" :placeholder="$t('images.cloudDiskImage.form.inputImageName')" />
        </el-form-item>
        <el-form-item :label="$t('images.cloudDiskImage.form.imageType') + ':'">
          <el-select
            v-model="form.is_vm"
            class="!w-50"
            :placeholder="$t('images.cloudDiskImage.form.inputImageType')"
            @change="onSubmit"
          >
            <el-option :label="$t('images.cloudDiskImage.form.all')" :value="''" />
            <el-option :label="$t('images.cloudDiskImage.form.virtualMachine')" :value="true" />
            <!-- <el-option :label="$t('images.cloudDiskImage.form.bareMetal')" :value="false" /> -->
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
        <el-table-column prop="date" :label="$t('images.cloudDiskImage.form.name')">
          <template #default="scope">
            <span class="text-blue-400 cursor-pointer" @click="toDetail(scope.row)">{{ scope.row.imageName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="imageOsType" :label="$t('images.cloudDiskImage.form.osType')">
          <template #default="scope">
            <span v-if="scope.row.imageOsType == 0">linux</span>
            <span v-else-if="scope.row.imageOsType == 1">windows</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="isPublic" :label="$t('images.cloudDiskImage.form.isPublic')">
          <template #default="scope">
            <span v-if="scope.row.isPublic">{{ $t('images.cloudDiskImage.form.yes') }}</span>
            <span v-else>{{ $t('images.cloudDiskImage.form.no') }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="imageFormat" :label="$t('images.cloudDiskImage.form.imageFormat')">
          <template #default="scope">
            <span v-if="scope.row.imageFormat == 4">{{ $t('images.cloudDiskImage.form.bareMetal') }}</span>
            <span v-else-if="scope.row.imageFormat == 3">{{ $t('images.cloudDiskImage.form.virtualMachine') }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="phaseStatus" :label="$t('images.cloudDiskImage.form.status')">
          <template #default="scope">
            <el-tag
              :size="mainStoreData.viewSize.tagStatus"
              :type="filtersFun.getVolumeStatus(scope.row.phaseStatus, 'tag')"
              >{{ filtersFun.getVolumeStatus(scope.row.phaseStatus, 'status') }}</el-tag
            >
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="$t('images.cloudDiskImage.form.createTime')" />
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

import cloudDiskImagedetail from './detail.vue';

const { proxy }: any = getCurrentInstance();
const mainStoreData = mainStore(); // pinia 信息
const router = useRouter();

const timer: any = ref('');

const loading = ref(false);

const drawer = ref(false);
const drawerData: any = ref('');
const currentView: any = ref(cloudDiskImagedetail);
const closeDrawer = () => {
  drawer.value = false;
  currentView.value = '';
  drawerData.value = '';
  getImagesList();
};

const toDetail = (item: any) => {
  // 详情
  drawerData.value = {
    title: proxy.$t('images.cloudDiskImage.page.detailImage'),
    closeText: proxy.$t('images.cloudDiskImage.page.closeDetail'),
    direction: 'rtl',
    size: '80%',
    close: false,
    isDrawer: true,
    link: `/cloudDiskImage/${item.imageId}`,
    linkName: proxy.$t('images.cloudDiskImage.page.openDetail'),
    id: item.imageId,
  };
  currentView.value = cloudDiskImagedetail;

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
  is_vm: '',
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
  getImagesList();
};
const handleCurrentChange = (val: any) => {
  // 改变页码
  console.log(`current page: ${val}`);
  form.page_num = val;
  getImagesList();
};

const toDelete = (item: any) => {
  // 删除
  mainApi
    .eipPoolsDel(item.poolId)
    .then((res: any) => {
      loading.value = false;
      ElMessage.success(proxy.$t('common.operations.success.delete'));
      getImagesList();
    })
    .catch((error: any) => {
      loading.value = false;
    });
  return true;
};
const onSubmit = () => {
  // 提交查询
  form.page_num = 1;
  getImagesList();
};
const onReset = () => {
  // 重置查询
  form.name = '';
  form.is_vm = '';
  form.page_num = 1;
  getImagesList();
};
const getImagesList = () => {
  // images列表
  loading.value = true;
  const params: any = {
    name: form.name,
    is_vm: form.is_vm,
    page_num: form.page_num,
    page_size: form.page_size,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }
  mainApi
    .imageList(params)
    .then((res: any) => {
      loading.value = false;
      tableData.value = res.images;
      form.total = res.totalNum;
    })
    .catch((error: any) => {
      loading.value = false;
    });
};
const getImagesListTime = () => {
  // images列表
  const params: any = {
    name: form.name,
    is_vm: form.is_vm,
    page_num: form.page_num,
    page_size: form.page_size,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }
  mainApi.imageList(params).then((res: any) => {
    tableData.value = res.images;
    form.total = res.totalNum;
  });
};
onMounted(() => {
  getImagesList(); // images列表
  timer.value = setInterval(async () => {
    getImagesListTime(); // 请求镜像循环列表
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

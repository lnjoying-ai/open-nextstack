<template>
  <div class="bmsPage h-full">
    <!-- <h5 class="mb-3 px-5 pt-2">
      <span class="text-lg">{{ $route.meta.title }}</span>
      <el-divider class="!my-2"></el-divider>
    </h5> -->
    <div class="py-3 px-4 mb-4 bg-white rounded-lg tableTop">
      <el-form :model="form" :inline="true" :size="mainStoreData.viewSize.main">
        <el-form-item :label="$t('publicKey.form.publicKeyName') + ':'">
          <el-input v-model="form.name" class="!w-50" :placeholder="$t('publicKey.form.inputPublicKeyName')" />
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

        <el-button type="primary" class="float-right" @click="uploadKey">{{
          $t('publicKey.form.uploadPublicKey')
        }}</el-button>
        <el-button type="primary" class="mr-2 float-right" @click="addKey">{{
          $t('publicKey.form.createPublicKey')
        }}</el-button>
      </div>

      <el-table
        :size="mainStoreData.viewSize.main"
        :data="tableData"
        max-height="calc(100vh - 250px)"
        class="!overflow-y-auto"
        stripe
        :scrollbar-always-on="false"
      >
        <el-table-column prop="date" :label="$t('publicKey.form.name')">
          <template #default="scope">
            <span class="text-blue-400 cursor-pointer" @click="toDetail(scope.row)">{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" :label="$t('publicKey.form.description')" />
        <el-table-column prop="createTime" :label="$t('publicKey.form.createTime')" />
        <el-table-column prop="address" :label="$t('common.operation')" width="120">
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
                    >
                  </el-dropdown-item>
                  <el-popconfirm
                    :confirm-button-text="$t('common.delete')"
                    :cancel-button-text="$t('common.cancel')"
                    icon-color="#626AEF"
                    :title="$t('publicKey.message.deletePublicKey')"
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
import mainStore from '@/store/mainStore';
import publickeyadd from './add.vue';
import publickeyupload from './upload.vue';
import publickeydetail from './detail.vue';
import publickeyedit from './edit.vue';

const { proxy }: any = getCurrentInstance();
const mainStoreData = mainStore(); // pinia 信息
const timer: any = ref('');

const loading = ref(false);
const router = useRouter();

const drawer = ref(false);
const drawerData: any = ref('');
const currentView: any = ref(publickeydetail);

const form = reactive({
  // 搜索 筛选
  name: '',
  page_num: 1,
  page_size: mainStoreData.page_size,
  total: 0,
});

const tableData: any = ref([]);
// const tableData = ref([]);
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
const closeDrawer = () => {
  drawer.value = false;
  currentView.value = '';
  drawerData.value = '';
  getpubkeysList();
};
const uploadKey = () => {
  drawerData.value = {
    title: proxy.$t('publicKey.page.uploadPublicKey'),
    closeText: proxy.$t('publicKey.page.cancelUploadPublicKey'),
    direction: 'rtl',
    size: '80%',
    close: true,
    isDrawer: true,
    link: '/publicKeyUpload',
    linkName: proxy.$t('publicKey.page.openUploadPublicKey'),
    id: '',
  };
  currentView.value = publickeyupload;
  drawer.value = true;
};
const addKey = () => {
  drawerData.value = {
    title: proxy.$t('publicKey.page.createPublicKey'),
    closeText: proxy.$t('publicKey.page.cancelCreatePublicKey'),
    direction: 'rtl',
    size: '80%',
    close: false,
    isDrawer: true,
    link: '/publicKeyAdd',
    linkName: proxy.$t('publicKey.page.openCreatePublicKey'),
    id: '',
  };
  currentView.value = publickeyadd;
  drawer.value = true;
};
const toEdit = (item: any) => {
  // 修改
  drawerData.value = {
    title: proxy.$t('publicKey.page.editPublicKey'),
    closeText: proxy.$t('publicKey.page.cancelEditPublicKey'),
    direction: 'rtl',
    size: '80%',
    close: false,
    isDrawer: true,
    link: `/publicKey/${item.pubkeyId}`,
    linkName: proxy.$t('publicKey.page.openEditPublicKey'),
    id: item.pubkeyId,
  };
  currentView.value = publickeyedit;

  drawer.value = true;
};
const toDetail = (item: any) => {
  // 详情
  drawerData.value = {
    title: proxy.$t('publicKey.page.detailPublicKey'),
    closeText: proxy.$t('publicKey.page.cancelDetailPublicKey'),
    direction: 'rtl',
    size: '80%',
    close: false,
    isDrawer: true,
    link: `/publicKey/${item.pubkeyId}`,
    linkName: proxy.$t('publicKey.page.openDetailPublicKey'),
    id: item.pubkeyId,
  };
  currentView.value = publickeydetail;

  drawer.value = true;
};
const handleSizeChange = (val: any) => {
  // 改变每页显示数量
  console.log(`${val} items per page`);
  localStorage.setItem('page_size', val);
  mainStoreData.page_size = val;
  form.page_size = val;
  getpubkeysList();
};
const handleCurrentChange = (val: any) => {
  // 改变页码
  console.log(`current page: ${val}`);

  form.page_num = val;
  getpubkeysList();
};

const toDelete = (item: any) => {
  // 删除
  mainApi
    .pubkeysDel(item.pubkeyId)
    .then((res: any) => {
      loading.value = false;
      ElMessage.success(proxy.$t('common.operations.success.delete'));
      getpubkeysList();
    })
    .catch((error: any) => {
      loading.value = false;
    });
  return true;
};
const onSubmit = () => {
  // 提交
  form.page_num = 1;
  getpubkeysList();
};
const onReset = () => {
  // 重置
  form.name = '';
  form.page_num = 1;
  getpubkeysList();
};
const getpubkeysList = () => {
  // 公钥列表
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
    .pubkeysList(params)
    .then((res: any) => {
      loading.value = false;
      tableData.value = res.pubkeys;
      form.total = res.totalNum;
    })
    .catch((error: any) => {
      loading.value = false;
    });
};
const getpubkeysListTime = () => {
  // 公钥循环列表
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

  mainApi.pubkeysList(params).then((res: any) => {
    tableData.value = res.pubkeys;
    form.total = res.totalNum;
  });
};
onMounted(() => {
  getpubkeysList(); // 公钥列表
  timer.value = setInterval(async () => {
    getpubkeysListTime(); // 请求公钥循环列表
  }, mainStoreData.listRefreshTime);
});
onUnmounted(() => {
  clearInterval(timer.value);
});
</script>

<style lang="scss" scoped>
.bmsPage {
}
</style>

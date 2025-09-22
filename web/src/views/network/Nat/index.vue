<template>
  <div class="natPage h-full">
    <!-- <h5 class="mb-3 px-5 pt-2">
      <span class="text-lg">{{ $route.meta.title }}</span>
      <el-divider class="!my-2"></el-divider>
    </h5> -->
    <div class="py-3 px-4 mb-4 bg-white rounded-lg tableTop">
      <el-form :model="form" :inline="true" :size="mainStoreData.viewSize.main">
        <el-form-item :label="$t('network.nat.form.name') + ':'">
          <el-input v-model="form.name" class="!w-50" :placeholder="$t('network.nat.form.namePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('network.nat.form.eip') + ':'">
          <el-select
            v-model="form.eip_id"
            class="!w-50"
            :placeholder="$t('network.nat.form.selectEip')"
            @change="onSubmit"
          >
            <el-option :label="$t('network.nat.form.all')" value="" />
            <el-option
              v-for="(item, index) in eipTableData"
              :key="index"
              :label="item.ipAddress"
              :value="item.eipId"
            ></el-option>
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

        <el-button type="primary" class="float-right" @click="addNat">{{ $t('network.nat.form.createNat') }}</el-button>
      </div>

      <el-table
        v-loading="loading"
        :size="mainStoreData.viewSize.main"
        :data="tableData"
        max-height="calc(100% - 3rem)"
        class="!overflow-y-auto"
        stripe
        :scrollbar-always-on="false"
        :element-loading-text="$t('common.loading')"
      >
        <el-table-column prop="date" :label="$t('network.nat.form.name')">
          <template #default="scope">
            <span class="text-blue-400 cursor-pointer" @click="toDetail(scope.row)">{{ scope.row.mapName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="eipAddress" :label="$t('network.nat.form.publicIp')" />
        <el-table-column prop="name" :label="$t('network.nat.form.protocol')">
          <template #default="scope">
            <span v-if="scope.row.oneToOne">{{ $t('network.nat.form.ip') }}</span>
            <span v-else>
              <span v-for="(item, index) in scope.row.portMaps" :key="index" class="block">
                {{
                  item.protocol == 0
                    ? $t('network.nat.form.tcp')
                    : item.protocol == 1
                    ? $t('network.nat.form.udp')
                    : ''
                }}:{{ item.globalPort }}
              </span>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="vpcId" :label="$t('network.nat.form.vpc')">
          <template #default="scope">
            <router-link :to="'/vpc/' + scope.row.vpcId" target="_blank" class="text-blue-400">{{
              scope.row.vpcName
            }}</router-link>
          </template>
        </el-table-column>
        <el-table-column prop="subnetCidr" :label="$t('network.nat.form.subnet')" />
        <el-table-column prop="instanceName" :label="$t('network.nat.form.server')">
          <template #default="scope">
            <router-link :to="'/vm/' + scope.row.instanceId" target="_blank" class="text-blue-400">{{
              scope.row.instanceName
            }}</router-link>
          </template>
        </el-table-column>
        <el-table-column prop="phaseStatus" :label="$t('network.nat.form.status')">
          <template #default="scope">
            <el-tag
              :size="mainStoreData.viewSize.tagStatus"
              :type="filtersFun.getNatStatus(scope.row.phaseStatus, 'tag')"
              >{{ filtersFun.getNatStatus(scope.row.phaseStatus, 'status') }}</el-tag
            >
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="$t('network.nat.form.createTime')" />
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
                    :title="$t('network.nat.validation.confirmDelete')"
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

import natadd from './add.vue';
import natdetail from './detail.vue';
import natedit from './edit.vue';

const { proxy }: any = getCurrentInstance();
const router = useRouter();
const mainStoreData = mainStore(); // pinia 信息
const timer: any = ref('');

const loading = ref(false);

const drawer = ref(false);
const drawerData: any = ref('');
const currentView: any = ref(natdetail);
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

const form: any = reactive({
  // 搜索 筛选
  name: '',
  eip_id: '',
  page_num: 1,
  page_size: mainStoreData.page_size,
  total: 0,
});
const tableData: any = ref([]);
const eipTableData: any = ref([]);

const closeDrawer = () => {
  drawer.value = false;
  currentView.value = '';
  drawerData.value = '';
  getportMapList();
};
const addNat = () => {
  drawerData.value = {
    title: proxy.$t('network.nat.page.createNat'),
    closeText: proxy.$t('network.nat.page.closeCreateNat'),
    direction: 'rtl',
    size: '80%',
    close: true,
    isDrawer: true,
    link: '/NatAdd',
    linkName: proxy.$t('network.nat.page.openCreateNat'),
    id: '',
  };
  currentView.value = natadd;
  drawer.value = true;
};
const toEdit = (item: any) => {
  // 编辑
  drawerData.value = {
    title: proxy.$t('network.nat.page.editNat'),
    closeText: proxy.$t('network.nat.page.closeEditNat'),
    direction: 'rtl',
    size: '80%',
    close: true,
    isDrawer: true,
    link: `/natEdit/${item.eipMapId}`,
    linkName: proxy.$t('network.nat.page.openEditNat'),
    id: item.eipMapId,
  };
  currentView.value = natedit;
  drawer.value = true;
};
const toDetail = (item: any) => {
  // 详情
  drawerData.value = {
    title: proxy.$t('network.nat.page.natDetail'),
    closeText: proxy.$t('network.nat.page.closeNatDetail'),
    direction: 'rtl',
    size: '80%',
    close: false,
    isDrawer: true,
    link: `/Nat/${item.eipMapId}`,
    linkName: proxy.$t('network.nat.page.openNatDetail'),
    id: item.eipMapId,
  };
  currentView.value = natdetail;

  drawer.value = true;
};

const handleSizeChange = (val: any) => {
  // 改变每页显示数量
  form.page_size = val;
  localStorage.setItem('page_size', val);
  mainStoreData.page_size = val;
  getportMapList();
};
const handleCurrentChange = (val: any) => {
  // 改变页码
  form.page_num = val;
  getportMapList();
};

const toDelete = (item: any) => {
  // 删除
  mainApi
    .portMapDel(item.eipMapId)
    .then((res: any) => {
      ElMessage.success(proxy.$t('network.nat.message.startDelete'));
      onSubmit();
    })
    .catch((error: any) => {});
  return true;
};
const onSubmit = () => {
  // 提交查询
  form.page_num = 1;
  getportMapList();
};
const onReset = () => {
  // 重置查询
  form.name = '';
  form.eip_id = '';
  form.page_num = 1;
  getportMapList();
};
const getportMapList = () => {
  // 获取数据
  loading.value = true;

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
    .portMapList(params)
    .then((res: any) => {
      loading.value = false;
      tableData.value = res.eipPortMaps;
      form.total = res.totalNum;
    })
    .catch((error: any) => {
      loading.value = false;
    });
};
const getportMapListTime = () => {
  // 获取数据

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

  mainApi.portMapList(params).then((res: any) => {
    tableData.value = res.eipPortMaps;
    form.total = res.totalNum;
  });
};
const getEipList = () => {
  // eip列表
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
  getportMapList(); // 请求列表
  timer.value = setInterval(async () => {
    getportMapListTime(); // 请求Nat循环列表
  }, mainStoreData.listRefreshTime);
});
onUnmounted(() => {
  clearInterval(timer.value);
});
</script>

<style lang="scss" scoped>
.natPage {
}
</style>

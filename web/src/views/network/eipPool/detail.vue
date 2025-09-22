<template>
  <div class="subNetAddPage h-full">
    <h5 v-if="!(drawerData && drawerData.isDrawer)" class="bg-white mb-3 px-5 pt-2 pb-2 rounded-md">
      <el-page-header :title="$t('common.backToList')" @back="goBack">
        <template #content> {{ $route.meta.title }} </template>
      </el-page-header>
    </h5>
    <el-form :size="mainStoreData.viewSize.main" :model="form" label-width="160px">
      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('network.eipPool.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('network.eipPool.form.name') + ':'">
            <span>{{ form.name || '-' }}</span>
          </el-form-item>

          <el-form-item :label="$t('network.eipPool.form.vlanId') + ':'">
            <span>{{ form.vlanId || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('network.eipPool.form.description') + ':'">
            <span>
              {{ form.description || '-' }}
            </span>
          </el-form-item>
          <el-form-item :label="$t('network.eipPool.form.updateTime') + ':'">
            <span>
              {{ form.updateTime || '-' }}
            </span>
          </el-form-item>
          <el-form-item :label="$t('network.eipPool.form.createTime') + ':'">
            <span>
              {{ form.createTime || '-' }}
            </span>
          </el-form-item>
        </div>
      </el-card>
      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('network.eipPool.eipInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-table :data="tableData" stripe style="width: 100%">
            <el-table-column :label="$t('network.eipPool.form.eip')" prop="ipAddress">
              <template #default="scope">
                <span>{{ scope.row.ipAddress }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="$t('network.eipPool.form.description')" prop="description">
              <template #default="scope">
                <span>{{ scope.row.description }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="$t('network.eipPool.form.createTime')" prop="createTime">
              <template #default="scope">
                <span>{{ scope.row.createTime }}</span>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:page_num="formList.page_num"
            v-model:page-size="formList.page_size"
            class="!py-4 !pr-8 float-right"
            :page-sizes="mainStoreData.page_sizes"
            :current-page="formList.page_num"
            :small="true"
            layout="total, sizes, prev, pager, next, jumper"
            :total="formList.total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </el-card>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import mainStore from '@/store/mainStore'; // pinia 信息
import mainApi from '@/api/modules/main';

const { proxy }: any = getCurrentInstance();
const mainStoreData = mainStore();

const { drawerData } = defineProps<{
  drawerData: any;
}>();

const router: any = useRouter();
const tableData: any = ref([]);

const form: any = ref({});
const formList = reactive({
  // 搜索 筛选
  name: '',
  page_num: 1,
  page_size: mainStoreData.page_size,
  total: 0,
});
const goBack = () => {
  router.push('/eipPool');
};
const getDetail = () => {
  // 获取详情
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  mainApi
    .eipPoolsDetail(id)
    .then((res: any) => {
      form.value = res;
      getEipList();
    })
    .catch((error: any) => {});
};
const getEipList = () => {
  // eip列表
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  const params: any = {
    name: formList.name,
    page_num: formList.page_num,
    page_size: formList.page_size,
    eip_pool_id: id,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }
  mainApi.eipsList(params).then((res: any) => {
    tableData.value = res.eips;
    formList.total = res.totalNum;
  });
};
const handleSizeChange = (val: any) => {
  // 改变每页显示数量
  localStorage.setItem('page_size', val);
  mainStoreData.page_size = val;
  formList.page_size = val;
  getEipList();
};
const handleCurrentChange = (val: any) => {
  // 改变页码
  formList.page_num = val;
  getEipList();
};
onMounted(() => {
  getDetail(); // 获取详情
});
</script>

<style lang="scss" scoped>
.subNetAddPage {
}
</style>

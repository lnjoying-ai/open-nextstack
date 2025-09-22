<template>
  <div class="subNetPage h-full">
    <!-- <h5 class="mb-3 px-5 pt-2">
      <span class="text-lg">{{ $route.meta.title }}</span>
      <el-divider class="!my-2"></el-divider>
    </h5> -->
    <div class="py-3 px-4 mb-4 bg-white rounded-lg tableTop">
      <el-form :model="form" :inline="true" :size="mainStoreData.viewSize.main">
        <el-form-item :label="$t('devOps.event.resourceId') + ':'">
          <el-input v-model="form.detail_info" class="!w-50" :placeholder="$t('devOps.event.inputResourceId')" />
        </el-form-item>
        <el-form-item :label="$t('devOps.event.timeRange') + ':'" class="!mr-0">
          <el-date-picker
            v-model="form.time"
            class="!w-88"
            type="datetimerange"
            :shortcuts="shortcuts"
            range-separator="-"
            :start-placeholder="$t('devOps.event.startTime')"
            :end-placeholder="$t('devOps.event.endTime')"
            @change="onSubmit"
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
        max-height="calc(100vh - 250px - 0.75rem)"
        class="!overflow-y-auto"
        stripe
        :scrollbar-always-on="false"
      >
        <el-table-column prop="content" :label="$t('devOps.event.event')">
          <template #default="scope">
            {{ scope.row.content }}
          </template>
        </el-table-column>
        <el-table-column prop="detailInfo" :label="$t('devOps.event.detailInfo')">
          <template #default="scope">
            {{ scope.row.detailInfo }}
          </template>
        </el-table-column>

        <el-table-column prop="result" :label="$t('devOps.event.result')">
          <template #default="scope">
            {{ scope.row.result }}
          </template>
        </el-table-column>
        <el-table-column prop="userId" :label="$t('devOps.event.userId')">
          <template #default="scope">
            <span class="block">{{ scope.row.username }}</span>
            <span
              ><small>({{ scope.row.userId }})</small></span
            >
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="$t('devOps.event.triggerTime')" />
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
  </div>
</template>

<script setup lang="ts">
import mainApi from '@/api/modules/main';
import filtersFun from '@/utils/statusFun';
import mainStore from '@/store/mainStore';

const { proxy }: any = getCurrentInstance();
const mainStoreData = mainStore(); // pinia 信息
const router = useRouter();

const timer: any = ref('');

const loading = ref(false);
const form = reactive({
  // 搜索 筛选
  detail_info: '',
  time: [],
  page_num: 1,
  page_size: mainStoreData.page_size,
  total: 0,
});
const tableData: any = ref([]);
const shortcuts = [
  {
    text: proxy.$t('devOps.event.lastWeek'),
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 7);
      return [start, end];
    },
  },
  {
    text: proxy.$t('devOps.event.lastMonth'),
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 30);
      return [start, end];
    },
  },
  {
    text: proxy.$t('devOps.event.lastThreeMonths'),
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 90);
      return [start, end];
    },
  },
];
const handleSizeChange = (val: any) => {
  // 改变每页显示数量
  console.log(`${val} items per page`);
  localStorage.setItem('page_size', val);
  mainStoreData.page_size = val;
  form.page_size = val;
  getEventsList();
};
const handleCurrentChange = (val: any) => {
  // 改变页码
  console.log(`current page: ${val}`);
  form.page_num = val;
  getEventsList();
};

const onSubmit = () => {
  // 提交查询
  form.page_num = 1;
  getEventsList();
};
const onReset = () => {
  // 重置查询
  form.detail_info = '';
  form.time = [];
  form.page_num = 1;
  getEventsList();
};
const getEventsList = () => {
  // 事件列表
  loading.value = true;
  const dataTime: any = form.time;
  const params: any = {
    detail_info: form.detail_info,
    start_time: dataTime[0] ? dataTime[0].getTime() : '',
    end_time: dataTime[1] ? dataTime[1].getTime() : '',
    page_num: form.page_num,
    page_size: form.page_size,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }
  mainApi
    .operationEventsList(params)
    .then((res: any) => {
      loading.value = false;
      tableData.value = res.events;
      form.total = res.totalNum;
    })
    .catch((error: any) => {
      loading.value = false;
    });
};
const getEventsListTime = () => {
  // 事件列表
  const dataTime: any = form.time;
  const params: any = {
    detail_info: form.detail_info,
    start_time: dataTime[0] ? dataTime[0].getTime() : '',
    end_time: dataTime[1] ? dataTime[1].getTime() : '',
    page_num: form.page_num,
    page_size: form.page_size,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }
  mainApi.operationEventsList(params).then((res: any) => {
    tableData.value = res.events;
    form.total = res.totalNum;
  });
};
onMounted(() => {
  getEventsList(); // 事件列表
  timer.value = setInterval(async () => {
    getEventsListTime(); // 请求事件循环列表
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

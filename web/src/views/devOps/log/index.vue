<template>
  <div class="subNetPage h-full">
    <!-- <h5 class="mb-3 px-5 pt-2">
      <span class="text-lg">{{ $route.meta.title }}</span>
      <el-divider class="!my-2"></el-divider>
    </h5> -->
    <div class="py-3 px-4 mb-4 bg-white rounded-lg tableTop">
      <el-form :model="form" :inline="true" :size="mainStoreData.viewSize.main">
        <el-form-item :label="$t('devOps.log.log') + ':'">
          <el-input v-model="form.description" class="!w-50" :placeholder="$t('devOps.log.inputLog')" />
        </el-form-item>
        <el-form-item :label="$t('devOps.log.timeRange') + ':'" class="!mr-0">
          <el-date-picker
            v-model="form.time"
            class="!w-88"
            type="datetimerange"
            :shortcuts="shortcuts"
            range-separator="-"
            :start-placeholder="$t('devOps.log.startTime')"
            :end-placeholder="$t('devOps.log.endTime')"
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
        <el-table-column prop="description" width="700" :label="$t('devOps.log.operationDescription')">
          <template #default="scope">
            <span>{{ scope.row.description }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="resource" :label="$t('devOps.log.operationResource')">
          <template #default="scope">
            {{ scope.row.resource }}
          </template>
        </el-table-column>
        <el-table-column prop="operator" :label="$t('devOps.log.operationOperator')">
          <template #default="scope">
            {{ scope.row.operator }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="$t('devOps.log.operationTime')">
          <template #default="scope">
            {{ scope.row.createTime }}
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
  description: '',
  time: [],
  page_num: 1,
  page_size: mainStoreData.page_size,
  total: 0,
});
const tableData: any = ref([]);
const shortcuts = [
  {
    text: proxy.$t('devOps.log.lastWeek'),
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 7);
      return [start, end];
    },
  },
  {
    text: proxy.$t('devOps.log.lastMonth'),
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 30);
      return [start, end];
    },
  },
  {
    text: proxy.$t('devOps.log.lastThreeMonths'),
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
  getLogsList();
};
const handleCurrentChange = (val: any) => {
  // 改变页码
  console.log(`current page: ${val}`);
  form.page_num = val;
  getLogsList();
};

const onSubmit = () => {
  // 提交查询
  form.page_num = 1;
  getLogsList();
};
const onReset = () => {
  // 重置查询
  form.description = '';
  form.time = [];
  form.page_num = 1;
  getLogsList();
};
const getLogsList = () => {
  // 日志列表
  loading.value = true;
  const dataTime: any = form.time;
  const params: any = {
    description: form.description,
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
    .operationLogsList(params)
    .then((res: any) => {
      loading.value = false;
      tableData.value = res.alarmRules;
      form.total = res.totalNum;
    })
    .catch((error: any) => {
      loading.value = false;
    });
};
const getLogsListTime = () => {
  // 日志列表
  const dataTime: any = form.time;
  const params: any = {
    description: form.description,
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
  mainApi.operationLogsList(params).then((res: any) => {
    tableData.value = res.alarmRules;
    form.total = res.totalNum;
  });
};
onMounted(() => {
  getLogsList(); // 日志列表
  timer.value = setInterval(async () => {
    getLogsListTime(); // 请求日志循环列表
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

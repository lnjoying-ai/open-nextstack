<template>
  <div class="snapsAddPage h-full">
    <h5 v-if="!(drawerData && drawerData.isDrawer)" class="bg-white mb-3 px-5 pt-2 pb-2 rounded-md">
      <el-page-header :title="$t('common.back')" @back="goBack">
        <template #content> {{ $route.meta.title }} </template>
      </el-page-header>
    </h5>
    <el-form
      ref="addVmForm"
      v-loading="loading"
      :size="mainStoreData.viewSize.main"
      :model="form"
      :rules="rules"
      label-width="120px"
      :element-loading-text="$t('common.loading')"
    >
      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('common.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('common.name') + ':'" prop="name">
            <span>{{ form.name }}</span>
          </el-form-item>
          <el-form-item :label="$t('common.id') + ':'" prop="instanceGroupId">
            <span>{{ form.instanceGroupId || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('common.description') + ':'" prop="description">
            <span class="!w-100 block">{{ form.description }}</span>
          </el-form-item>
          <el-form-item :label="$t('compute.vmGroup.form.vm') + ':'">
            <el-table
              :size="mainStoreData.viewSize.main"
              :data="vmTableData"
              class="!overflow-y-auto"
              stripe
              :scrollbar-always-on="false"
            >
              <el-table-column prop="date" :label="$t('common.name')">
                <template #default="scope">
                  <!-- 新窗口打开 -->
                  <router-link :to="'/vm/' + scope.row.instanceId" target="_blank">
                    <span class="text-blue-400 cursor-pointer">{{ scope.row.name }}</span>
                  </router-link>
                </template>
              </el-table-column>

              <el-table-column prop="hostname" :label="$t('compute.vmGroup.form.hostname')" />
              <el-table-column prop="portInfo.ipAddress" :label="$t('compute.vmGroup.form.ip')">
                <template #default="scope">
                  <span>{{ scope.row.portInfo.ipAddress || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="vpcInfo.cidr" :label="$t('compute.vmGroup.form.networkAddress')">
                <template #default="scope">
                  <span>{{ scope.row.subnetInfo.cidr || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="eip" :label="$t('compute.vmGroup.form.eip')">
                <template #default="scope">
                  <span v-if="!scope.row.boundPhaseStatus">{{ '-' }}</span>
                  <span v-if="scope.row.boundType && scope.row.boundType === 'port'">
                    <span v-if="scope.row.boundPhaseStatus == 82">{{ scope.row.eip || '-' }}</span>
                    <el-tag
                      v-else
                      :size="mainStoreData.viewSize.tagStatus"
                      :type="filtersFun.getVmToEipStatus(scope.row.boundPhaseStatus, 'tag')"
                      >{{ filtersFun.getVmToEipStatus(scope.row.boundPhaseStatus, 'status') }}</el-tag
                    >
                  </span>
                  <span v-if="scope.row.boundType && scope.row.boundType === 'nat'">
                    <span v-if="scope.row.boundPhaseStatus == 7">
                      <span v-if="scope.row.eip">
                        <el-tag :size="mainStoreData.viewSize.tagStatus">{{ $t('compute.vmGroup.form.nat') }}</el-tag>
                        {{ scope.row.eip }}
                      </span>
                      <span v-else>-</span>
                    </span>
                    <el-tag
                      v-else
                      :size="mainStoreData.viewSize.tagStatus"
                      :type="filtersFun.getNatStatus(scope.row.boundPhaseStatus, 'tag')"
                      >{{ filtersFun.getNatStatus(scope.row.boundPhaseStatus, 'status') }}</el-tag
                    >
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="phaseStatus" :label="$t('common.status')">
                <template #default="scope">
                  <el-tag
                    :size="mainStoreData.viewSize.tagStatus"
                    :type="filtersFun.getVmStatus(scope.row.phaseStatus, 'tag')"
                    >{{ filtersFun.getVmStatus(scope.row.phaseStatus, 'status') }}</el-tag
                  >
                </template>
              </el-table-column>
              <el-table-column prop="imageInfo.name" :label="$t('compute.vmGroup.form.system')" />
              <el-table-column prop="createTime" :label="$t('common.createTime')" />
            </el-table>
          </el-form-item>
        </div>
      </el-card>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import mainApi from '@/api/modules/main';
import mainStore from '@/store/mainStore';
import filtersFun from '@/utils/statusFun';

const { drawerData } = defineProps<{
  drawerData: any;
}>();
const { proxy }: any = getCurrentInstance();

const mainStoreData = mainStore(); // pinia 信息
const router = useRouter();
const loading = ref(false);
const addVmForm = ref<any>();
const tableData: any = ref([]);
const vpcList: any = ref([]); // 虚拟私有云列表
const subnetsDataList: any = ref([]); // 子网列表
const vpcId = ref(''); // 虚拟私有云id
const subnetId = ref(''); // 子网id

const form = reactive({
  // 表单
  name: '',
  instanceGroupId: '',
  description: '',
});

const rules = reactive({
  name: [{ required: true, validator: proxy.$scriptMain.validateName, trigger: 'change' }],
});

const goBack = () => {
  router.push('/vmGroup');
};

const vmTableData: any = ref([]); // 已选虚拟机列表
const nowCheckVm: any = ref([]); // 已选虚拟机列表
const vmTableList: any = ref([]); // 虚拟机列表
const dialogVm = ref(false); // 虚拟机弹窗
const vmLoading = ref(false); // 虚拟机弹窗loading
const vmForm = reactive({
  // 搜索 筛选
  name: '',
  page_num: 1,
  page_size: 10,
  total: 0,
});

const getDetail = () => {
  // 获取详情
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  mainApi
    .vmInstanceGroupsDetail(id)
    .then((res: any) => {
      form.name = res.name;
      form.instanceGroupId = res.instanceGroupId;
      form.description = res.description;
      getVmsInstabcesDetail(res.instanceGroupId);
    })
    .catch((error: any) => {});
};
const getVmsInstabcesDetail = (id: any) => {
  mainApi
    .vmsInstabcesList({ instance_group_id: id })
    .then((res: any) => {
      vmTableData.value = res.vmInstancesInfo;
    })
    .catch((error: any) => {});
};
onMounted(() => {
  // 详情
  getDetail();
});
</script>

<style lang="scss" scoped>
.snapsAddPage {
}
</style>

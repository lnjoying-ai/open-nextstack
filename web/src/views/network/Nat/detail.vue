<template>
  <div class="bmsAddPage h-full">
    <h5 v-if="!(drawerData && drawerData.isDrawer)" class="bg-white mb-3 px-5 pt-2 pb-2 rounded-md">
      <el-page-header :title="$t('common.backToList')" @back="goBack">
        <template #content> {{ $route.meta.title }} </template>
      </el-page-header>
    </h5>
    <el-form
      v-loading="loading"
      :size="mainStoreData.viewSize.main"
      :model="form"
      label-width="120px"
      :element-loading-text="$t('common.loading')"
    >
      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('network.nat.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('network.nat.form.name') + ':'">
            <span>{{ form.mapName || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('network.nat.form.id') + ':'">
            <span>{{ form.eipMapId || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('network.nat.form.createTime') + ':'">
            <span>{{ form.createTime || '-' }}</span>
          </el-form-item>
        </div>
      </el-card>

      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('network.nat.networkConfig') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('network.nat.form.vpc') + ':'">
            <span>
              <router-link :to="'/vpc/' + form.vpcId" target="_blank" class="text-blue-400">{{
                form.vpcName
              }}</router-link>
            </span>
          </el-form-item>
          <el-form-item :label="$t('network.nat.form.subnet') + ':'">
            <span>
              <router-link :to="'/subnet/' + form.subnetId" target="_blank" class="text-blue-400">{{
                form.subnetName
              }}</router-link>
            </span>
          </el-form-item>
          <el-form-item :label="$t('network.nat.form.subnetCidr') + ':'">
            <span>{{ form.subnetCidr || '-' }}</span>
          </el-form-item>
        </div>
      </el-card>

      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('network.nat.form.server') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('network.nat.form.serverType') + ':'">
            <span>{{ form.vm ? $t('network.nat.form.vm') : $t('network.nat.form.bms') }}</span>
          </el-form-item>
          <el-form-item :label="$t('network.nat.form.server') + ':'">
            <span>
              <router-link v-if="form.vm" target="_blank" :to="'/vm/' + form.instanceId" class="text-blue-400">{{
                form.instanceName
              }}</router-link>
              <router-link v-else target="_blank" :to="'/bms/' + form.instanceId" class="text-blue-400">{{
                form.instanceName
              }}</router-link>
            </span>
          </el-form-item>
          <el-form-item :label="$t('network.nat.form.ip') + ':'">
            <span>{{ form.insideIp || '-' }}</span>
          </el-form-item>

          <el-form-item :label="$t('network.nat.form.protocolType') + ':'">
            <span>{{ form.oneToOne ? $t('network.nat.form.ip') : $t('network.nat.form.tcpUdp') }}</span>
          </el-form-item>
          <div>
            <el-form-item :label="$t('network.nat.form.eipAddress') + ':'">
              <span>{{ form.eipAddress || '-' }}</span>
            </el-form-item>
          </div>
          <div v-if="!form.oneToOne">
            <el-form-item label="">
              <el-table :data="form.portMaps" style="width: 100%">
                <el-table-column type="index" />
                <el-table-column prop="protocol" :label="$t('network.nat.form.protocol')" width="180">
                  <template #default="scope">
                    <span v-if="scope.row.protocol === 0">{{ $t('network.nat.form.tcp') }}</span>
                    <span v-if="scope.row.protocol === 1">{{ $t('network.nat.form.udp') }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="localPort" :label="$t('network.nat.form.serverPort')" width="180" />
                <el-table-column prop="globalPort" :label="$t('network.nat.form.eipPort')" width="180" />
              </el-table>
            </el-form-item>
          </div>
        </div>
      </el-card>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import mainStore from '@/store/mainStore'; // pinia 信息
import mainApi from '@/api/modules/main';

const mainStoreData = mainStore();

const { drawerData } = defineProps<{
  drawerData: any;
}>();

const loading = ref(false);

const router = useRouter();
const form: any = ref({});

const goBack = () => {
  router.push('/Nat');
};
const getDetail = () => {
  loading.value = true;
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  // 获取详情
  mainApi
    .portMapDetail(id)
    .then((res: any) => {
      form.value = res;
      loading.value = false;
    })
    .catch((error: any) => {
      loading.value = false;
    });
};
onMounted(() => {
  getDetail(); // 获取详情
});
</script>

<style lang="scss" scoped>
.bmsAddPage {
}
</style>

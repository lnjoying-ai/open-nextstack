<template>
  <el-row>
    <el-col>
      <el-result icon="warning" :title="$t('exception.404.title')" :sub-title="$t('exception.404.subtitle')">
        <template #extra>
          <el-button type="primary" @click="goBack">{{ $t('common.back') }}</el-button>
        </template>
      </el-result>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
const router = useRouter();
const goBack = () => {
  router.push('/');
};
</script>

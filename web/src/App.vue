<script setup lang="ts">
import zhCn from 'element-plus/lib/locale/lang/zh-cn';
import en from 'element-plus/lib/locale/lang/en';
import { useI18n } from 'vue-i18n';
import mainApi from '@/api/modules/main';
import viewaside from '@/components/layout/aside.vue';
import viewheader from '@/components/layout/header.vue';
import viewfooter from '@/components/layout/footer.vue';

const { locale } = useI18n();

const currentLocale = computed(() => {
  return locale.value === 'zh-CN' ? zhCn : en;
});
</script>

<template>
  <div class="appMain bg-gray-100">
    <el-config-provider :locale="currentLocale">
      <el-container v-if="$route.meta.layout">
        <el-aside class="p-0 overflow-hidden max-w-200px w-auto">
          <viewaside></viewaside>
        </el-aside>

        <el-container style="height: calc(100vh - 0px)">
          <el-header class="h-45px">
            <viewheader></viewheader>
          </el-header>
          <el-container class="block">
            <router-view v-if="$route.name == 'index' || $route.name == 'statistics'"></router-view>
            <el-main v-else>
              <div class="mainStyle">
                <router-view></router-view>
              </div>
            </el-main>
            <!-- <el-footer>
              <viewfooter></viewfooter>
            </el-footer> -->
          </el-container>
        </el-container>
      </el-container>

      <router-view v-else></router-view>
    </el-config-provider>
  </div>
</template>

<style lang="scss" scoped>
.appMain {
  height: 100vh;
}

.el-header {
  padding: 0;
}

.el-footer {
  padding: 0;
}

.el-main {
  padding: 15px;
  height: calc(100vh - 40px);
  flex: none;

  .mainStyle {
    height: calc(100vh - 70px);
    border-radius: 10px;
    padding: 0;
    overflow: auto;
  }
}
</style>

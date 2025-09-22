<template>
  <div class="asidePage">
    <div class="asideMenu">
      <div class="logo" :style="mainStoreData.mainStatus ? 'width:44px;height:50px' : 'width:200px;height:110px'">
        <router-link to="/">
          <img
            src="@/assets/img/menu/menu-logo.png"
            alt=""
            :style="mainStoreData.mainStatus ? 'width:150%' : 'width:50%'"
          />
        </router-link>
      </div>
      <el-menu
        class="el-menu-vertical-demo pt-5 pb-80px"
        active-text-color="#096DD9"
        text-color="#fff"
        :default-active="$route.path"
        :collapse="mainStoreData.mainStatus"
        router
        @open="handleOpen"
        @close="handleClose"
      >
        <template v-for="(item, index) in routeData" :key="index">
          <el-sub-menu v-if="item.children && item.meta.hidden" :index="item.path">
            <template #title>
              <img :src="imgList[item.meta.img]" class="w-4 mr-2 menuIcon" alt="" />
              <img :src="imgList[item.meta.img + '0']" class="w-4 mr-2 menuIconActive" alt="" />
              <span>{{ $t(item.meta.title) }}</span>
            </template>
            <template v-for="(item2, index2) in item.children" :key="index2">
              <el-menu-item v-if="item2.meta.hidden" :index="item2.path">
                <img :src="imgList[item2.meta.img]" alt="" class="menuIcon" />
                <img :src="imgList[item2.meta.img + '0']" class="menuIconActive" alt="" />
                <template #title>
                  <span>{{ $t(item2.meta.title) }}</span>
                </template>
              </el-menu-item>
            </template>
          </el-sub-menu>

          <el-menu-item v-if="!item.children && item.meta.hidden" :index="item.path">
            <img :src="imgList[item.meta.img]" class="w-4 mr-2 menuIcon" alt="" />
            <img :src="imgList[item.meta.img + '0']" class="w-4 mr-2 menuIconActive" alt="" />
            <template #title>
              <span>{{ $t(item.meta.title) }}</span>
            </template>
          </el-menu-item>
        </template>
      </el-menu>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Icon } from '@iconify/vue';
import viewfooter from '@/components/layout/footer.vue';
import router from '@/router';
import mainStore from '@/store/mainStore';

// 导入菜单图标
import home from '@/assets/img/menu/home.png';
import home0 from '@/assets/img/menu/home0.png';
import statistics from '@/assets/img/menu/statistics.png';
import statistics0 from '@/assets/img/menu/statistics0.png';
import compute from '@/assets/img/menu/compute.png';
import compute0 from '@/assets/img/menu/compute0.png';
import bms from '@/assets/img/menu/bms.png';
import bms0 from '@/assets/img/menu/bms0.png';
import vm from '@/assets/img/menu/vm.png';
import vm0 from '@/assets/img/menu/vm0.png';
import vmGroup from '@/assets/img/menu/vmGroup.png';
import vmGroup0 from '@/assets/img/menu/vmGroup0.png';
import node from '@/assets/img/menu/node.png';
import node0 from '@/assets/img/menu/node0.png';
import flavors from '@/assets/img/menu/flavors.png';
import flavors0 from '@/assets/img/menu/flavors0.png';
import snaps from '@/assets/img/menu/snaps.png';
import snaps0 from '@/assets/img/menu/snaps0.png';
import network from '@/assets/img/menu/network.png';
import network0 from '@/assets/img/menu/network0.png';
import vpc from '@/assets/img/menu/vpc.png';
import vpc0 from '@/assets/img/menu/vpc0.png';
import subnet from '@/assets/img/menu/subnet.png';
import subnet0 from '@/assets/img/menu/subnet0.png';
import secGroups from '@/assets/img/menu/secGroups.png';
import secGroups0 from '@/assets/img/menu/secGroups0.png';
import nat from '@/assets/img/menu/nat.png';
import nat0 from '@/assets/img/menu/nat0.png';
import eip from '@/assets/img/menu/eip.png';
import eip0 from '@/assets/img/menu/eip0.png';
import eipPool from '@/assets/img/menu/eipPool.png';
import eipPool0 from '@/assets/img/menu/eipPool0.png';
import publicKey from '@/assets/img/menu/publicKey.png';
import publicKey0 from '@/assets/img/menu/publicKey0.png';
import dataBase from '@/assets/img/menu/dataBase.png';
import dataBase0 from '@/assets/img/menu/dataBase0.png';
import hardware from '@/assets/img/menu/hardware.png';
import hardware0 from '@/assets/img/menu/hardware0.png';
import serve from '@/assets/img/menu/serve.png';
import serve0 from '@/assets/img/menu/serve0.png';
import storage from '@/assets/img/menu/storage.png';
import storage0 from '@/assets/img/menu/storage0.png';
import storagePool from '@/assets/img/menu/storagePool.png';
import storagePool0 from '@/assets/img/menu/storagePool0.png';
import volume from '@/assets/img/menu/volume.png';
import volume0 from '@/assets/img/menu/volume0.png';
import volumeSnap from '@/assets/img/menu/volumeSnap.png';
import volumeSnap0 from '@/assets/img/menu/volumeSnap0.png';
import images from '@/assets/img/menu/images.png';
import images0 from '@/assets/img/menu/images0.png';
import loadBalancing from '@/assets/img/menu/loadBalancing.png';
import loadBalancing0 from '@/assets/img/menu/loadBalancing0.png';
import alert from '@/assets/img/menu/alert.png';
import alert0 from '@/assets/img/menu/alert0.png';
import log from '@/assets/img/menu/log.png';
import log0 from '@/assets/img/menu/log0.png';
import devOps from '@/assets/img/menu/devOps.png';
import devOps0 from '@/assets/img/menu/devOps0.png';
import event from '@/assets/img/menu/event.png';
import event0 from '@/assets/img/menu/event0.png';

// 菜单图标映射
const imgList: any = {
  home,
  home0,
  statistics,
  statistics0,
  compute,
  compute0,
  bms,
  bms0,
  vm,
  vm0,
  vmGroup,
  vmGroup0,
  node,
  node0,
  flavors,
  flavors0,
  snaps,
  snaps0,
  network,
  network0,
  vpc,
  vpc0,
  subnet,
  subnet0,
  secGroups,
  secGroups0,
  nat,
  nat0,
  eip,
  eip0,
  eipPool,
  eipPool0,
  publicKey,
  publicKey0,
  dataBase,
  dataBase0,
  hardware,
  hardware0,
  serve,
  serve0,
  storage,
  storage0,
  storagePool,
  storagePool0,
  volume,
  volume0,
  volumeSnap,
  volumeSnap0,
  images,
  images0,
  loadBalancing,
  loadBalancing0,
  alert,
  alert0,
  log,
  log0,
  devOps,
  devOps0,
  event,
  event0,
};

// pinia store
const mainStoreData = mainStore();

// 路由信息
const routeData: any = router.options.routes;

const handleOpen = (key: string, keyPath: string[]) => {
  console.log(key, keyPath);
};

const handleClose = (key: string, keyPath: string[]) => {
  console.log(key, keyPath);
};
</script>

<style lang="scss" scpoed>
.asidePage {
  position: relative;
  background-color: var(--layoutBgColor);
  height: calc(100vh - 0px);

  .asideMenu {
    overflow: auto;
    height: calc(100vh - 0px);

    &::-webkit-scrollbar {
      width: 0;
      height: 0;
    }
  }

  .logo {
    position: relative;
    margin: 0 auto;
    overflow: hidden;
    text-align: center;
    transition: all 0.3s;

    img {
      position: absolute;
      top: 15px;
      left: 50%;
      max-width: 150%;
      display: inline-block;
      transition: all 0.3s;
      transform: translateX(-50%);
    }
  }

  .el-menu {
    background-color: #314256;
    border-right: 1px solid #314256;
    color: #fff;
    font-size: 14px;
    line-height: 40px;
    text-align: center;

    .el-sub-menu {
      .el-sub-menu__title {
        .menuIconActive {
          display: none;
        }

        .menuIcon {
          display: block;
        }
      }
    }

    .el-menu-item {
      font-size: 14px;
      transition: all 0s;

      .el-tooltip__trigger {
        vertical-align: middle;
      }

      .menuIconActive {
        display: none;
      }

      .menuIcon {
        vertical-align: middle;
        display: inline-block;
      }

      & > img {
        position: relative;
        width: 15px;
        margin-right: 8px;
        z-index: 2;
      }

      & > span {
        position: relative;
        z-index: 2;
      }

      &:hover,
      &.is-active {
        position: relative;
        background-color: #314256;
        color: #096dd9;

        .menuIconActive {
          position: relative;
          z-index: 2;
          vertical-align: middle;
          display: inline-block;
        }

        .menuIcon {
          display: none;
        }

        &::after {
          content: '';
          position: absolute;
          top: 50%;
          right: 0;
          transform: translateY(-50%);
          border-top-left-radius: 40px;
          border-bottom-left-radius: 40px;
          width: 95%;
          height: 80%;
          background-color: #263444;
          color: #096dd9;

          .el-icon {
            color: #096dd9;
          }
        }
      }
    }
  }
}

.el-popper {
  .el-menu-item {
    font-size: 14px;
    transition: all 0s;

    .menuIconActive {
      display: none;
    }

    .menuIcon {
      display: block;
    }

    & > img {
      position: relative;
      width: 15px;
      margin-right: 8px;
      z-index: 2;
    }

    & > span {
      position: relative;
      z-index: 2;
    }

    &:hover,
    &.is-active {
      position: relative;
      background-color: transparent !important;
      color: #096dd9;

      .menuIconActive {
        display: block;
      }

      .menuIcon {
        display: none;
      }

      &::after {
        content: '';
        position: absolute;
        top: 50%;
        right: 0;
        transform: translateY(-50%);
        border-top-left-radius: 40px;
        border-bottom-left-radius: 40px;
        width: 95%;
        height: 80%;
        background-color: #263444;
        color: #096dd9;

        .el-icon {
          color: #096dd9;
        }
      }
    }
  }
}
</style>

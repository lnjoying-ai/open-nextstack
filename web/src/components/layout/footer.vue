<template>
  <div class="footerPage">
    <p class="">
      Copyright2022©北京秒如科技有限公司版权所有<a
        href="http://www.beian.miit.gov.cn/"
        target="_blank"
        style="display: block"
        >京ICP备19050439号</a
      >
    </p>
  </div>
</template>

<script setup lang="ts"></script>

<style lang="scss" scpoed>
.footerPage {
  position: relative;
  background-color: var(--layoutBgColor);
  z-index: 3;
  p {
    color: var(--layoutTextColor);
    font-size: 10px;
    line-height: 20px;
    text-align: center;
    margin: 0 -10px;

    padding-top: 8px;
    padding-bottom: 8px;
    transform: scale(0.8);
    a {
      color: var(--layoutTextColor);
      text-decoration: none;
    }
  }
}
</style>

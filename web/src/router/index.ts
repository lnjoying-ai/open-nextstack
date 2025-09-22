import { createRouter, createWebHistory, createWebHashHistory, Router, RouteRecordRaw } from 'vue-router';
import NProgress from 'nprogress';
import exceptionRoutes from '@/router/route.exception';
import asyncRoutes from '@/router/route.async';
import commonRoutes from '@/router/route.common';
import i18n from '@/locales';

const routes: Array<RouteRecordRaw> = [...commonRoutes, ...asyncRoutes, ...exceptionRoutes];

// const router: Router = createRouter({
//   history: createWebHashHistory(import.meta.env.VITE_BASE),
//   routes,
// });
const router: Router = createRouter({
  history: createWebHistory(import.meta.env.VITE_BASE),
  routes,
});

/**
 * @description: 全局路由前置守卫
 */
router.beforeEach((to, from) => {
  document.title = (i18n.global.t(to.meta.title as string) as string) || import.meta.env.VITE_APP_TITLE;

  if (!NProgress.isStarted()) {
    NProgress.start();
  }
});

router.afterEach(() => {
  NProgress.done();
});

export default router;

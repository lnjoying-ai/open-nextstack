// 不需要鉴权的业务路由
import { RouteRecordRaw } from 'vue-router';
import login from '@/views/login.vue';
import register from '@/views/register.vue';

const commonRoutes: Array<RouteRecordRaw> = [
  {
    path: '/login',
    name: 'login',
    meta: {
      title: 'router.common.login',
      icon: '',
      layout: false,
    },
    component: login,
  },
  {
    path: '/register',
    name: 'register',
    meta: {
      title: 'router.common.register',
      icon: '',
      layout: false,
    },
    component: register,
  },
];

export default commonRoutes;

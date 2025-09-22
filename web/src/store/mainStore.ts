import { defineStore } from 'pinia';

const getuserInfo = localStorage.getItem('userInfo');
const userInfo: any = getuserInfo && getuserInfo !== 'undefined' ? JSON.parse(getuserInfo) : '';

const getpageSize = localStorage.getItem('page_size');
const pageSize: any = getpageSize && getpageSize !== 'undefined' ? JSON.parse(getpageSize) : 10;

const getIsAdmin = (userInfo: any) => {
  if (!userInfo.permissions) {
    return false;
  }

  const statusaa: any = userInfo.permissions.map((item: any) => {
    if (item.role === 'ADMIN') {
      return true;
    }
    return false;
  });

  return statusaa.includes(true);
};

const isAdmin: any = userInfo ? getIsAdmin(userInfo) : true;

const mainStore = defineStore({
  id: 'mainStore',
  state: () => {
    return {
      mainStatus: false, // 侧边菜单状态
      userInfo, // 用户信息
      listRefreshTime: 5000,
      page_size: pageSize,
      page_sizes: [5, 10, 20, 30, 40, 50, 100],
      isAdmin, // 管理员权限
      viewSize: {
        main: 'default' as const,
        listSet: 'small' as const,
        tagStatus: 'small' as const,
        tabChange: 'small' as const,
      },
    };
  },
  getters: {},
  actions: {},
});

export default mainStore;

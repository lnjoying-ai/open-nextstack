import { defineStore } from 'pinia';

const theme = defineStore({
  // 唯一标识
  id: 'theme',
  state: () => ({
    themeType: '亮蓝色',
    themeColor: '#2080F0FF',
  }),
  getters: {
    getThemeType: (state) => state.themeType,
    getThemeColor: (state) => state.themeColor,
  },
  actions: {
    setThemeType(type: string) {
      this.themeType = type;
    },
  },
});

export default theme;

import { createI18n } from 'vue-i18n';
import zhCN from './zh-CN.json';
import en from './en.json';

// 获取当前浏览器语言，如 i18n 没有对应的语言，则使用zh-CN
const language = localStorage.getItem('locale') || navigator.language;
localStorage.setItem('locale', language);

const i18n = createI18n({
  legacy: false, // 使用 Composition API 模式
  globalInjection: true, // 全局注册 $t 方法
  locale: language,
  messages: {
    'zh-CN': zhCN,
    en,
  },
});

export default i18n;

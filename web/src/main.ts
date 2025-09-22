// i18n
import i18n from '@/locales';

// vue router
import router from '@/router/index';
// pinia
import store from '@/store';
import App from './App.vue';

// 公共js方法
import scriptMain from './utils/script';

import 'virtual:windi.css';
// Devtools: https://windicss.org/integrations/vite.html#design-in-devtools
import 'virtual:windi-devtools';
import '@/assets/styles/index.scss';

const app: any = createApp(App);
app.config.globalProperties.$scriptMain = scriptMain; // 公共js方法

app.use(router).use(store);
app.use(i18n);
app.mount('#app');

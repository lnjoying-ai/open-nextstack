import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';
import { ElMessage } from 'element-plus';
import Cookies from 'js-cookie';
import showCodeMessage from '@/api/code';
import { formatJsonToUrlParams, instanceObject } from '@/utils/format';
import router from '@/router/index';
import i18n from '@/locales';
const BASE_PREFIX = import.meta.env.VITE_API_BASEURL;

// 创建axios实例
const axiosInstance: AxiosInstance = axios.create({
  baseURL: BASE_PREFIX,
  timeout: 1000 * 30,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
axiosInstance.interceptors.request.use(
  (config: any) => {
    config.headers['x-access-token'] = Cookies.get('Access-Token') || '';
    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  },
);

// 响应拦截器
axiosInstance.interceptors.response.use(
  (response: AxiosResponse) => {
    if (response.data.code) {
      ElMessage.info(JSON.stringify(response.data.message));
    }
    if (response.status === 200 || response.status === 201) {
      return response.data;
    }
    return Promise.reject(response.data);
  },
  (error: AxiosError) => {
    const { response } = error;
    if (response) {
      if (response.status === 401 || response.data.code === 1128) {
        if (router.currentRoute.value.path === '/login') {
          localStorage.clear();
          Cookies.remove('Access-Token');
          if (response.config.url === '/api/ums/v1/auth/password/tokens') {
            ElMessage.warning(showCodeMessage(response));
          }
        } else {
          localStorage.clear();
          Cookies.remove('Access-Token');
          if (router.currentRoute.value.path === '/login') {
            router.push(router.currentRoute.value.fullPath);
          } else {
            const redirectUrl = window.location.hash.replace('#', '');
            router.push(`/login?redirect=${redirectUrl}`);
          }
        }
      } else if (response.data.message.indexOf('Unexpected consumer error, please check logs for details') === -1) {
        ElMessage.warning(showCodeMessage(response));
      }
      return Promise.reject(response.data);
    }
    ElMessage.warning(i18n.global.t('page.networkError'));
    return Promise.reject(error);
  },
);

// HTTP请求方法封装
const service = {
  get: (url: string, data?: object) => axiosInstance.get(url, { params: data }),
  post: (url: string, data?: object) => axiosInstance.post(url, data),
  put: (url: string, data?: object) => axiosInstance.put(url, data),
  patch: (url: string, data?: object) => axiosInstance.patch(url, data),
  delete: (url: string, data?: object) => axiosInstance.delete(url, data),
  upload: (url: string, file: File) =>
    axiosInstance.post(url, file, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),
  download: (url: string, data: instanceObject) => {
    const downloadUrl = `${BASE_PREFIX}/${url}?${formatJsonToUrlParams(data)}`;
    window.location.href = downloadUrl;
  },
};

export default service;

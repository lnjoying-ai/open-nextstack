import service from '@/api/http';

const version = import.meta.env.VITE_API_BASEVERSION;

const loginApi: any = {
  // 用户登录
  login: (params: object) => service.post(`/api/ums/${version}/auth/password/tokens`, params),
  // 用户退出
  logout: (params: object) => service.delete(`/api/ums/${version}/auth/tokens`, params),
};

export default loginApi;

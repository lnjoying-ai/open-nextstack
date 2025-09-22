
import Cookies from 'js-cookie'
// cookie保存的天数

// export const TOKEN_KEY = 'NOW_TOKEN'

export const setToken = (TOKEN_KEY:string,token:string, cookieExpires:any) => {
  Cookies.set(TOKEN_KEY, token, { expires: cookieExpires || 1 })
}

export const getToken = (TOKEN_KEY:string) => {
  const token = Cookies.get(TOKEN_KEY)
  if (token) return token
  else return false
}

export const delToken = (TOKEN_KEY:string) => {
  Cookies.remove(TOKEN_KEY)
}

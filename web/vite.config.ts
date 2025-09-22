import { defineConfig, loadEnv } from 'vite';
import { resolve } from 'path';
import viteCompression from 'vite-plugin-compression';
import presets from './presets/presets';

export default defineConfig((env) => {
  const viteEnv = loadEnv(env.mode, `.env.${env.mode}`);

  return {
    base: viteEnv.VITE_BASE,
    plugins: [
      presets(env),
      viteCompression({
        verbose: true, // 是否在控制台输出日志
        disable: false, // 是否禁用压缩
        threshold: 10240,
        algorithm: 'gzip',
        ext: '.gz',
      }),
    ],
    resolve: {
      alias: {
        '@': resolve(__dirname, './src'), // 把 @ 指向到 src 目录去
      },
    },
    // 服务设置
    server: {
      host: true, // 允许使用IP地址访问
      port: 8081, // 指定开发服务器端口号
      https: true, // 启用HTTPS安全协议
      open: true, // 服务启动时自动在浏览器打开
      cors: true, // 启用CORS跨域资源共享
      strictPort: true, // 端口被占用时直接退出而不是尝试下一个可用端口
      // 接口代理
      proxy: {
        '/api': {
          target: 'https://192.168.8.67',
          secure: false,
          changeOrigin: true,
          rewrite: (path) => path.replace('/api/', '/'),
        },
        '/ws': {
          target: 'ws://192.168.8.67:9085',
          changeOrigin: true,
          ws: true,
          rewrite: (path) => path.replace('/ws/', '/'),
        },
      },
    },
    build: {
      brotliSize: false,
      chunkSizeWarningLimit: 4000,
      terserOptions: {
        compress: {
          drop_console: true,
          drop_debugger: true,
        },
      },
      assetsDir: 'static/assets',
      rollupOptions: {
        output: {
          chunkFileNames: 'static/js/[name]-[hash].js',
          entryFileNames: 'static/js/[name]-[hash].js',
          assetFileNames: 'static/[ext]/[name]-[hash].[ext]',
        },
      },
    },
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: `
          @import "@/assets/styles/variables.scss";
        `,
          javascriptEnabled: true,
        },
      },
    },
  };
});

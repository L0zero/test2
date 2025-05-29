import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import legacy from '@vitejs/plugin-legacy'
import vue2 from '@vitejs/plugin-vue2'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue2(),
    legacy({
      targets: ['ie >= 11'],
      additionalLegacyPolyfills: ['regenerator-runtime/runtime']
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      echarts: 'echarts/dist/echarts.js', // 支持echarts绘图
    }
  },
  server: {
    proxy: {
      '/sonar': {
        target: 'http://8.140.206.102:8000/', // 替换为你的后端 API 地址
        changeOrigin: true, // 是否改变请求源
        //rewrite: (path) => path.replace(/^\/sonar/, '') // 去掉路径中的前缀
      },
      '/api': {
        target: 'http://8.140.206.102:8000/',
        changeOrigin: true, // 是否改变请求源
      }
    }
  },
  preview: {
    host: '0.0.0.0',
    port: 8000,
  }
})

import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'NeoTab',
  description: 'Minecraft 1.21.1 NeoForge TAB 列表增强模组文档',
  base: '/NeoTab/',

  head: [
    ['link', { rel: 'icon', href: '/NeoTab/favicon.ico' }],
  ],

  themeConfig: {
    logo: '/logo.png',
    siteTitle: 'NeoTab',

    nav: [
      { text: '首页', link: '/' },
      { text: '快速开始', link: '/guide/quick-start' },
      { text: '功能', link: '/features/rich-text' },
      { text: '开发者', link: '/developer/title-api' },
      { text: '更新日志', link: '/changelog' },
    ],

    sidebar: [
      {
        text: '指南',
        items: [
          { text: '介绍', link: '/guide/introduction' },
          { text: '快速开始', link: '/guide/quick-start' },
          { text: '安装', link: '/guide/installation' },
        ],
      },
      {
        text: '功能',
        items: [
          { text: '富文本标签', link: '/features/rich-text' },
          { text: '渐变颜色', link: '/features/gradient' },
          { text: '占位符', link: '/features/placeholders' },
          { text: '配置界面', link: '/features/config-ui' },
        ],
      },
      {
        text: '开发者',
        items: [
          { text: '称号系统 API', link: '/developer/title-api' },
          { text: '多版本管理', link: '/developer/multi-version' },
          { text: '性能优化', link: '/developer/performance' },
        ],
      },
      {
        text: '其他',
        items: [
          { text: '更新日志', link: '/changelog' },
        ],
      },
    ],

    socialLinks: [
      { icon: 'github', link: 'https://github.com/banxxx/NeoTab' },
    ],

    footer: {
      message: 'Released under the All Rights Reserved License.',
      copyright: 'Copyright © 2026 NeoTab',
    },

    search: {
      provider: 'local',
    },

    outline: {
      label: '本页目录',
      level: [2, 3],
    },

    docFooter: {
      prev: '上一页',
      next: '下一页',
    },

    lastUpdated: {
      text: '最后更新于',
    },
  },

  lastUpdated: true,
})

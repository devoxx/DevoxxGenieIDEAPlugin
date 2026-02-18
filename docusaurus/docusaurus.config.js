// @ts-check
// `@type` JSDoc annotations allow editor autocompletion and type checking
// (when paired with `@ts-check`).
// There are various equivalent ways to declare your Docusaurus config.
// See: https://docusaurus.io/docs/api/docusaurus-config

import {themes as prismThemes} from 'prism-react-renderer';

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'DevoxxGenie',
  tagline: 'A fully Java-based LLM Code Assistant plugin for IntelliJ IDEA',
  favicon: 'img/favicon.ico',
  customFields: {
    description: 'Enhance your Java development with AI assistance. DevoxxGenie is a free, open-source plugin that brings local and cloud LLM capabilities directly to your IntelliJ IDEA environment using your own API keys (BYOK).',
    noIndex: false  // Allows search engines to index your site
  },

  // Set the production url of your site here
  url: 'https://genie.devoxx.com',
  // Set the /<baseUrl>/ pathname under which your site is served
  // For GitHub pages deployment, it is often '/<projectName>/'
  // For custom domain, use '/'
  baseUrl: '/',

  // Trailing slash behavior for canonical URLs
  trailingSlash: false,

  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  organizationName: 'devoxx', // Usually your GitHub org/user name.
  projectName: 'DevoxxGenieIDEAPlugin', // Usually your repo name.

  onBrokenLinks: 'throw',

  markdown: {
    hooks: {
      onBrokenMarkdownLinks: 'warn',
    },
  },

  // Even if you don't use internationalization, you can use this field to set
  // useful metadata like html lang. For example, if your site is Chinese, you
  // may want to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  plugins: [],

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: './sidebars.js',
          // Please change this to your repo.
          // Remove this to remove the "edit this page" links.
          editUrl:
            'https://github.com/devoxx/DevoxxGenieIDEAPlugin/tree/master/docusaurus/',
        },
        blog: {
          showReadingTime: true,
          blogTitle: 'DevoxxGenie Blog - AI & IntelliJ IDEA Development',
          blogDescription: 'News, tutorials, and updates on DevoxxGenie — the free, open-source AI code assistant plugin for IntelliJ IDEA. Learn about agent mode, MCP, spec-driven development, and more.',
          editUrl:
            'https://github.com/devoxx/DevoxxGenieIDEAPlugin/tree/master/docusaurus/',
          feedOptions: {
            type: 'all',
            title: 'DevoxxGenie Blog',
            description: 'Stay up to date with the latest DevoxxGenie news and features',
            copyright: `Copyright © ${new Date().getFullYear()} Devoxx`,
          },
        },
        theme: {
          customCss: './src/css/custom.css',
        },
        sitemap: {
          lastmod: 'date',
          changefreq: 'weekly',
          priority: 0.5,
          ignorePatterns: ['/tags/**', '/blog/tags/**', '/blog/archive', '/blog/authors', '/docs/category/**'],
          createSitemapItems: async (params) => {
            const {defaultCreateSitemapItems, ...rest} = params;
            const items = await defaultCreateSitemapItems(rest);
            return items.map((item) => {
              // Homepage — highest priority
              if (item.url === 'https://genie.devoxx.com/' || item.url === 'https://genie.devoxx.com') {
                return {...item, priority: 1.0, changefreq: 'daily'};
              }
              // Blog index — set lastmod to today so Google knows it's fresh
              if (item.url === 'https://genie.devoxx.com/blog') {
                return {...item, priority: 0.8, changefreq: 'weekly', lastmod: new Date().toISOString().split('T')[0]};
              }
              // Getting started & installation — high priority
              if (item.url.includes('/docs/intro') || item.url.includes('/docs/getting-started/')) {
                return {...item, priority: 0.9, changefreq: 'weekly'};
              }
              // Feature pages — high priority
              if (item.url.includes('/docs/features/')) {
                return {...item, priority: 0.8, changefreq: 'weekly'};
              }
              // LLM providers — medium-high priority
              if (item.url.includes('/docs/llm-providers/')) {
                return {...item, priority: 0.7, changefreq: 'weekly'};
              }
              // Blog posts — medium-high priority
              if (item.url.includes('/blog/') && !item.url.includes('/blog/tags') && !item.url.includes('/blog/archive')) {
                return {...item, priority: 0.7, changefreq: 'monthly'};
              }
              // Configuration pages — medium priority
              if (item.url.includes('/docs/configuration/')) {
                return {...item, priority: 0.6, changefreq: 'monthly'};
              }
              // Category index pages — lower priority
              if (item.url.includes('/docs/category/')) {
                return {...item, priority: 0.4, changefreq: 'monthly'};
              }
              // Contributing pages — lower priority
              if (item.url.includes('/docs/contributing/')) {
                return {...item, priority: 0.4, changefreq: 'monthly'};
              }
              // Blog infrastructure (archive, tags index) — lowest
              if (item.url.includes('/blog/archive') || item.url.includes('/blog/tags')) {
                return {...item, priority: 0.3, changefreq: 'monthly'};
              }
              return item;
            });
          },
        },
        gtag: {
          trackingID: 'G-HFMW39MG3G',
          anonymizeIP: true,
        },
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      colorMode: {
        defaultMode: 'dark',
        respectPrefersColorScheme: true,
      },
      // SEO and Social cards
      image: 'img/devoxxgenie-social-card.jpg',
      metadata: [
        {name: 'keywords', content: 'java, intellij plugin, llm, code assistant, spec-driven development, sdd, backlog.md, rag, ai coding, local llm, cloud llm, mcp, openai, anthropic, ollama'},
        {name: 'description', content: 'DevoxxGenie is a free, open-source LLM Code Assistant plugin for IntelliJ IDEA. Supports local models (Ollama, LMStudio) and cloud providers (OpenAI, Anthropic, Google). Features include MCP, RAG, web search, and custom skills.'},
        {property: 'og:type', content: 'website'},
        {property: 'og:site_name', content: 'DevoxxGenie'},
        {name: 'twitter:card', content: 'summary_large_image'},
        {name: 'twitter:site', content: '@DevoxxGenie'},
        {name: 'twitter:creator', content: '@DevoxxGenie'},
        {property: 'og:image:alt', content: 'DevoxxGenie - IntelliJ IDEA Code Assistant'},
        {property: 'og:image:width', content: '1200'},
        {property: 'og:image:height', content: '630'},
        {name: 'msvalidate.01', content: 'DBDB8BF0B394F33D60BAB8AEE5DB11B3'},
      ],
      navbar: {
        title: 'DevoxxGenie',
        logo: {
          alt: 'DevoxxGenie Logo',
          src: 'img/genie.svg',
        },
        items: [
          {
            type: 'docSidebar',
            sidebarId: 'docsSidebar',
            position: 'left',
            label: 'Documentation',
          },
          {
            type: 'docSidebar',
            sidebarId: 'integrationsSidebar',
            position: 'left',
            label: 'Integrations',
          },
          {to: '/blog', label: 'Blog', position: 'left'},
          {
            href: 'https://github.com/devoxx/DevoxxGenieIDEAPlugin',
            label: 'GitHub',
            position: 'right',
          },
          {
            href: 'https://plugins.jetbrains.com/plugin/24169-devoxxgenie',
            label: 'JetBrains Marketplace',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Docs',
            items: [
              {
                label: 'Getting Started',
                to: '/docs/getting-started/installation',
              },
              {
                label: 'Features',
                to: '/docs/features/overview',
              },
              {
                label: 'LLM Providers',
                to: '/docs/llm-providers/overview',
              },
            ],
          },
          {
            title: 'Community',
            items: [
              {
                label: 'X (Twitter)',
                href: 'https://x.com/DevoxxGenie',
              },
              {
                label: 'BlueSky',
                href: 'https://bsky.app/profile/devoxxgenie.bsky.social',
              },
              {
                label: 'GitHub Discussions',
                href: 'https://github.com/devoxx/DevoxxGenieIDEAPlugin/discussions',
              },
              {
                label: 'GitHub Issues',
                href: 'https://github.com/devoxx/DevoxxGenieIDEAPlugin/issues',
              },
            ],
          },
          {
            title: 'More',
            items: [
              {
                label: 'Blog',
                to: '/blog',
              },
              {
                label: 'RSS Feed',
                href: 'https://genie.devoxx.com/blog/rss.xml',
              },
              {
                label: 'GitHub',
                href: 'https://github.com/devoxx/DevoxxGenieIDEAPlugin',
              },
              {
                label: 'Devoxx',
                href: 'https://devoxx.com',
              },
            ],
          },
        ],
        copyright: `Copyright © ${new Date().getFullYear()} Devoxx. Built with Docusaurus.`,
      },
      prism: {
        theme: prismThemes.github,
        darkTheme: prismThemes.dracula,
        additionalLanguages: ['java', 'kotlin', 'groovy', 'bash', 'json'],
      },
    }),
};

export default config;

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

  onBrokenLinks: 'warn',
  onBrokenMarkdownLinks: 'warn',

  // Even if you don't use internationalization, you can use this field to set
  // useful metadata like html lang. For example, if your site is Chinese, you
  // may want to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  plugins: [
    // Remove standalone sitemap plugin as it's already included in the classic preset
    // If you need custom sitemap settings, configure it in the preset options instead
    [
      // Using our custom schema plugin instead of the npm package
      require.resolve('./src/plugins/schema-plugin'),
      {
        schemas: [
          {
            '@context': 'https://schema.org',
            '@type': 'SoftwareApplication',
            'name': 'DevoxxGenie',
            'applicationCategory': 'DeveloperApplication',
            'operatingSystem': 'Windows, macOS, Linux',
            'offers': {
              '@type': 'Offer',
              'price': '0',
              'priceCurrency': 'USD'
            },
            'description': 'A fully Java-based LLM Code Assistant plugin for IntelliJ IDEA, designed to integrate with both local and cloud-based LLM providers.',
            'screenshot': 'https://genie.devoxx.com/img/devoxxgenie-social-card.jpg',
            'softwareVersion': '0.9.2',
            'author': {
              '@type': 'Organization',
              'name': 'Devoxx',
              'url': 'https://devoxx.com'
            }
          }
        ]
      }
    ],
  ],

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
          // Please change this to your repo.
          // Remove this to remove the "edit this page" links.
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
          changefreq: 'weekly',
          priority: 0.5,
          ignorePatterns: ['/tags/**'],
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
        {name: 'keywords', content: 'java, intellij plugin, llm, code assistant, spec driven development, sdd, backlog.md, rag, ai coding, local llm, cloud llm, mcp, openai, anthropic, ollama'},
        {name: 'description', content: 'DevoxxGenie is a free, open-source LLM Code Assistant plugin for IntelliJ IDEA. Supports local models (Ollama, LMStudio) and cloud providers (OpenAI, Anthropic, Google). Features include MCP, RAG, web search, and custom skills.'},
        {name: 'og:type', content: 'website'},
        {name: 'og:site_name', content: 'DevoxxGenie'},
        {name: 'twitter:card', content: 'summary_large_image'},
        {name: 'twitter:site', content: '@DevoxxGenie'},
        {name: 'twitter:creator', content: '@DevoxxGenie'},
        {name: 'og:image:alt', content: 'DevoxxGenie - IntelliJ IDEA Code Assistant'},
        {name: 'og:image:width', content: '1200'},
        {name: 'og:image:height', content: '630'},
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

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

  // Set the production url of your site here
  url: 'https://devoxx.github.io',
  // Set the /<baseUrl>/ pathname under which your site is served
  // For GitHub pages deployment, it is often '/<projectName>/'
  baseUrl: '/',

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
        },
        theme: {
          customCss: './src/css/custom.css',
        },
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      // Replace with your project's social card
      image: 'img/devoxxgenie-social-card.jpg',
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
                href: 'https://x.com/home',
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

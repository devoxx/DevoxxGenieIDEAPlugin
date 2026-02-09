/**
 * Creating a sidebar enables you to:
 - create an ordered group of docs
 - render a sidebar for each doc of that group
 - provide next/previous navigation

 The sidebars can be generated from the filesystem, or explicitly defined here.

 Create as many sidebars as you want.
 */

// @ts-check

/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */
const sidebars = {
  docsSidebar: [
    {
      type: 'category',
      label: 'Getting Started',
      items: [
        'getting-started/introduction',
        'getting-started/installation',
        'getting-started/quick-start-local',
        'getting-started/quick-start-cloud',
      ],
    },
    {
      type: 'category',
      label: 'Features',
      items: [
        'features/overview',
        'features/chat-interface',
        'features/mcp_expanded',
        'features/agent-mode',
        'features/skills',
        'features/web-search',
        'features/rag',
        'features/dnd-images',
        'features/project-scanner',
        'features/chat-memory',
        'features/inline-completion',
      ],
    },
    {
      type: 'category',
      label: 'LLM Providers',
      items: [
        'llm-providers/overview',
        'llm-providers/local-models',
        'llm-providers/cloud-models',
        'llm-providers/custom-providers',
      ],
    },
    {
      type: 'category',
      label: 'Configuration',
      items: [
        'configuration/settings',
        'configuration/prompts',
        'configuration/appearance',
        'configuration/token-cost',
        'configuration/devoxxgenie-md',
      ],
    },
    {
      type: 'category',
      label: 'Contributing',
      items: [
        'contributing/how-to-contribute',
        'contributing/prompt-structure',
        'contributing/development',
        'contributing/documentation-standards',
      ],
    },
  ],
};

export default sidebars;

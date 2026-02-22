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
        'getting-started/use-ollama-in-intellij',
        'getting-started/why-devoxxgenie',
        'getting-started/faq',
      ],
    },
    {
      type: 'category',
      label: 'Features',
      items: [
        'features/overview',
        'features/agent-mode',
        'features/spec-driven-development',
        'features/sdd-agent-loop',
        'features/chat-interface',
        'features/mcp_expanded',
        'features/skills',
        'features/web-search',
        'features/rag',
        'features/dnd-images',
        'features/project-scanner',
        'features/chat-memory',
        'features/inline-completion',
        'features/security-scanning',
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
        'features/acp-runners',
        'features/cli-runners',
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
  integrationsSidebar: [
    'integrations/overview',
    'integrations/sonarlint',
    'integrations/spotbugs',
  ],
};

export default sidebars;

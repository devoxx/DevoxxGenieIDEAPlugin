import React from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import useBaseUrl from '@docusaurus/useBaseUrl';
import styles from './styles.module.css';

// Import our custom icons
import MultipleLLMIcon from './icons/MultipleLLM';
import MCPSupportIcon from './icons/MCPSupport';
import MCPMarketplaceIcon from './icons/MCPMarketplace';
import SubAgentsIcon from './icons/SubAgents';
import ProjectScannerIcon from './icons/ProjectScanner';
import InlineCompletionIcon from './icons/InlineCompletion';
import WebSearchIcon from './icons/WebSearch';
import DragDropIcon from './icons/DragDrop';
import NaiveRAGIcon from './icons/NaiveRAG';

const FeatureList = [
  {
    title: 'Agent Mode',
    icon: SubAgentsIcon,
    link: '/docs/features/agent-mode',
    description: (
      <>
        Available from v0.9.4. Enable agent mode for autonomous codebase exploration with read-only tools. Use parallel sub-agents to investigate multiple aspects concurrently, each configurable with a different LLM from any provider.
      </>
    ),
  },
  {
    title: 'MCP Support',
    icon: MCPSupportIcon,
    link: '/docs/features/mcp_expanded',
    description: (
      <>
        Model Context Protocol (MCP) support for advanced agent-like capabilities, allowing the LLM to access external tools and services for more comprehensive responses.
      </>
    ),
  },
  {
    title: 'MCP Marketplace',
    icon: MCPMarketplaceIcon,
    link: '/docs/features/mcp_expanded',
    description: (
      <>
        Browse, search, and install MCP servers directly from within IntelliJ IDEA. Discover tools for filesystem, databases, web browsing, and more without leaving your IDE.
      </>
    ),
  },
  {
    title: 'Multiple LLM Providers',
    icon: MultipleLLMIcon,
    link: '/docs/llm-providers/overview',
    description: (
      <>
        Connect to local LLMs like Ollama, LMStudio, and GPT4All, as well as cloud-based providers like OpenAI, Anthropic, Mistral, Groq, Kimi, GLM, Gemini, and more.
      </>
    ),
  },
  {
    title: 'Project Scanner',
    icon: ProjectScannerIcon,
    link: '/docs/features/project-scanner',
    description: (
      <>
        Add source code (full project or by package) to prompt context when using compatible LLM providers for better code-aware responses.
      </>
    ),
  },
  {
    title: 'Inline Code Completion',
    icon: InlineCompletionIcon,
    link: '/docs/features/inline-completion',
    description: (
      <>
        Available from v0.9.5. AI-powered code suggestions as you type using Fill-in-the-Middle (FIM) models via Ollama or LM Studio. Context-aware completions using code before and after your cursor.
      </>
    ),
  },
  {
    title: 'Web Search',
    icon: WebSearchIcon,
    link: '/docs/features/web-search',
    description: (
      <>
        Integrate web search functionality using Google or Tavily to find relevant information for your programming questions.
      </>
    ),
  },
  {
    title: 'Drag & Drop Images',
    icon: DragDropIcon,
    link: '/docs/features/dnd-images',
    description: (
      <>
        Drag and drop images directly into the chat when working with multimodal LLMs for visual context in your conversations.
      </>
    ),
  },
  {
    title: 'Naive RAG',
    icon: NaiveRAGIcon,
    link: '/docs/features/rag',
    description: (
      <>
        Retrieval-Augmented Generation that automatically finds and incorporates relevant code from your project to enhance the LLM's understanding of your codebase.
      </>
    ),
  },
];

function Feature({icon: Icon, title, description, link}) {
  return (
    <div className={clsx('col col--4', styles.feature)}>
      <Link to={useBaseUrl(link)} className={styles.featureLink}>
        <div className={clsx('text--center feature-card', styles.featureCard)}>
          <div className={styles.iconContainer}>
            <Icon className={styles.featureIcon} />
          </div>
          <h3 className={styles.featureTitle}>{title}</h3>
          <p>{description}</p>
          <div className={styles.learnMore}>
            Learn more <span className={styles.arrow}>→</span>
          </div>
        </div>
      </Link>
    </div>
  );
}

export default function HomepageFeatures() {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}

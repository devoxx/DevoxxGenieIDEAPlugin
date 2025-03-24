import React from 'react';
import clsx from 'clsx';
import styles from './styles.module.css';

const FeatureList = [
  {
    title: 'Multiple LLM Providers',
    description: (
      <>
        Connect to local LLMs like Ollama, LMStudio, and GPT4All, as well as cloud-based providers like OpenAI, Anthropic, Mistral, Groq, and more.
      </>
    ),
  },
  {
    title: 'MCP Support',
    description: (
      <>
        Model Context Protocol (MCP) support for advanced agent-like capabilities, allowing the LLM to access external tools and services for more comprehensive responses.
      </>
    ),
  },
  {
    title: 'Chat Memory',
    description: (
      <>
        Your chats are stored locally, allowing you to easily restore them in the future. Set your preferred chat memory size for efficient context management.
      </>
    ),
  },
  {
    title: 'Project Scanner',
    description: (
      <>
        Add source code (full project or by package) to prompt context when using compatible LLM providers for better code-aware responses.
      </>
    ),
  },
  {
    title: 'Token Cost Calculator',
    description: (
      <>
        Calculate the token usage and cost when using Cloud LLM providers to help manage your API usage efficiently.
      </>
    ),
  },
  {
    title: 'Web Search',
    description: (
      <>
        Integrate web search functionality using Google or Tavily to find relevant information for your programming questions.
      </>
    ),
  },
  {
    title: 'Git Diff/Merge',
    description: (
      <>
        Show Git Diff/Merge dialog to review and accept LLM suggestions directly without copying and pasting code.
      </>
    ),
  },
  {
    title: 'Drag & Drop Images',
    description: (
      <>
        Drag and drop images directly into the chat when working with multimodal LLMs for visual context in your conversations.
      </>
    ),
  },
  {
    title: 'Naive RAG',
    description: (
      <>
        Retrieval-Augmented Generation that automatically finds and incorporates relevant code from your project to enhance the LLM's understanding of your codebase.
      </>
    ),
  },
];

function Feature({title, description}) {
  return (
    <div className={clsx('col col--4', styles.feature)}>
      <div className="text--center feature-card">
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
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

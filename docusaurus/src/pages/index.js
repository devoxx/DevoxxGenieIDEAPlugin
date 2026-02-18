import React from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import useBaseUrl from '@docusaurus/useBaseUrl';
import Layout from '@theme/Layout';
import HomepageFeatures from '@site/src/components/HomepageFeatures';

import styles from './index.module.css';

function HomepageHeader() {
  const {siteConfig} = useDocusaurusContext();
  return (
    <header className={styles.heroBanner}>
      <video
        poster={useBaseUrl('/img/devoxxgenie-hero-poster.jpg')}
        autoPlay
        loop
        muted
        playsInline
        preload="none"
        className={styles.heroVideo}>
        <source src={useBaseUrl('/img/DevoxxGenie.webm')} type="video/webm" />
        <source src={useBaseUrl('/img/DevoxxGenie.mp4')} type="video/mp4" />
      </video>
      <div className={styles.heroOverlay}>
        <h1 className="hero__title">{siteConfig.title}</h1>
        <p className="hero__subtitle">{siteConfig.tagline}</p>
        <div className={styles.buttons}>
          <Link
          className="button button--primary button--lg"
          to={useBaseUrl('/docs/getting-started/introduction')}>
          Get Started with DevoxxGenie
          </Link>
        </div>
      </div>
    </header>
  );
}

export default function Home() {
  const {siteConfig} = useDocusaurusContext();
  return (
    <Layout
      title={`${siteConfig.title} - Java-based LLM Assistant for IntelliJ IDEA`}
      description={siteConfig.customFields.description}>
      <HomepageHeader />
      <div style={{background: 'var(--ifm-color-primary)', padding: '0.6rem 1rem', textAlign: 'center', color: '#fff', fontSize: '0.95rem'}}>
        Learn hands-on Agentic Engineering with the founder of DevoxxGenie —{' '}
        <a href="https://stephanjanssen.be?utm_source=DevoxxGenie" target="_blank" rel="noopener noreferrer" style={{color: '#fff', fontWeight: 'bold', textDecoration: 'underline'}}>
          Agentic Engineering Workshop
        </a>
      </div>
      <main>
        <div className="container home-section">
          <div className="row">
            <div className="col col--6">
              <h2>The Power of AI in Your IDE</h2>
              <p>
                DevoxxGenie is a fully Java-based LLM Code Assistant plugin for IntelliJ IDEA, designed to integrate with both local and cloud-based LLM providers.
              </p>
              <p>
                With DevoxxGenie, developers can leverage the power of artificial intelligence to improve code quality, solve problems faster, and learn new concepts, all within their familiar IDE environment.
              </p>
              <p>
                <strong>100% Open Source and Free</strong> - DevoxxGenie is completely open source and free to use, following the BYOK (Bring Your Own Keys) model for LLM API keys.
              </p>
              <div className={styles.buttons}>
                <Link
                  className="button button--primary button--md"
                  to="https://plugins.jetbrains.com/plugin/24169-devoxxgenie">
                  Download from JetBrains Marketplace
                </Link>
              </div>
            </div>
            <div className="col col--6">
              <div className={styles.videoContainer}>
                <a
                  href="https://www.youtube.com/watch?v=t1MOHCfsdvk"
                  target="_blank"
                  rel="noopener noreferrer"
                  style={{display: 'block', position: 'relative', borderRadius: '8px', overflow: 'hidden'}}
                >
                  <img
                    src="https://img.youtube.com/vi/t1MOHCfsdvk/maxresdefault.jpg"
                    alt="DevoxxGenie AI Code Assistant demo video for IntelliJ IDEA"
                    style={{width: '100%', display: 'block', borderRadius: '8px'}}
                  />
                  <div style={{
                    position: 'absolute', top: '50%', left: '50%',
                    transform: 'translate(-50%, -50%)',
                    width: '68px', height: '48px',
                    background: '#ff0000', borderRadius: '12px',
                    display: 'flex', alignItems: 'center', justifyContent: 'center'
                  }}>
                    <svg viewBox="0 0 68 48" width="68" height="48">
                      <polygon points="27,17 27,31 41,24" fill="#fff"/>
                    </svg>
                  </div>
                </a>
              </div>
            </div>
          </div>
        </div>
        <div className="container home-section home-section-alternate">
          <div className="row">
            <div className="col col--12 text--center">
              <h2>Unlock the Full Potential of AI in Your Development Workflow</h2>
            </div>
          </div>
        </div>
        <HomepageFeatures />
        <div className="container home-section">
          <div className="row">
            <div className="col col--6">
              <h2>Spec-driven Development</h2>
              <p>
                Define <strong>what</strong> needs to be built as structured task specs with acceptance criteria, and let the LLM agent figure out <strong>how</strong> to build it. Powered by <a href="https://backlog.md" target="_blank" rel="noopener noreferrer">Backlog.md</a> integration, SDD brings disciplined, traceable AI-assisted development to your IDE.
              </p>
              <p>
                <strong>Backlog Browser:</strong> Browse tasks grouped by status (To Do, In Progress, Done) in a dedicated tool window. Select any task and click "Implement with Agent" to inject the full spec as structured context into the LLM prompt.
              </p>
              <p>
                <strong>17 Built-in Backlog Tools:</strong> Just type "Create a task for..." in the prompt to create structured specs instantly. The agent can also edit, search, and complete tasks, manage documents and milestones — all from natural language. Acceptance criteria are checked off in real-time as the agent works.
              </p>
              <div className={styles.buttons} style={{justifyContent: 'flex-start', marginTop: '10px'}}>
                <Link
                  className="button button--primary button--md"
                  to={useBaseUrl('/docs/features/spec-driven-development')}>
                  Learn More
                </Link>
              </div>
            </div>
            <div className="col col--6">
              <div style={{background: 'var(--ifm-background-surface-color)', borderRadius: '8px', padding: '1.5rem', border: '1px solid var(--ifm-color-emphasis-300)'}}>
                <pre style={{margin: 0, fontSize: '0.85rem', lineHeight: '1.5', overflow: 'auto'}}>
{`---
id: TASK-42
title: Add caching layer
status: To Do
priority: high
milestone: v2.0
---

## Acceptance Criteria

- [ ] Redis client configured
- [ ] GET endpoints cached
- [ ] Cache invalidation on writes
- [ ] Metrics exposed
- [ ] Graceful degradation`}
                </pre>
              </div>
            </div>
          </div>
        </div>
        <div className="container home-section home-section-alternate">
          <div className="row">
            <div className="col col--6">
              <img src={useBaseUrl('/img/mcp-logs.jpg')} alt="MCP feature" className="feature-image" />
            </div>
            <div className="col col--6">
              <h2>Advanced MCP Support</h2>
              <p>
                DevoxxGenie implements Model Context Protocol (MCP) support, which enables advanced agent-like capabilities, allowing the LLM to access external tools and services to provide more comprehensive and accurate responses.
              </p>
              <p>
                <strong>Built-in MCP Marketplace:</strong> Browse, search, and install MCP servers directly from within IntelliJ IDEA. Discover tools for filesystem access, databases, web browsing, and more — all without leaving your IDE.
              </p>
              <p>
                <strong>MCP Debugging:</strong> Built-in debugging panel lets you monitor MCP requests and responses in real-time, making it easy to understand how the LLM interacts with external tools and troubleshoot any issues.
              </p>
              <div className={styles.buttons} style={{justifyContent: 'flex-start', marginTop: '10px'}}>
                <Link
                  className="button button--primary button--md"
                  to={useBaseUrl('/docs/features/mcp_expanded')}>
                  Learn More
                </Link>
              </div>
            </div>
          </div>
        </div>
        <div className="container home-section">
          <div className="row">
            <div className="col col--6">
              <h2>Agent Mode</h2>
              <p>
                Agent Mode enables the LLM to autonomously explore and modify your codebase using built-in tools — reading files, listing directories, searching for patterns, and making targeted edits.
              </p>
              <p>
                <strong>Works with Local Models:</strong> Run Agent Mode entirely on your machine using powerful local models like <code>GLM-4.7-flash</code> via Ollama — no cloud API keys required.
              </p>
              <p>
                <strong>Built-in Tools:</strong> <code>read_file</code>, <code>write_file</code>, <code>edit_file</code>, <code>list_files</code>, <code>search_files</code>, and <code>run_command</code> — all managed by the LLM with safety approvals for write operations.
              </p>
              <div className={styles.buttons} style={{justifyContent: 'flex-start', marginTop: '10px'}}>
                <Link
                  className="button button--primary button--md"
                  to={useBaseUrl('/docs/features/agent-mode')}>
                  Learn More
                </Link>
              </div>
            </div>
            <div className="col col--6">
              <img src={useBaseUrl('/img/agent-mode-top.jpg')} alt="Agent Mode settings" className="feature-image" />
            </div>
          </div>
        </div>
        <div className="container home-section home-section-alternate">
          <div className="row">
            <div className="col col--6">
              <img src={useBaseUrl('/img/agent-mode-parallel.jpg')} alt="Parallel sub-agents configuration" className="feature-image" />
            </div>
            <div className="col col--6">
              <h2>Parallel Sub-Agents</h2>
              <p>
                Spawn multiple read-only AI assistants that concurrently investigate different aspects of your project. Each sub-agent can use a different LLM provider/model and runs with its own isolated context and tool call budget.
              </p>
              <p>
                <strong>Cost Optimization:</strong> Use smaller, faster models for sub-agents (e.g., <code>gpt-4o-mini</code>, <code>gemini-flash</code>) while keeping a powerful model for the main agent coordinator.
              </p>
              <p>
                <strong>Per-Agent Model Overrides:</strong> Dynamically add or remove sub-agent slots (up to 10) and assign a specific model to each one — mix cloud and local providers as needed.
              </p>
              <div className={styles.buttons} style={{justifyContent: 'flex-start', marginTop: '10px'}}>
                <Link
                  className="button button--primary button--md"
                  to={useBaseUrl('/docs/features/agent-mode#parallel-sub-agents')}>
                  Learn More
                </Link>
              </div>
            </div>
          </div>
        </div>
        <div className="container home-section">
          <div className="row">
            <div className="col col--6">
              <h2>CLI Runners</h2>
              <p>
                Execute prompts and spec tasks via external CLI tools like <strong>Claude Code</strong>, <strong>GitHub Copilot</strong>, <strong>OpenAI Codex</strong>, <strong>Google Gemini CLI</strong>, and <strong>Kimi</strong> — directly from the chat interface or the Spec Browser.
              </p>
              <p>
                <strong>Chat Mode:</strong> Select a CLI runner as your provider and chat naturally. DevoxxGenie routes your prompts to the external CLI tool and streams the response back into the conversation.
              </p>
              <p>
                <strong>Spec Task Execution:</strong> Run individual or batch spec tasks through any configured CLI runner. Combined with the Agent Loop, you can delegate entire task backlogs to your preferred coding assistant.
              </p>
              <div className={styles.buttons} style={{justifyContent: 'flex-start', marginTop: '10px', gap: '10px'}}>
                <Link
                  className="button button--primary button--md"
                  to={useBaseUrl('/docs/features/cli-runners')}>
                  CLI Runners
                </Link>
                <Link
                  className="button button--secondary button--md"
                  to={useBaseUrl('/docs/features/acp-runners')}>
                  ACP Runners
                </Link>
              </div>
            </div>
            <div className="col col--6">
              <img src={useBaseUrl('/img/CLI-Runners-Homepage.jpg')} alt="CLI Runners configuration" className="feature-image" />
            </div>
          </div>
        </div>
        <div className="container home-section home-section-alternate">
          <div className="row">
            <div className="col col--6">
              <Link
                to="/blog/devoxxgenie-plugin-integrations"
                style={{display: 'block', position: 'relative', borderRadius: '8px', overflow: 'hidden', boxShadow: '0 4px 8px rgba(0,0,0,0.1)'}}>
                <img
                  src={useBaseUrl('/img/integrations/sonarlint-banner.webp')}
                  alt="DevoxxGenie Plugin Integrations"
                  style={{width: '100%', display: 'block', borderRadius: '8px'}}
                />
              </Link>
            </div>
            <div className="col col--6">
              <h2>Plugin Integration API</h2>
              <p>
                DevoxxGenie exposes a lightweight runtime API that other IntelliJ plugins can use to interact with it — no hard compile-time dependency required. Detect DevoxxGenie via <code>PluginManagerCore</code>, send prompts via reflection, or write structured <code>TASK-*.md</code> files for deferred AI-assisted resolution.
              </p>
              <p>
                <strong>SonarLint DevoxxGenie:</strong> A fork of SonarLint v11.13 that adds three entry points for AI-assisted fixes — Alt+Enter intention action, rule panel button, and batch task creation for the SDD workflow.
              </p>
              <p>
                <strong>SpotBugs DevoxxGenie:</strong> A fork of the JetBrains SpotBugs plugin that sends bug findings directly to DevoxxGenie with full context — pattern ID, category, priority, and ±10 lines of code.
              </p>
              <div className={styles.buttons} style={{justifyContent: 'flex-start', marginTop: '10px', gap: '10px'}}>
                <Link
                  className="button button--primary button--md"
                  to="/blog/devoxxgenie-plugin-integrations">
                  Read the Blog Post
                </Link>
                <Link
                  className="button button--secondary button--md"
                  to={useBaseUrl('/docs/integrations/overview')}>
                  API Reference
                </Link>
              </div>
            </div>
          </div>
        </div>
        <div className="container home-section">
          <div className="row">
            <div className="col col--12 text--center">
              <h2>Start Using DevoxxGenie Today</h2>
              <p>Join over 45,000 developers who are already using DevoxxGenie to improve their productivity.</p>
              <p><em>100% free and open source with no hidden costs - just bring your own API keys (BYOK)!</em></p>
              <div className={styles.buttons} style={{marginTop: '20px'}}>
                <Link
                  className="button button--primary button--lg"
                  to={useBaseUrl('/docs/getting-started/installation')}>
                  Installation Guide
                </Link>
                <Link
                  className="button button--secondary button--lg"
                  to={useBaseUrl('/docs/features/overview')}
                  style={{marginLeft: '10px'}}>
                  Explore Features
                </Link>
              </div>
            </div>
          </div>
        </div>
      </main>
    </Layout>
  );
}

import React from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import HomepageFeatures from '@site/src/components/HomepageFeatures';

import styles from './index.module.css';

function HomepageHeader() {
  const {siteConfig} = useDocusaurusContext();
  return (
    <header className={clsx('hero hero--primary', styles.heroBanner)}>
      <div className="container">
        <img 
          src="/img/genie.svg" 
          alt="DevoxxGenie Logo" 
          style={{width: '128px', height: '128px', marginBottom: '1rem'}} 
        />
        <h1 className="hero__title">{siteConfig.title}</h1>
        <p className="hero__subtitle">{siteConfig.tagline}</p>
        <div className={styles.buttons}>
          <Link
            className="button button--secondary button--lg"
            to="/docs/getting-started/introduction">
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
      description="A fully Java-based LLM Code Assistant plugin for IntelliJ IDEA, designed to integrate with local LLM providers and cloud-based LLM services.">
      <HomepageHeader />
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
              <div className={styles.buttons}>
                <Link
                  className="button button--primary button--md"
                  to="https://plugins.jetbrains.com/plugin/24169-devoxxgenie">
                  Download from JetBrains Marketplace
                </Link>
              </div>
            </div>
            <div className="col col--6">
              <img src="/img/devoxxgenie-demo.gif" alt="DevoxxGenie in action" className="feature-image" />
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
              <img src="/img/mcp-logs.jpg" alt="MCP feature" className="feature-image" />
            </div>
            <div className="col col--6">
              <h2>Advanced MCP Support</h2>
              <p>
                DevoxxGenie implements Model Context Protocol (MCP) support, which enables advanced agent-like capabilities, allowing the LLM to access external tools and services to provide more comprehensive and accurate responses.
              </p>
              <p>
                The MCP feature is a significant enhancement to DevoxxGenie's AI assistant capabilities, enabling developers to leverage specialized tools directly from their LLM conversations and perform complex tasks that go beyond text generation.
              </p>
            </div>
          </div>
        </div>
        <div className="container home-section home-section-alternate">
          <div className="row">
            <div className="col col--12 text--center">
              <h2>Start Using DevoxxGenie Today</h2>
              <p>Join thousands of developers who are already using DevoxxGenie to improve their productivity.</p>
              <div className={styles.buttons} style={{marginTop: '20px'}}>
                <Link
                  className="button button--primary button--lg"
                  to="/docs/getting-started/installation">
                  Installation Guide
                </Link>
                <Link
                  className="button button--secondary button--lg"
                  to="/docs/features/overview"
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

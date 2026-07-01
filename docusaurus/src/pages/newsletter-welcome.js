import React from 'react';
import Link from '@docusaurus/Link';
import useBaseUrl from '@docusaurus/useBaseUrl';
import Layout from '@theme/Layout';
import Head from '@docusaurus/Head';
import styles from './index.module.css';

export default function NewsletterWelcome() {
  return (
    <Layout
      title="Welcome to the DevoxxGenie Newsletter"
      description="Thanks for subscribing to the DevoxxGenie newsletter — you're all set to receive the latest features, releases, and AI coding tips.">
      <Head>
        {/* Confirmation/thank-you page — no need to index it. */}
        <meta name="robots" content="noindex" />
      </Head>
      <main>
        <div className="container home-section text--center">
          <div className="row">
            <div className="col col--8 col--offset-2">
              <div style={{fontSize: '3.5rem', lineHeight: 1, marginBottom: '1rem'}}>🧞</div>
              <h1>You're in! Welcome to the DevoxxGenie Newsletter</h1>
              <p style={{fontSize: '1.15rem'}}>
                Thanks for subscribing. Your subscription is confirmed — you'll now get the
                latest DevoxxGenie features, releases, and AI coding tips delivered straight
                to your inbox.
              </p>
              <p style={{color: 'var(--ifm-color-emphasis-700)'}}>
                While you wait for the first issue, here's a great place to start:
              </p>
              <div className={styles.buttons} style={{marginTop: '1.5rem', gap: '10px', flexWrap: 'wrap'}}>
                <Link
                  className="button button--primary button--lg"
                  to={useBaseUrl('/docs/getting-started/introduction')}>
                  Get Started
                </Link>
                <Link
                  className="button button--secondary button--lg"
                  to="https://plugins.jetbrains.com/plugin/24169-devoxxgenie">
                  Install from JetBrains Marketplace
                </Link>
              </div>
              <div className={styles.buttons} style={{marginTop: '1rem', gap: '18px', flexWrap: 'wrap'}}>
                <Link to={useBaseUrl('/blog')}>Read the Blog</Link>
                <Link to="https://github.com/devoxx/DevoxxGenieIDEAPlugin">Star us on GitHub</Link>
                <Link to={useBaseUrl('/docs/features/overview')}>Explore Features</Link>
              </div>
            </div>
          </div>
        </div>
      </main>
    </Layout>
  );
}

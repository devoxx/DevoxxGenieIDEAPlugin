import React from 'react';
import Layout from '@theme/Layout';
import NewsletterSignup from '@site/src/components/NewsletterSignup';

export default function Newsletter() {
  return (
    <Layout
      title="DevoxxGenie Newsletter"
      description="Subscribe to the DevoxxGenie newsletter for the latest features, releases, and AI coding tips.">
      <main>
        <div className="container home-section text--center">
          <div className="row">
            <div className="col col--8 col--offset-2">
              <h1>DevoxxGenie Newsletter</h1>
              <p style={{fontSize: '1.15rem'}}>
                Get the latest DevoxxGenie features, releases, and AI coding tips
                delivered straight to your inbox.
              </p>
              <NewsletterSignup />
            </div>
          </div>
        </div>
      </main>
    </Layout>
  );
}

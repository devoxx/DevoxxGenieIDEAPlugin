import React from 'react';
import Layout from '@theme/Layout';
import Link from '@docusaurus/Link';

// The forms.gle short link resolves to this; using the long form directly so the
// iframe embed works (embedded=true is only honoured on the docs.google.com URL).
const FORM_ID = '1FAIpQLSejzxYodZDK-hb2JupMRaL4-OaKSt9t4HAkjaGjJq0dLRK_uw';
const FORM_URL = `https://docs.google.com/forms/d/e/${FORM_ID}/viewform`;
const FORM_EMBED_URL = `${FORM_URL}?embedded=true`;

export default function SuccessStories() {
  return (
    <Layout
      title="Share your DevoxxGenie success story"
      description="Tell us how DevoxxGenie helps you and your team ship code. Your story may be featured on the site, the blog or the newsletter.">
      <main>
        <div className="container home-section">
          <div className="row">
            <div className="col col--8 col--offset-2 text--center">
              <h1>Share your DevoxxGenie success story</h1>
              <p style={{fontSize: '1.15rem'}}>
                DevoxxGenie is used by developers and teams all over the world. We would
                love to hear what you are building with it: what you automated, what got
                faster, and which models and features you rely on.
              </p>
              <p>
                Stories may be featured on this site, on the{' '}
                <Link to="/blog">blog</Link> or in the{' '}
                <Link to="/newsletter">newsletter</Link>. Nothing is published without
                your consent, and you decide how you are credited.
              </p>
              <p>
                <Link
                  className="button button--primary button--lg"
                  href={FORM_URL}
                  target="_blank"
                  rel="noopener noreferrer">
                  Open the form in a new tab
                </Link>
              </p>
            </div>
          </div>

          <div className="row">
            <div className="col col--8 col--offset-2">
              <iframe
                title="DevoxxGenie success story form"
                src={FORM_EMBED_URL}
                width="100%"
                height="1200"
                frameBorder="0"
                marginHeight="0"
                marginWidth="0"
                style={{border: 0, maxWidth: '100%'}}>
                Loading…
              </iframe>
            </div>
          </div>
        </div>
      </main>
    </Layout>
  );
}

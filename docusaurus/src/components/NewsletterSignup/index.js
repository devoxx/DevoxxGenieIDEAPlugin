import React, {useEffect, useRef} from 'react';

const mailjetScriptSrc = 'https://app.mailjet.com/pas-nc-embedded-v2.js';
const newsletterFormSrc = 'https://redirect.devoxx.be/wgt/lkvt/0r4x/form?c=4860e3b0';

export default function NewsletterSignup({title = 'Subscribe to the DevoxxGenie newsletter'}) {
  const iframeRef = useRef(null);

  useEffect(() => {
    const iframe = iframeRef.current;

    const resizeIframe = () => {
      if (iframe && typeof window.iFrameResize === 'function') {
        window.iFrameResize({checkOrigin: false}, iframe);
      }
    };

    const handleMessage = (e) => {
      if (!iframe) {
        return;
      }
      if (e.data?.type === 'CAPTCHA_OPEN') iframe.style.minHeight = '650px';
      if (e.data?.type === 'CAPTCHA_CLOSE') iframe.style.minHeight = '0px';
    };

    const existingScript = document.querySelector(`script[src="${mailjetScriptSrc}"]`);
    let script = existingScript;

    if (script) {
      resizeIframe();
    } else {
      script = document.createElement('script');
      script.src = mailjetScriptSrc;
      script.type = 'text/javascript';
      script.async = true;
      script.onload = resizeIframe;
      document.body.appendChild(script);
    }

    window.addEventListener('message', handleMessage);

    return () => {
      window.removeEventListener('message', handleMessage);
    };
  }, []);

  return (
    <div style={{maxWidth: '520px', margin: '1.5rem auto 0', background: '#fff', borderRadius: '8px', padding: '1rem 1.25rem', boxShadow: '0 4px 12px rgba(0,0,0,0.15)'}}>
      <iframe
        ref={iframeRef}
        data-w-type="embedded"
        frameBorder="0"
        scrolling="no"
        marginHeight="0"
        marginWidth="0"
        src={newsletterFormSrc}
        width="100%"
        style={{height: 0}}
        title={title}
      />
    </div>
  );
}

// Makes Algolia Experiences autocomplete results clickable.
//
// The hosted experiences.js widget renders result items (.ais-AutocompleteIndexItem)
// without any link or URL in the DOM, so clicking them does nothing. This client
// module re-queries the same public index for the current input, then navigates to
// the matching record's `url` when a result is clicked.
//
// This is a stopgap. The proper fix is to configure the result URL/click-through in
// the Algolia Experiences dashboard, after which this module can be removed.

import ExecutionEnvironment from '@docusaurus/ExecutionEnvironment';

if (ExecutionEnvironment.canUseDOM) {
  const APP_ID = 'YFQ8A065SY';
  // Search-only key (already public in the experiences.js script tag).
  const API_KEY = '6ae04ce6197d1e96bad9c0c99d56ddac';
  const INDEX = 'genie_devoxx_com_yfq8a065sy_pages';
  const ENDPOINT = `https://${APP_ID}-dsn.algolia.net/1/indexes/${INDEX}/query`;

  const cache = new Map();

  const getInput = () => {
    const c = document.querySelector('#autocomplete');
    return c ? c.querySelector('input') : null;
  };
  const currentQuery = () => {
    const i = getInput();
    return i ? i.value.trim() : '';
  };

  const search = (q) => {
    if (!q) return Promise.resolve([]);
    if (cache.has(q)) return Promise.resolve(cache.get(q));
    return fetch(ENDPOINT, {
      method: 'POST',
      headers: {
        'X-Algolia-Application-Id': APP_ID,
        'X-Algolia-API-Key': API_KEY,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({query: q, hitsPerPage: 20}),
    })
      .then((r) => r.json())
      .then((d) => {
        const hits = (d && d.hits) || [];
        cache.set(q, hits);
        return hits;
      })
      .catch(() => []);
  };

  // The rendered item id ends with the hit's position, e.g. "...pages:0".
  const indexOfItem = (li) => {
    const m = li && li.id && li.id.match(/:(\d+)$/);
    return m ? parseInt(m[1], 10) : -1;
  };

  const navigate = (url) => {
    if (url) window.location.assign(url);
  };

  // Pre-fetch results as the user types so the URL is ready on click.
  document.addEventListener(
    'input',
    (e) => {
      const c = document.querySelector('#autocomplete');
      if (c && c.contains(e.target)) {
        const q = currentQuery();
        if (q) search(q);
      }
    },
    true,
  );

  // Intercept result clicks and navigate to the matching record url.
  document.addEventListener(
    'click',
    (e) => {
      const li =
        e.target.closest && e.target.closest('.ais-AutocompleteIndexItem');
      if (!li || !li.id || li.id.indexOf(INDEX) === -1) return;
      const idx = indexOfItem(li);
      if (idx < 0) return;
      e.preventDefault();
      e.stopPropagation();
      const q = currentQuery();
      const cached = cache.get(q);
      if (cached && cached[idx]) {
        navigate(cached[idx].url);
        return;
      }
      search(q).then((hits) => {
        if (hits[idx]) navigate(hits[idx].url);
      });
    },
    true,
  );
}

// Live DevoxxGenie figures for the landing-page stats band.
//
// Three upstreams, fetched independently so one being down never blanks the others:
//  - plugins.jetbrains.com owns the download counter: `GET /api/plugins/24169`
//    is the Marketplace's own public API and the authoritative source.
//  - devoxx.com owns the GA4 active-user figure (and mirrors the download count,
//    refreshed server-side every 6 hours): `GET /api/genie-stats`.
//  - api.github.com owns the star count for devoxx/DevoxxGenieIDEAPlugin.
// We read them here at BUILD time (Node, so no CORS involved) and bake the
// result into the static HTML via global data -- the numbers are in the markup
// for crawlers and there is no loading flash on first paint.
//
// Refresh cadence is therefore the deploy cadence: .github/workflows/deploy-docs.yml
// runs on a daily cron precisely so this stays current without anyone editing
// src/pages/index.js by hand.

const STATS_URL = 'https://devoxx.com/api/genie-stats';
const MARKETPLACE_PLUGIN_URL = 'https://plugins.jetbrains.com/api/plugins/24169';
const GITHUB_REPO_URL = 'https://api.github.com/repos/devoxx/DevoxxGenieIDEAPlugin';
const TIMEOUT_MS = 5000;

// devoxx.com sits behind a WAF that 403s Node's default `User-Agent: node`
// (browser requests pass, which is why the endpoint looks fine when checked by
// hand). Identify ourselves explicitly instead -- see the fallback warnings in
// the deploy-docs Actions logs from before this header existed.
const USER_AGENT =
  'Mozilla/5.0 (compatible; DevoxxGenieDocsBuild/1.0; +https://genie.devoxx.com)';

/**
 * Used when a fetch fails -- an offline laptop, a CI runner without egress,
 * or an upstream being down must never fail the build or render "0"/"NaN".
 * These were the last figures observed live; refresh them if they ever drift
 * far enough that a fallback render would look wrong.
 */
const FALLBACK = {
  activeUsers: 114000,
  downloads: 83926,
  asOf: 'July 2026',
  live: false,
};

const FALLBACK_GITHUB_STARS = 671;

function warn(message) {
  console.warn(`[genie-stats] ${message}`);
  // A stale stats band is otherwise invisible inside a green build -- surface
  // fallback use as an annotation in the Actions run summary.
  if (process.env.GITHUB_ACTIONS) {
    console.log(`::warning title=genie-stats::${message}`);
  }
}

function formatAsOf(date) {
  // Built once in Node and serialized into global data, so the client never
  // recomputes it -- no locale-dependent hydration mismatch.
  return `${date.toLocaleString('en-US', {month: 'long'})} ${date.getFullYear()}`;
}

/** A positive, finite count -- anything else means the upstream gave us junk. */
function isUsableCount(value) {
  return typeof value === 'number' && Number.isFinite(value) && value > 0;
}

async function fetchJson(url, headers) {
  const res = await fetch(url, {
    headers: {Accept: 'application/json', 'User-Agent': USER_AGENT, ...headers},
    // Node's fetch has no default overall timeout; an unbounded hang here
    // would stall the build.
    signal: AbortSignal.timeout(TIMEOUT_MS),
  });
  if (!res.ok) {
    throw new Error(`responded ${res.status}`);
  }
  return res.json();
}

async function fetchUsageStats() {
  try {
    const body = await fetchJson(STATS_URL);
    if (!isUsableCount(body.activeUsers) || !isUsableCount(body.downloads)) {
      throw new Error('response has no usable activeUsers/downloads');
    }

    return {
      activeUsers: body.activeUsers,
      downloads: body.downloads,
      live: true,
    };
  } catch (err) {
    warn(`${STATS_URL} unavailable (${err.message}); using baked-in fallback figures.`);
    return {activeUsers: FALLBACK.activeUsers, downloads: FALLBACK.downloads, live: false};
  }
}

/** The Marketplace's own counter for the plugin; null when unavailable. */
async function fetchMarketplaceDownloads() {
  try {
    const body = await fetchJson(MARKETPLACE_PLUGIN_URL);
    if (!isUsableCount(body.downloads)) {
      throw new Error('response has no usable downloads');
    }
    return body.downloads;
  } catch (err) {
    warn(`${MARKETPLACE_PLUGIN_URL} unavailable (${err.message}); falling back to devoxx.com's download count.`);
    return null;
  }
}

async function fetchGithubStars() {
  try {
    const headers = {Accept: 'application/vnd.github+json'};
    // Unauthenticated api.github.com is limited to 60 requests/hour per IP,
    // and Actions runners share IPs -- so CI passes its workflow token through.
    // Local builds work fine without one.
    if (process.env.GITHUB_TOKEN) {
      headers.Authorization = `Bearer ${process.env.GITHUB_TOKEN}`;
    }

    const body = await fetchJson(GITHUB_REPO_URL, headers);
    if (!isUsableCount(body.stargazers_count)) {
      throw new Error('response has no usable stargazers_count');
    }

    return body.stargazers_count;
  } catch (err) {
    warn(`${GITHUB_REPO_URL} unavailable (${err.message}); using baked-in fallback star count.`);
    return FALLBACK_GITHUB_STARS;
  }
}

module.exports = function genieStatsPlugin() {
  return {
    name: 'genie-stats',

    async loadContent() {
      const [usage, marketplaceDownloads, githubStars] = await Promise.all([
        fetchUsageStats(),
        fetchMarketplaceDownloads(),
        fetchGithubStars(),
      ]);

      // Marketplace is authoritative for its own counter; devoxx.com's mirror
      // (already in `usage.downloads`) covers a Marketplace outage, and the
      // baked-in FALLBACK covers both being down.
      const downloads = marketplaceDownloads ?? usage.downloads;
      const live = usage.live || marketplaceDownloads !== null;

      return {
        activeUsers: usage.activeUsers,
        downloads,
        githubStars,
        live,
        asOf: live ? formatAsOf(new Date()) : FALLBACK.asOf,
      };
    },

    contentLoaded({content, actions}) {
      actions.setGlobalData(content);
    },
  };
};

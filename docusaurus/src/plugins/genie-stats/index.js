// Live DevoxxGenie figures for the landing-page stats band.
//
// Two upstreams, fetched independently so one being down never blanks the other:
//  - devoxx.com owns the usage numbers: `GET /api/genie-stats` returns cumulative
//    JetBrains Marketplace downloads and GA4 active users, refreshed server-side
//    every 6 hours.
//  - api.github.com owns the star count for devoxx/DevoxxGenieIDEAPlugin.
// We read both here at BUILD time (Node, so no CORS involved) and bake the
// result into the static HTML via global data -- the numbers are in the markup
// for crawlers and there is no loading flash on first paint.
//
// Refresh cadence is therefore the deploy cadence: .github/workflows/deploy-docs.yml
// runs on a daily cron precisely so this stays current without anyone editing
// src/pages/index.js by hand.

const STATS_URL = 'https://devoxx.com/api/genie-stats';
const GITHUB_REPO_URL = 'https://api.github.com/repos/devoxx/DevoxxGenieIDEAPlugin';
const TIMEOUT_MS = 5000;

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

function formatAsOf(date) {
  // Built once in Node and serialized into global data, so the client never
  // recomputes it -- no locale-dependent hydration mismatch.
  return `${date.toLocaleString('en-US', {month: 'long'})} ${date.getFullYear()}`;
}

/** A positive, finite count -- anything else means the upstream gave us junk. */
function isUsableCount(value) {
  return typeof value === 'number' && Number.isFinite(value) && value > 0;
}

async function fetchUsageStats() {
  try {
    const res = await fetch(STATS_URL, {
      headers: {Accept: 'application/json'},
      // Node's fetch has no default overall timeout; an unbounded hang here
      // would stall the build.
      signal: AbortSignal.timeout(TIMEOUT_MS),
    });
    if (!res.ok) {
      throw new Error(`responded ${res.status}`);
    }

    const body = await res.json();
    if (!isUsableCount(body.activeUsers) || !isUsableCount(body.downloads)) {
      throw new Error('response has no usable activeUsers/downloads');
    }

    return {
      activeUsers: body.activeUsers,
      downloads: body.downloads,
      asOf: formatAsOf(new Date()),
      live: true,
    };
  } catch (err) {
    console.warn(
      `[genie-stats] ${STATS_URL} unavailable (${err.message}); using baked-in fallback figures.`,
    );
    return {...FALLBACK};
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

    const res = await fetch(GITHUB_REPO_URL, {
      headers,
      signal: AbortSignal.timeout(TIMEOUT_MS),
    });
    if (!res.ok) {
      throw new Error(`responded ${res.status}`);
    }

    const body = await res.json();
    if (!isUsableCount(body.stargazers_count)) {
      throw new Error('response has no usable stargazers_count');
    }

    return body.stargazers_count;
  } catch (err) {
    console.warn(
      `[genie-stats] ${GITHUB_REPO_URL} unavailable (${err.message}); using baked-in fallback star count.`,
    );
    return FALLBACK_GITHUB_STARS;
  }
}

module.exports = function genieStatsPlugin() {
  return {
    name: 'genie-stats',

    async loadContent() {
      const [usage, githubStars] = await Promise.all([
        fetchUsageStats(),
        fetchGithubStars(),
      ]);
      return {...usage, githubStars};
    },

    contentLoaded({content, actions}) {
      actions.setGlobalData(content);
    },
  };
};

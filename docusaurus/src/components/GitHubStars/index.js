import React, {useState, useEffect} from 'react';
import styles from './styles.module.css';

const REPO = 'devoxx/DevoxxGenieIDEAPlugin';
const CACHE_KEY = 'devoxxgenie_github_stars';
const CACHE_TTL = 3600000; // 1 hour in milliseconds

function formatStars(count) {
  if (count >= 1000) {
    return (count / 1000).toFixed(1).replace(/\.0$/, '') + 'k';
  }
  return count.toLocaleString();
}

export default function GitHubStars() {
  const [stars, setStars] = useState(null);

  useEffect(() => {
    // Check localStorage cache first
    try {
      const cached = JSON.parse(localStorage.getItem(CACHE_KEY));
      if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
        setStars(cached.count);
        return;
      }
    } catch {
      // ignore parse errors
    }

    fetch(`https://api.github.com/repos/${REPO}`)
      .then((res) => {
        if (!res.ok) throw new Error('GitHub API error');
        return res.json();
      })
      .then((data) => {
        const count = data.stargazers_count;
        setStars(count);
        try {
          localStorage.setItem(
            CACHE_KEY,
            JSON.stringify({count, timestamp: Date.now()})
          );
        } catch {
          // ignore storage errors
        }
      })
      .catch(() => {
        // On error, show cached value if available (even if expired)
        try {
          const cached = JSON.parse(localStorage.getItem(CACHE_KEY));
          if (cached) setStars(cached.count);
        } catch {
          // ignore
        }
      });
  }, []);

  if (stars === null) return null;

  return (
    <a
      href={`https://github.com/${REPO}`}
      target="_blank"
      rel="noopener noreferrer"
      className={styles.starBadge}
      aria-label={`${formatStars(stars)} stars on GitHub`}
    >
      <svg
        className={styles.starIcon}
        viewBox="0 0 16 16"
        width="16"
        height="16"
        fill="currentColor"
        aria-hidden="true"
      >
        <path d="M8 .25a.75.75 0 0 1 .673.418l1.882 3.815 4.21.612a.75.75 0 0 1 .416 1.279l-3.046 2.97.719 4.192a.75.75 0 0 1-1.088.791L8 12.347l-3.766 1.98a.75.75 0 0 1-1.088-.79l.72-4.194L.818 6.374a.75.75 0 0 1 .416-1.28l4.21-.611L7.327.668A.75.75 0 0 1 8 .25z" />
      </svg>
      <span className={styles.starCount}>{formatStars(stars)}</span>
      <span className={styles.starLabel}>Stars</span>
    </a>
  );
}

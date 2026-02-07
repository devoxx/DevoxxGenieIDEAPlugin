import React from 'react';

export default function MCPMarketplaceIcon({className}) {
  return (
    <svg
      className={className}
      width="48"
      height="48"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      {/* Store/marketplace icon */}
      <path d="M3 9L5 3h14l2 6" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M3 9v10a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V9" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M9 13v6" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
      <path d="M15 13v6" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
      {/* MCP badge/indicator */}
      <circle cx="18" cy="6" r="2.5" fill="currentColor" opacity="0.7" />
    </svg>
  );
}

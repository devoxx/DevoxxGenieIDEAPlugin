import React from 'react';

export default function MCPSupportIcon({className}) {
  return (
    <svg 
      className={className}
      width="48" 
      height="48" 
      viewBox="0 0 24 24" 
      fill="none" 
      xmlns="http://www.w3.org/2000/svg"
    >
      <rect x="3" y="4" width="18" height="16" rx="2" stroke="currentColor" strokeWidth="1.5" />
      <path d="M8 9L16 9" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
      <path d="M8 12L16 12" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
      <path d="M8 15L12 15" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
      <circle cx="17" cy="7" r="3" fill="currentColor" opacity="0.7" />
    </svg>
  );
}

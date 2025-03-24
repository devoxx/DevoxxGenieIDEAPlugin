import React from 'react';

export default function ProjectScannerIcon({className}) {
  return (
    <svg 
      className={className}
      width="48" 
      height="48" 
      viewBox="0 0 24 24" 
      fill="none" 
      xmlns="http://www.w3.org/2000/svg"
    >
      <rect x="3" y="5" width="18" height="14" rx="2" stroke="currentColor" strokeWidth="1.5" />
      <path d="M7 9V15" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
      <path d="M11 9V15" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
      <path d="M15 9V15" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
      <path d="M3 10L21 10" stroke="currentColor" strokeWidth="1" strokeDasharray="1 1" />
      <path d="M3 14L21 14" stroke="currentColor" strokeWidth="1" strokeDasharray="1 1" />
    </svg>
  );
}

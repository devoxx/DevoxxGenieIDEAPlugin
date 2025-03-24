import React from 'react';

export default function GitDiffIcon({className}) {
  return (
    <svg 
      className={className}
      width="48" 
      height="48" 
      viewBox="0 0 24 24" 
      fill="none" 
      xmlns="http://www.w3.org/2000/svg"
    >
      <circle cx="7" cy="6" r="3" stroke="currentColor" strokeWidth="1.5" />
      <circle cx="17" cy="6" r="3" stroke="currentColor" strokeWidth="1.5" />
      <circle cx="7" cy="18" r="3" stroke="currentColor" strokeWidth="1.5" />
      <circle cx="17" cy="18" r="3" stroke="currentColor" strokeWidth="1.5" />
      <path d="M7 9V15" stroke="currentColor" strokeWidth="1.5" />
      <path d="M17 9V15" stroke="currentColor" strokeWidth="1.5" />
      <path d="M10 6H14" stroke="currentColor" strokeWidth="1.5" />
      <path d="M7 18L17 6" stroke="currentColor" strokeWidth="1.5" strokeDasharray="1 1" />
    </svg>
  );
}

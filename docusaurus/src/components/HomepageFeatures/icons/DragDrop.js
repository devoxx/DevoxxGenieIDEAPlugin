import React from 'react';

export default function DragDropIcon({className}) {
  return (
    <svg 
      className={className}
      width="48" 
      height="48" 
      viewBox="0 0 24 24" 
      fill="none" 
      xmlns="http://www.w3.org/2000/svg"
    >
      <rect x="4" y="7" width="12" height="10" rx="2" stroke="currentColor" strokeWidth="1.5" />
      <rect x="6" y="9" width="8" height="6" rx="1" stroke="currentColor" strokeWidth="1.5" />
      <path d="M16 10L20 7V17L16 14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
      <circle cx="8" cy="12" r="1" fill="currentColor" />
      <path d="M9 15L11 11" stroke="currentColor" strokeWidth="1" strokeLinecap="round" />
    </svg>
  );
}

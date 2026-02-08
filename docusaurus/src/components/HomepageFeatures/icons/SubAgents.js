import React from 'react';

export default function SubAgentsIcon({className}) {
  return (
    <svg 
      className={className}
      width="48" 
      height="48" 
      viewBox="0 0 24 24" 
      fill="none" 
      xmlns="http://www.w3.org/2000/svg"
    >
      <circle cx="12" cy="6" r="3" fill="currentColor" opacity="0.9" />
      <circle cx="6" cy="16" r="3" fill="currentColor" opacity="0.8" />
      <circle cx="18" cy="16" r="3" fill="currentColor" opacity="0.8" />
      <path d="M12 9L6 14" stroke="currentColor" strokeWidth="1.5" />
      <path d="M12 9L18 14" stroke="currentColor" strokeWidth="1.5" />
      <path d="M6 19V21" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
      <path d="M18 19V21" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
      <path d="M12 3V1" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  );
}

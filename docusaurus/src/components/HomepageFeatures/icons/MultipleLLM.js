import React from 'react';

export default function MultipleLLMIcon({className}) {
  return (
    <svg 
      className={className}
      width="48" 
      height="48" 
      viewBox="0 0 24 24" 
      fill="none" 
      xmlns="http://www.w3.org/2000/svg"
    >
      <circle cx="6" cy="12" r="4" fill="currentColor" opacity="0.8" />
      <circle cx="12" cy="6" r="4" fill="currentColor" opacity="0.8" />
      <circle cx="12" cy="18" r="4" fill="currentColor" opacity="0.8" />
      <circle cx="18" cy="12" r="4" fill="currentColor" opacity="0.8" />
      <path d="M6 12L12 6M6 12L12 18M12 18L18 12M18 12L12 6" stroke="currentColor" strokeWidth="1.5" />
    </svg>
  );
}

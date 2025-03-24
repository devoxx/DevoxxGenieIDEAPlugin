import React from 'react';

export default function NaiveRAGIcon({className}) {
  return (
    <svg 
      className={className}
      width="48" 
      height="48" 
      viewBox="0 0 24 24" 
      fill="none" 
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M4 6L12 3L20 6L12 9L4 6Z" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" />
      <path d="M4 6V14L12 17L20 14V6" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M12 9V17" stroke="currentColor" strokeWidth="1.5" strokeDasharray="1 1" />
      <path d="M7 7.5L7 12.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
      <path d="M17 7.5V12.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  );
}

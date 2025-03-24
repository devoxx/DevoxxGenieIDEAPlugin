import React from 'react';

export default function ChatMemoryIcon({className}) {
  return (
    <svg 
      className={className}
      width="48" 
      height="48" 
      viewBox="0 0 24 24" 
      fill="none" 
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M7 8H17C17.5523 8 18 8.44772 18 9V16C18 16.5523 17.5523 17 17 17H8.5L5 20V9C5 8.44772 5.44772 8 6 8H7Z" stroke="currentColor" strokeWidth="1.5" />
      <path d="M9 12H14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
      <circle cx="18" cy="7" r="2" fill="currentColor" opacity="0.7" />
      <circle cx="14" cy="4" r="2" fill="currentColor" opacity="0.7" />
      <circle cx="9" cy="4" r="2" fill="currentColor" opacity="0.7" />
      <circle cx="5" cy="7" r="2" fill="currentColor" opacity="0.7" />
    </svg>
  );
}

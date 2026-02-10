import React from 'react';

export default function SpecDrivenDevIcon({className}) {
  return (
    <svg
      className={className}
      width="48"
      height="48"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      {/* Document/spec sheet */}
      <rect x="4" y="2" width="12" height="16" rx="2" stroke="currentColor" strokeWidth="1.5" />
      {/* Checklist lines */}
      <path d="M7 6H13" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
      <path d="M7 9H13" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
      <path d="M7 12H11" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
      {/* Checkmarks */}
      <path d="M5.5 6L6.2 6.7L7.5 5.5" stroke="currentColor" strokeWidth="0.8" strokeLinecap="round" strokeLinejoin="round" opacity="0" />
      {/* Gear/cog representing automation */}
      <circle cx="17" cy="17" r="5" fill="currentColor" opacity="0.15" />
      <path d="M17 13V14.2" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
      <path d="M17 19.8V21" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
      <path d="M13 17H14.2" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
      <path d="M19.8 17H21" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
      <circle cx="17" cy="17" r="2" stroke="currentColor" strokeWidth="1.2" />
      {/* Arrow from spec to gear */}
      <path d="M14 15L15.5 16" stroke="currentColor" strokeWidth="1" strokeLinecap="round" strokeDasharray="2 1" />
    </svg>
  );
}

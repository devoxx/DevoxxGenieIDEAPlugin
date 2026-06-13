import React from 'react';

// Overrides the default Docusaurus navbar SearchBar.
// Renders the #autocomplete container that the Algolia Experiences
// library (loaded via the algolia-experiences plugin) attaches to.
export default function SearchBar() {
  return <div id="autocomplete" className="algolia-experiences-autocomplete" />;
}

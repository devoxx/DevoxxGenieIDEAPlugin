// Algolia Experiences search plugin for Docusaurus.
// Loads the Algolia Experiences library at the end of <body>, which
// attaches the search UI to the #autocomplete container rendered by
// the swizzled navbar SearchBar component (src/theme/SearchBar).
//
// See: https://www.algolia.com/doc/

const DEFAULTS = {
  appId: 'YFQ8A065SY',
  apiKey: '6ae04ce6197d1e96bad9c0c99d56ddac',
  experienceId: 'YFQ8A065SY',
  env: 'prod',
};

module.exports = function algoliaExperiencesPlugin(context, options) {
  const {appId, apiKey, experienceId, env} = {...DEFAULTS, ...options};

  const src =
    'https://cdn.jsdelivr.net/npm/@algolia/experiences/dist/experiences.js' +
    `?appId=${appId}&apiKey=${apiKey}&experienceId=${experienceId}&env=${env}`;

  return {
    name: 'algolia-experiences',

    injectHtmlTags() {
      return {
        postBodyTags: [
          {
            tagName: 'script',
            attributes: {
              src,
            },
          },
        ],
      };
    },
  };
};

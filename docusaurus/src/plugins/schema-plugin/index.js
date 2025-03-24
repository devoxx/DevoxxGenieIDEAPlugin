// Custom schema plugin for Docusaurus
// This plugin injects schema.org metadata into the HTML

module.exports = function(context, options) {
  const {schemas = []} = options || {};
  
  return {
    name: 'custom-schema-plugin',
    
    injectHtmlTags() {
      const schemaScripts = schemas.map(schema => {
        return {
          tagName: 'script',
          attributes: {
            type: 'application/ld+json',
          },
          innerHTML: JSON.stringify(schema),
        };
      });
      
      return {
        headTags: schemaScripts,
      };
    },
  };
};

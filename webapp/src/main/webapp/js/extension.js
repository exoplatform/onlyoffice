(() => {
  const newDocumentTypeExtensionOptions = [
    {
      id: 'docs',
      rank: 10,
      label: 'documents.type.slides',
      extension: '.docx',
      type: 'MicrosoftOfficeDocument',
      icon: 'uiIconFileTypeapplicationvndopenxmlformats-officedocumentwordprocessingmldocument uiIconFileTypeDefault'
    },
    {
      id: 'sheets',
      rank: 20,
      label: 'documents.type.slides',
      extension: '.xlsx',
      type: 'MicrosoftOfficeSpreadsheet',
      icon: 'uiIconFileTypeapplicationvndopenxmlformats-officedocumentspreadsheetmlsheet uiIconFileTypeDefault'
    },
    {
      id: 'slides',
      rank: 30,
      label: 'documents.type.slides',
      extension: '.pptx',
      type: 'MicrosoftOfficePresentation',
      icon: 'uiIconFileTypeapplicationvndopenxmlformats-officedocumentpresentationmlpresentation uiIconFileTypeDefault'
    }
  ];
  const lang = eXo.env.portal.language || 'en';
  const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/locale.navigation.portal.global-${lang}.json`;

  exoi18n.loadLanguageAsync(lang, url).then(i18n => new Vue({i18n}));
  newDocumentTypeExtensionOptions.forEach(extension => extensionRegistry.registerExtension('attachment', 'new-document-action', extension));
  document.dispatchEvent(new CustomEvent('new-document-action-updated'));
})();
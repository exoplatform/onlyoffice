(() => {
  const newDocumentTypeExtensionOptions = [
    {
      id: 'docs',
      rank: 10,
      label: 'documents.type.docs',
      extension: '.docx',
      type: 'MicrosoftOfficeDocument',
      icon: 'fas fa-file-word',
      color: '#2A5699',
    },
    {
      id: 'sheets',
      rank: 20,
      label: 'documents.type.sheets',
      extension: '.xlsx',
      type: 'MicrosoftOfficeSpreadsheet',
      icon: 'fas fa-file-excel',
      color: '#217345',
    },
    {
      id: 'slides',
      rank: 30,
      label: 'documents.type.slides',
      extension: '.pptx',
      type: 'MicrosoftOfficePresentation',
      icon: 'fas fa-file-powerpoint',
      color: '#CB4B32',
    }
  ];
  const lang = eXo.env.portal.language || 'en';
  const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/locale.navigation.portal.global-${lang}.json`;

  exoi18n.loadLanguageAsync(lang, url).then(i18n => new Vue({i18n}));
  newDocumentTypeExtensionOptions.forEach(extension => extensionRegistry.registerExtension('attachment', 'new-document-action', extension));
  document.dispatchEvent(new CustomEvent('attachment-new-document-action-updated'));
})();
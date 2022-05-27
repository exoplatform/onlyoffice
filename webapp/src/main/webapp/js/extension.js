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
  const supportedDocumentsExtensionOptions = [
    {
      provider: 'onlyoffice',
      extension: '.docx',
      mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      view: true,
      edit: true
    },
    {
      provider: 'onlyoffice',
      extension: '.docxf',
      mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      view: true,
      edit: true
    },
    {
      provider: 'onlyoffice',
      extension: '.pptx',
      mimeType: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
      view: true,
      edit: true
    },
    {
      provider: 'onlyoffice',
      extension: '.xlsx',
      mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      view: true,
      edit: true
    },
    {
      provider: 'onlyoffice',
      extension: '.oform',
      mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document.form',
      view: true,
      edit: true
    },
    {
      provider: 'onlyoffice',
      extension: '.pdf',
      mimeType: 'application/pdf',
      view: true,
      edit: false
    },
  ];

  const lang = eXo.env.portal.language || 'en';
  const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/locale.navigation.portal.global-${lang}.json`;

  exoi18n.loadLanguageAsync(lang, url).then(i18n => new Vue({i18n}));
  newDocumentTypeExtensionOptions.forEach(extension => extensionRegistry.registerExtension('attachment', 'new-document-action', extension));
  document.dispatchEvent(new CustomEvent('attachment-new-document-action-updated'));

  supportedDocumentsExtensionOptions.forEach(extension => extensionRegistry.registerExtension('documents', 'supported-document-types', extension));
  document.dispatchEvent(new CustomEvent('documents-supported-document-types-updated'));
})();
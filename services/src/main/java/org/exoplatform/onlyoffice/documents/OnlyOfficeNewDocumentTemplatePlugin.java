package org.exoplatform.onlyoffice.documents;

import java.util.Collections;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.DocumentTemplate;
import org.exoplatform.services.cms.documents.DocumentEditorPlugin;
import org.exoplatform.services.cms.documents.NewDocumentTemplatePlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * The Class OnlyOfficeNewDocumentTemplatePlugin.
 */
public class OnlyOfficeNewDocumentTemplatePlugin extends BaseComponentPlugin implements NewDocumentTemplatePlugin {

  /** The Constant LOG. */
  protected static final Log       LOG                              =
                                       ExoLogger.getLogger(OnlyOfficeNewDocumentTemplatePlugin.class);

  /**   The DOCUMENT_TYPES_CONFIGURATION param. */
  private static final String      DOCUMENT_TEMPLATES_CONFIGURATION = "document-templates-configuration";

  /** The document types. */
  protected List<DocumentTemplate> templates                        = Collections.emptyList();

  /** The document service. */
  protected final DocumentService  documentService;

  /**
   * Instantiates a new only office new document template plugin.
   *
   * @param documentService the new document service
   * @param initParams the init params
   */
  public OnlyOfficeNewDocumentTemplatePlugin(DocumentService documentService, InitParams initParams) {
    ObjectParameter typesParam = initParams.getObjectParam(DOCUMENT_TEMPLATES_CONFIGURATION);
    if (typesParam != null) {
      Object obj = typesParam.getObject();
      if (obj != null && DocumentService.DocumentTemplatesConfig.class.isAssignableFrom(obj.getClass())) {
        DocumentService.DocumentTemplatesConfig config = DocumentService.DocumentTemplatesConfig.class.cast(obj);
        this.templates = config.getTemplates();
      } else {
        LOG.error("The document templates are not set");
      }
    }
    this.documentService = documentService;
  }

  /**
   * Gets the templates.
   *
   * @return the templates
   */
  @Override
  public List<DocumentTemplate> getTemplates() {
    return templates;
  }

  /**
   * Creates the document.
   *
   * @param parent the parent
   * @param title the title
   * @param template the template
   * @return the node
   * @throws Exception the exception
   */
  @Override
  public Node createDocument(Node parent, String title, DocumentTemplate template) throws Exception {
    LOG.debug("Creating new document {} from template {}", title, template);
    return documentService.createDocumentFromTemplate(parent, title, template);
  }

  @Override
  public DocumentTemplate getTemplate(String name) {
    return templates.stream().filter(t -> t.getName().equals(name)).findAny().orElse(null);
  }

  @Override
  public DocumentEditorPlugin getEditor() {
    return documentService.getRegisteredEditorPlugins()
                          .stream()
                          .filter(plugin -> plugin.getProviderName().equals(OnlyOfficeDocumentEditorPlugin.PROVIDER_NAME))
                          .findAny()
                          .orElse(null);
  }

}

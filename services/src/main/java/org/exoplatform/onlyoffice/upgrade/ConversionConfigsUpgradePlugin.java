/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <gnu.org/licenses>.
 */
package org.exoplatform.onlyoffice.upgrade;

import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.onlyoffice.OnlyofficeEditorService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * One-time close of the stored editor configs that were built for a document
 * conversion (a thumbnail, an export), are still active and were never opened
 * by an editor: an editor joining such a key edits apart from its co-editors.
 * A conversion saves its config closed (EXO-91026); this plugin closes the
 * ones stored active.
 */
public class ConversionConfigsUpgradePlugin extends UpgradeProductPlugin {

  private static final Log              LOG = ExoLogger.getLogger(ConversionConfigsUpgradePlugin.class);

  private final OnlyofficeEditorService editorService;

  public ConversionConfigsUpgradePlugin(OnlyofficeEditorService editorService, InitParams initParams) {
    super(initParams);
    this.editorService = editorService;
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    int closed = editorService.closeUnopenedConversionConfigs();
    LOG.info("Closed {} OnlyOffice editor configs left active by document conversions", closed);
  }
}

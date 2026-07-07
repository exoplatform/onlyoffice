/*
 * Copyright (C) 2003-2026 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.onlyoffice;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.PropertySource;

import io.meeds.spring.AvailableIntegration;
import io.meeds.spring.kernel.PortalApplicationContextInitializer;

/**
 * Makes the ONLYOFFICE editor add-on participate in the portal's shared Spring
 * {@code ApplicationContext}, so its Spring-annotated beans (notably the
 * {@code OnlyofficeMcpTool} {@code @Service}, active under the
 * {@code mcp-server} profile) are discovered and can be collected by the
 * mcp-server tool registry.
 * <p>
 * The add-on has NO JPA / Elasticsearch repositories and no Liquibase Spring
 * configuration, so only the kernel and web integration modules are scanned in
 * addition to the add-on package. The existing kernel wiring (declared in the
 * add-on {@code configuration.xml}) is untouched: this Spring context lives
 * alongside it, exactly like the Documents add-on does.
 * <p>
 * Because there is no DB layer at all, the Spring Boot DataSource / JPA /
 * Liquibase auto-configurations (which are on the classpath transitively) are
 * fully excluded. Otherwise Spring Boot would try to build an
 * {@code entityManagerFactory} that depends on a {@code liquibase} bean whose
 * default {@code classpath:/db/changelog/db.changelog-master.yaml} does not
 * exist in this add-on, failing the context startup with a
 * {@code BeanCreationException} at boot.
 */
@SpringBootApplication(scanBasePackages = {
  OnlyofficeApplication.MODULE_NAME,
  AvailableIntegration.KERNEL_MODULE,
  AvailableIntegration.WEB_MODULE,
}, exclude = {
  LiquibaseAutoConfiguration.class,
  DataSourceAutoConfiguration.class,
  DataSourceTransactionManagerAutoConfiguration.class,
  HibernateJpaAutoConfiguration.class,
  JpaRepositoriesAutoConfiguration.class,
})
@PropertySource("classpath:application.properties")
@PropertySource("classpath:application-common.properties")
public class OnlyofficeApplication extends PortalApplicationContextInitializer {

  public static final String MODULE_NAME = "org.exoplatform.onlyoffice";

}

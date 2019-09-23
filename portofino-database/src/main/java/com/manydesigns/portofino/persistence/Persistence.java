/*
 * Copyright (C) 2005-2019 ManyDesigns srl.  All rights reserved.
 * http://www.manydesigns.com/
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
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

package com.manydesigns.portofino.persistence;

import com.manydesigns.elements.configuration.CommonsConfigurationUtils;
import com.manydesigns.elements.util.ElementsFileUtils;
import com.manydesigns.portofino.PortofinoProperties;
import com.manydesigns.portofino.cache.CacheResetEvent;
import com.manydesigns.portofino.cache.CacheResetListenerRegistry;
import com.manydesigns.portofino.model.Model;
import com.manydesigns.portofino.model.database.*;
import com.manydesigns.portofino.model.database.platforms.DatabasePlatformsRegistry;
import com.manydesigns.portofino.modules.DatabaseModule;
import com.manydesigns.portofino.persistence.hibernate.HibernateDatabaseSetup;
import com.manydesigns.portofino.persistence.hibernate.SessionFactoryAndCodeBase;
import com.manydesigns.portofino.persistence.hibernate.SessionFactoryBuilder;
import com.manydesigns.portofino.reflection.TableAccessor;
import com.manydesigns.portofino.reflection.ViewAccessor;
import com.manydesigns.portofino.sync.DatabaseSyncer;
import io.reactivex.subjects.BehaviorSubject;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
 * @author Angelo Lupo          - angelo.lupo@manydesigns.com
 * @author Giampiero Granatella - giampiero.granatella@manydesigns.com
 * @author Alessio Stalla       - alessio.stalla@manydesigns.com
 */
public class Persistence {
    public static final String copyright =
            "Copyright (C) 2005-2019 ManyDesigns srl";

    //**************************************************************************
    // Constants
    //**************************************************************************

    public static final String APP_MODEL_FILE = "portofino-model.xml";
    public static final String APP_CONTEXT = "liquibase.context";
    public final static String changelogFileNameTemplate = "liquibase.changelog.xml";

    //**************************************************************************
    // Fields
    //**************************************************************************

    protected final DatabasePlatformsRegistry databasePlatformsRegistry;
    protected Model model;
    protected final Map<String, HibernateDatabaseSetup> setups;

    protected final FileObject appDir;
    protected final FileObject appModelFile;
    protected final Configuration configuration;
    public final BehaviorSubject<Status> status = BehaviorSubject.create();

    public enum Status {
        STARTING, STARTED, STOPPING, STOPPED
    }

    public CacheResetListenerRegistry cacheResetListenerRegistry;

    //**************************************************************************
    // Logging
    //**************************************************************************

    public static final Logger logger =
            LoggerFactory.getLogger(Persistence.class);

    //**************************************************************************
    // Constructors
    //**************************************************************************

    public Persistence(FileObject appDir, Configuration configuration, DatabasePlatformsRegistry databasePlatformsRegistry) throws FileSystemException {
        this.appDir = appDir;
        this.configuration = configuration;
        this.databasePlatformsRegistry = databasePlatformsRegistry;

        appModelFile = appDir.resolveFile(APP_MODEL_FILE);
        logger.info("Application model file: {}", appModelFile.getName().getPath());

        setups = new HashMap<>();
    }

    //**************************************************************************
    // Model loading
    //**************************************************************************

    public synchronized void loadXmlModel() {
        logger.info("Loading xml model from file: {}", appModelFile.getName().getPath());

        try(InputStream inputStream = appModelFile.getContent().getInputStream()) {
            JAXBContext jc = JAXBContext.newInstance(Model.class.getPackage().getName());
            Unmarshaller um = jc.createUnmarshaller();
            Model model = (Model) um.unmarshal(inputStream);
            FileObject modelDir = getModelDirectory();
            for(Database database : model.getDatabases()) {
                FileObject databaseDir = modelDir.resolveFile(database.getDatabaseName());
                for(Schema schema : database.getSchemas()) {
                    FileObject schemaDir = databaseDir.resolveFile(schema.getSchemaName());
                    if(schemaDir.getType() == FileType.FOLDER) {
                        logger.debug("Schema directory {} exists", schemaDir);
                        FileObject[] tableFiles = schemaDir.getChildren();
                        for(FileObject tableFile : tableFiles) {
                            if(!tableFile.getName().getBaseName().endsWith(".table.xml")) {
                                continue;
                            }
                            try(InputStream tableInputStream = tableFile.getContent().getInputStream()) {
                                Table table = (Table) um.unmarshal(tableInputStream);
                                if (!tableFile.getName().getBaseName().equalsIgnoreCase(table.getTableName() + ".table.xml")) {
                                    logger.error("Skipping table " + table.getTableName() + " defined in file " + tableFile);
                                    continue;
                                }
                                table.afterUnmarshal(um, schema);
                                schema.getTables().add(table);
                            }
                        }
                    } else {
                        logger.debug("Schema directory {} does not exist", schemaDir);
                    }
                }
            }
            this.model = model;
            initModel();
        } catch (Exception e) {
            String msg = "Cannot load/parse model: " + appModelFile;
            logger.error(msg, e);
        }
    }

    public FileObject getModelDirectory() throws FileSystemException {
        return appModelFile.getParent().resolveFile(FilenameUtils.getBaseName(appModelFile.getName().getBaseName()));
    }

    public void runLiquibase(Database database) {
        logger.info("Updating database definitions");
        ResourceAccessor resourceAccessor = new FileSystemResourceAccessor(appDir.getName().getPath());
        ConnectionProvider connectionProvider =
                database.getConnectionProvider();
        for(Schema schema : database.getSchemas()) {
            String schemaName = schema.getSchemaName();
            try(Connection connection = connectionProvider.acquireConnection()) {
                FileObject changelogFile = getLiquibaseChangelogFile(schema);
                if(changelogFile.getType() != FileType.FILE) {
                    logger.info("Changelog file does not exist or is not a normal file, skipping: {}", changelogFile);
                    continue;
                }
                logger.info("Running changelog file: {}", changelogFile);
                JdbcConnection jdbcConnection = new JdbcConnection(connection);
                liquibase.database.Database lqDatabase =
                        DatabaseFactory.getInstance().findCorrectDatabaseImplementation(jdbcConnection);
                lqDatabase.setDefaultSchemaName(schema.getActualSchemaName());
                String relativeChangelogPath = appDir.getName().getRelativeName(changelogFile.getName());
                Liquibase lq = new Liquibase(relativeChangelogPath, resourceAccessor, lqDatabase);

                String[] contexts = configuration.getStringArray(APP_CONTEXT);
                logger.info("Using context {}", Arrays.toString(contexts));
                lq.update(new Contexts(contexts));

            } catch (Exception e) {
                String msg = "Couldn't update database: " + schemaName;
                logger.error(msg, e);
            }
        }
    }

    public synchronized void saveXmlModel() throws IOException, JAXBException, ConfigurationException {
        //TODO gestire conflitti con modifiche esterne?
        File tempFile = File.createTempFile(appModelFile.getName().getBaseName(), "");

        JAXBContext jc = JAXBContext.newInstance(Model.class.getPackage().getName());
        Marshaller m = jc.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        m.marshal(model, tempFile);

        ElementsFileUtils.moveFileSafely(tempFile, appModelFile.getName().getPath());

        FileObject modelDir = getModelDirectory();
        for(Database database : model.getDatabases()) {
            FileObject databaseDir = modelDir.resolveFile(database.getDatabaseName());
            for(Schema schema : database.getSchemas()) {
                FileObject schemaDir = databaseDir.resolveFile(schema.getSchemaName());
                if(schemaDir.getType() == FileType.FOLDER) {
                    schemaDir.createFolder();
                    logger.debug("Schema directory {} exists", schemaDir);
                    FileObject[] tableFiles = schemaDir.getChildren();
                    for(FileObject tableFile : tableFiles) {
                        if(tableFile.getName().getBaseName().endsWith(".table.xml")) {
                            if (!tableFile.delete()) {
                                logger.warn("Could not delete table file {}", tableFile.getName().getPath());
                            }
                        }
                    }
                    for(Table table : schema.getTables()) {
                        FileObject tableFile = schemaDir.resolveFile(table.getTableName() + ".table.xml");
                        try(OutputStream outputStream = tableFile.getContent().getOutputStream()) {
                            m.marshal(table, outputStream);
                        }
                    }
                } else {
                    logger.debug("Schema directory {} does not exist", schemaDir);
                }
            }
        }
        logger.info("Saved xml model to file: {}", appModelFile);
        CommonsConfigurationUtils.save(configuration);
    }

    public synchronized void initModel() {
        logger.info("Cleaning up old setups");
        closeSessions();
        for (Map.Entry<String, HibernateDatabaseSetup> current : setups.entrySet()) {
            String databaseName = current.getKey();
            logger.info("Cleaning up old setup for: {}", databaseName);
            HibernateDatabaseSetup hibernateDatabaseSetup = current.getValue();
            try {
                SessionFactory sessionFactory = hibernateDatabaseSetup.getSessionFactory();
                sessionFactory.close();
            } catch (Throwable t) {
                logger.warn("Cannot close session factory for: " + databaseName, t);
            }
        }

        setups.clear();
        model.init(configuration);
        for (Database database : model.getDatabases()) {
            try {
                ConnectionProvider connectionProvider = database.getConnectionProvider();
                connectionProvider.init(databasePlatformsRegistry);
                if (connectionProvider.getStatus().equals(ConnectionProvider.STATUS_CONNECTED)) {
                    SessionFactoryBuilder builder = new SessionFactoryBuilder(database);
                    SessionFactoryAndCodeBase sessionFactoryAndCodeBase = builder.buildSessionFactory();
                    HibernateDatabaseSetup setup =
                            new HibernateDatabaseSetup(database, sessionFactoryAndCodeBase.sessionFactory);
                    String databaseName = database.getDatabaseName();
                    setups.put(databaseName, setup);
                }
            } catch (Exception e) {
                logger.error("Could not create connection provider for " + database, e);
            }
        }
        if(cacheResetListenerRegistry != null) {
            cacheResetListenerRegistry.fireReset(new CacheResetEvent(this));
        }
    }

    //**************************************************************************
    // Database stuff
    //**************************************************************************

    public ConnectionProvider getConnectionProvider(String databaseName) {
        for (Database database : model.getDatabases()) {
            if (database.getDatabaseName().equals(databaseName)) {
                return database.getConnectionProvider();
            }
        }
        return null;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public DatabasePlatformsRegistry getDatabasePlatformsRegistry() {
        return databasePlatformsRegistry;
    }

    //**************************************************************************
    // Model access
    //**************************************************************************

    public Model getModel() {
        return model;
    }

    public synchronized void syncDataModel(String databaseName) throws Exception {
        Database sourceDatabase = DatabaseLogic.findDatabaseByName(model, databaseName);
        if(sourceDatabase == null) {
            throw new IllegalArgumentException("Database " + databaseName + " does not exist");
        }
        if(configuration.getBoolean(DatabaseModule.LIQUIBASE_ENABLED, true)) {
            runLiquibase(sourceDatabase);
        } else {
            logger.debug("syncDataModel called, but Liquibase is not enabled");
        }
        ConnectionProvider connectionProvider = sourceDatabase.getConnectionProvider();
        DatabaseSyncer dbSyncer = new DatabaseSyncer(connectionProvider);
        Database targetDatabase = dbSyncer.syncDatabase(model);
        model.getDatabases().remove(sourceDatabase);
        model.getDatabases().add(targetDatabase);
    }

    //**************************************************************************
    // Persistance
    //**************************************************************************

    public Session getSession(String databaseName) {
        return ensureDatabaseSetup(databaseName).getThreadSession();
    }

    protected HibernateDatabaseSetup ensureDatabaseSetup(String databaseName) {
        HibernateDatabaseSetup setup = setups.get(databaseName);
        if (setup == null) {
            throw new Error("No setup exists for database: " + databaseName);
        }
        return setup;
    }

    public void closeSessions() {
        for (HibernateDatabaseSetup current : setups.values()) {
            closeSession(current);
        }
    }

    public void closeSession(String databaseName) {
        closeSession(ensureDatabaseSetup(databaseName));
    }

    protected void closeSession(HibernateDatabaseSetup current) {
        Session session = current.getThreadSession(false);
        if (session != null) {
            try {
                Transaction transaction = session.getTransaction();
                if(transaction != null && transaction.isActive()) {
                    transaction.rollback();
                }
                session.close();
            } catch (Throwable e) {
                logger.warn("Couldn't close session: " + ExceptionUtils.getRootCauseMessage(e), e);
            }
            current.removeThreadSession();
        }
    }

    public @NotNull TableAccessor getTableAccessor(String databaseName, String entityName) {
        Database database = DatabaseLogic.findDatabaseByName(model, databaseName);
        if(database == null) {
            throw new IllegalArgumentException("Database " + databaseName + " does not exist");
        }
        Table table = DatabaseLogic.findTableByEntityName(database, entityName);
        if(table == null) {
            throw new IllegalArgumentException("Table " + entityName + " not found in database " + databaseName);
        }
        return table instanceof View ? new ViewAccessor((View) table) : new TableAccessor(table);
    }

    //**************************************************************************
    // User
    //**************************************************************************

    public void start() {
        status.onNext(Status.STARTING);
        loadXmlModel();
        for(Database database : model.getDatabases()) {
            runLiquibase(database);
        }
        status.onNext(Status.STARTED);
    }

    public void stop() {
        status.onNext(Status.STOPPING);
        for(HibernateDatabaseSetup setup : setups.values()) {
            //TODO It is the responsibility of the application to ensure that there are no open Sessions before calling close().
            //http://ajava.org/online/hibernate3api/org/hibernate/SessionFactory.html#close%28%29
            setup.getSessionFactory().close();
        }
        for (Database database : model.getDatabases()) {
            ConnectionProvider connectionProvider =
                    database.getConnectionProvider();
            connectionProvider.shutdown();
        }
        status.onNext(Status.STOPPED);
    }

    //**************************************************************************
    // App directories and files
    //**************************************************************************

    public String getName() {
        return getConfiguration().getString(PortofinoProperties.APP_NAME);
    }

    public FileObject getLiquibaseChangelogFile(Schema schema) throws FileSystemException {
        if(schema == null) {
            return null;
        }
        FileObject dbDir = getModelDirectory().resolveFile(schema.getDatabaseName());
        FileObject schemaDir = dbDir.resolveFile(schema.getSchemaName());
        return schemaDir.resolveFile(changelogFileNameTemplate);
    }

    public FileObject getAppModelFile() {
        return appModelFile;
    }

}

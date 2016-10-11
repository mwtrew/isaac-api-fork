/**
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.configuration;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import ma.glasnost.orika.MapperFacade;

import org.elasticsearch.client.Client;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.ContentVersionController;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.StatisticsManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAuthenticationManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.EmailVerificationMisusehandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.EmailVerificationRequestMisusehandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.api.monitors.InMemoryMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.api.monitors.LogEventMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.PasswordResetRequestMisusehandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.SegueLoginMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.TokenOwnerLookupMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.QuestionAttemptMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.AnonQuestionAttemptMisuseHandler;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.FacebookAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.GoogleAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.SegueLocalAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.TwitterAuthenticator;
import uk.ac.cam.cl.dtg.segue.comm.EmailCommunicator;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.ICommunicator;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.LocationManager;
import uk.ac.cam.cl.dtg.segue.dao.PgLogManager;
import uk.ac.cam.cl.dtg.segue.dao.associations.IAssociationDataManager;
import uk.ac.cam.cl.dtg.segue.dao.associations.PgAssociationDataManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserDataManager;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserGroupPersistenceManager;
import uk.ac.cam.cl.dtg.segue.dao.users.PgUserGroupPersistenceManager;
import uk.ac.cam.cl.dtg.segue.dao.users.PgUsers;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dos.AbstractEmailPreferenceManager;
import uk.ac.cam.cl.dtg.segue.dos.LocationHistory;
import uk.ac.cam.cl.dtg.segue.dos.PgEmailPreferenceManager;
import uk.ac.cam.cl.dtg.segue.dos.PgLocationHistory;
import uk.ac.cam.cl.dtg.segue.quiz.IQuestionAttemptManager;
import uk.ac.cam.cl.dtg.segue.quiz.PgQuestionAttempts;
import uk.ac.cam.cl.dtg.segue.search.ElasticSearchProvider;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;
import uk.ac.cam.cl.dtg.util.PropertiesManager;
import uk.ac.cam.cl.dtg.util.locations.IPLocationResolver;
import uk.ac.cam.cl.dtg.util.locations.IPInfoDBLocationResolver;
import uk.ac.cam.cl.dtg.util.locations.PostCodeIOLocationResolver;
import uk.ac.cam.cl.dtg.util.locations.PostCodeLocationResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

/**
 * This class is responsible for injecting configuration values for persistence related classes.
 * 
 */
public class SegueGuiceConfigurationModule extends AbstractModule implements ServletContextListener {
    private static final Logger log = LoggerFactory.getLogger(SegueGuiceConfigurationModule.class);

    private static PropertiesLoader configLocationProperties = null;
    private static PropertiesLoader globalProperties = null;
    
    // Singletons - we only ever want there to be one instance of each of these.
    private static PostgresSqlDb postgresDB;
    private static ContentMapper mapper = null;
    private static ContentVersionController contentVersionController = null;
    private static GitContentManager contentManager = null;
    private static Client elasticSearchClient = null;
    private static UserAccountManager userManager = null;
    private static IQuestionAttemptManager questionPersistenceManager = null;
    private static ILogManager logManager;
    private static EmailManager emailCommunicationQueue = null;
    private static IMisuseMonitor misuseMonitor = null;
    private static StatisticsManager statsManager = null;
	private static GroupManager groupManager = null;

    private static Collection<Class<? extends ServletContextListener>> contextListeners;
    private static Reflections reflections = null;
    
    /**
     * Create a SegueGuiceConfigurationModule.
     */
    public SegueGuiceConfigurationModule() {
        if (globalProperties == null || configLocationProperties == null) {
            try {
                if (null == configLocationProperties) {
                    configLocationProperties = new PropertiesLoader("/config/segue-config-location.properties");

                }

                if (null == globalProperties) {
                    globalProperties = new PropertiesLoader(
                            configLocationProperties.getProperty(Constants.GENERAL_CONFIG_LOCATION));
                }
            } catch (IOException e) {
                log.error("Error loading properties file.", e);
            }
        }
    }

    @Override
    protected void configure() {
        try {
            this.configureProperties();
            this.configureDataPersistence();
            this.configureSegueSearch();
            this.configureAuthenticationProviders();
            this.configureApplicationManagers();

        } catch (IOException e) {
            e.printStackTrace();
            log.error("IOException during setup process.");
        }
    }

    /**
     * Extract properties and bind them to constants.
     */
    private void configureProperties() {
        // Properties loader
        bind(PropertiesLoader.class).toInstance(globalProperties);

        this.bindConstantToProperty(Constants.SEARCH_CLUSTER_NAME, globalProperties);
        this.bindConstantToProperty(Constants.SEARCH_CLUSTER_ADDRESS, globalProperties);
        this.bindConstantToProperty(Constants.SEARCH_CLUSTER_PORT, globalProperties);

        this.bindConstantToProperty(Constants.HOST_NAME, globalProperties);
        this.bindConstantToProperty(Constants.MAILER_SMTP_SERVER, globalProperties);
        this.bindConstantToProperty(Constants.MAIL_FROM_ADDRESS, globalProperties);
        this.bindConstantToProperty(Constants.MAIL_NAME, globalProperties);
        this.bindConstantToProperty(Constants.SERVER_ADMIN_ADDRESS, globalProperties);

        this.bindConstantToProperty(Constants.LOGGING_ENABLED, globalProperties);

        // IP address geocoding
        this.bindConstantToProperty(Constants.IP_INFO_DB_API_KEY, globalProperties);
        
        this.bindConstantToProperty(Constants.SCHOOL_CSV_LIST_PATH, globalProperties);
        
    }

    /**
     * Configure all things persistency.
     * 
     * @throws IOException
     *             - when we cannot load the database.
     */
    private void configureDataPersistence() throws IOException {
        // Setup different persistence bindings
        // MongoDb - currently not used.
        this.bindConstantToProperty(Constants.MONGO_DB_HOSTNAME, globalProperties);
        this.bindConstantToProperty(Constants.MONGO_DB_PORT, globalProperties);
        this.bindConstantToProperty(Constants.MONGO_CONNECTIONS_PER_HOST, globalProperties);
        this.bindConstantToProperty(Constants.MONGO_CONNECTION_TIMEOUT, globalProperties);
        this.bindConstantToProperty(Constants.MONGO_SOCKET_TIMEOUT, globalProperties);

        this.bindConstantToProperty(Constants.SEGUE_DB_NAME, globalProperties);

        // postgres
        this.bindConstantToProperty(Constants.POSTGRES_DB_URL, globalProperties);
        this.bindConstantToProperty(Constants.POSTGRES_DB_USER, globalProperties);
        this.bindConstantToProperty(Constants.POSTGRES_DB_PASSWORD, globalProperties);

        // GitDb
        bind(GitDb.class).toInstance(
                new GitDb(globalProperties.getProperty(Constants.LOCAL_GIT_DB), globalProperties
                        .getProperty(Constants.REMOTE_GIT_SSH_URL), globalProperties
                        .getProperty(Constants.REMOTE_GIT_SSH_KEY_PATH)));

        bind(IUserGroupPersistenceManager.class).to(PgUserGroupPersistenceManager.class);

        bind(IAssociationDataManager.class).to(PgAssociationDataManager.class);
    }

    /**
     * Configure segue search classes.
     */
    private void configureSegueSearch() {
        bind(ISearchProvider.class).to(ElasticSearchProvider.class);
    }

    /**
     * Configure user security related classes.
     */
    private void configureAuthenticationProviders() {
        this.bindConstantToProperty(Constants.HMAC_SALT, globalProperties);

        // Configure security providers
        // Google
        this.bindConstantToProperty(Constants.GOOGLE_CLIENT_SECRET_LOCATION, globalProperties);
        this.bindConstantToProperty(Constants.GOOGLE_CALLBACK_URI, globalProperties);
        this.bindConstantToProperty(Constants.GOOGLE_OAUTH_SCOPES, globalProperties);

        // Facebook
        this.bindConstantToProperty(Constants.FACEBOOK_SECRET, globalProperties);
        this.bindConstantToProperty(Constants.FACEBOOK_CLIENT_ID, globalProperties);
        this.bindConstantToProperty(Constants.FACEBOOK_CALLBACK_URI, globalProperties);
        this.bindConstantToProperty(Constants.FACEBOOK_OAUTH_SCOPES, globalProperties);

        // Twitter
        this.bindConstantToProperty(Constants.TWITTER_SECRET, globalProperties);
        this.bindConstantToProperty(Constants.TWITTER_CLIENT_ID, globalProperties);
        this.bindConstantToProperty(Constants.TWITTER_CALLBACK_URI, globalProperties);

        // Register a map of security providers
        MapBinder<AuthenticationProvider, IAuthenticator> mapBinder = MapBinder.newMapBinder(binder(),
                AuthenticationProvider.class, IAuthenticator.class);
        mapBinder.addBinding(AuthenticationProvider.GOOGLE).to(GoogleAuthenticator.class);
        mapBinder.addBinding(AuthenticationProvider.FACEBOOK).to(FacebookAuthenticator.class);
        mapBinder.addBinding(AuthenticationProvider.TWITTER).to(TwitterAuthenticator.class);
        mapBinder.addBinding(AuthenticationProvider.SEGUE).to(SegueLocalAuthenticator.class);

    }

    /**
     * Deals with application data managers.
     */
    private void configureApplicationManagers() {
        // Allows GitDb to take over content Management
        bind(IContentManager.class).to(GitContentManager.class);

        bind(LocationHistory.class).to(PgLocationHistory.class);
        
        bind(PostCodeLocationResolver.class).to(PostCodeIOLocationResolver.class);

        bind(IUserDataManager.class).to(PgUsers.class);
        
        bind(ICommunicator.class).to(EmailCommunicator.class);
        
        bind(AbstractEmailPreferenceManager.class).to(PgEmailPreferenceManager.class);
    }

    /**
     * This provides a singleton of the elasticSearch client that can be used by Guice.
     * 
     * The client is threadsafe so we don't need to keep creating new ones.
     * 
     * @param clusterName
     *            - The name of the cluster to create.
     * @param address
     *            - address of the cluster to create.
     * @param port
     *            - port of the custer to create.
     * @return Client to be injected into ElasticSearch Provider.
     */
    @Inject
    @Provides
    @Singleton
    private static Client getSearchConnectionInformation(
            @Named(Constants.SEARCH_CLUSTER_NAME) final String clusterName,
            @Named(Constants.SEARCH_CLUSTER_ADDRESS) final String address,
            @Named(Constants.SEARCH_CLUSTER_PORT) final int port) {
        if (null == elasticSearchClient) {
            try {
                elasticSearchClient = ElasticSearchProvider.getTransportClient(clusterName, address, port);
                log.info("Creating singleton of ElasticSearchProvider");
            } catch (UnknownHostException e) {
                log.error("Could not create ElasticSearchProvider");
                return null;
            }
        }
        // eventually we want to do something like the below to make sure we get updated clients        
//        if (elasticSearchClient instanceof TransportClient) {
//            TransportClient tc = (TransportClient) elasticSearchClient;
//            if (tc.connectedNodes().isEmpty()) {
//                tc.close();        
//                log.error("The elasticsearch client is not connected to any nodes. Trying to reconnect...");
//                elasticSearchClient = null;
//                return getSearchConnectionInformation(clusterName, address, port);
//            }
//        }

        return elasticSearchClient;
    }
    
    /**
     * This provides a singleton of the contentVersionController for the segue facade.
     * 
     * @param generalProperties
     *            - properties loader
     * @param contentManager
     *            - content manager (with associated persistence links).
     * @return Content version controller with associated dependencies.
     * @throws IOException
     *             - if we can't load the properties file for live version.
     */
    @Inject
    @Provides
    @Singleton
    private static ContentVersionController getContentVersionController(final PropertiesLoader generalProperties,
            final IContentManager contentManager) throws IOException {

        PropertiesManager versionPropertiesLoader = new PropertiesManager(
                configLocationProperties.getProperty(Constants.LIVE_VERSION_CONFIG_LOCATION));

        if (null == contentVersionController) {
            contentVersionController = new ContentVersionController(generalProperties, versionPropertiesLoader,
                    contentManager);
            log.info("Creating singleton of ContentVersionController");
        }
        return contentVersionController;
    }

    /**
     * This provides a singleton of the git content manager for the segue facade.
     * 
     * TODO: This is a singleton as the units and tags are stored in memory. If we move these out it can be an instance.
     * This would be better as then we can give it a new search provider if the client has closed.
     * 
     * @param database
     *            - database reference
     * @param searchProvider
     *            - search provider to use
     * @param contentMapper
     *            - content mapper to use.
     * @return a fully configured content Manager.
     */
    @Inject
    @Provides
    @Singleton
    private static GitContentManager getContentManager(final GitDb database, final ISearchProvider searchProvider,
            final ContentMapper contentMapper) {
        if (null == contentManager) {
            contentManager = new GitContentManager(database, searchProvider, contentMapper);
            log.info("Creating singleton of ContentManager");
        }

        return contentManager;
    }

    /**
     * This provides a singleton of the LogManager for the Segue facade.
     * 
     * Note: This is a singleton as logs are created very often and we wanted to minimise the overhead in class
     * creation. Although we can convert this to instances if we want to tidy this up.
     * 
     * @param database
     *            - database reference
     * @param loggingEnabled
     *            - boolean to determine if we should persist log messages.
     * @param lhm
     *            - location history manager
     * @return A fully configured LogManager
     */
    @Inject
    @Provides
    @Singleton
    private static ILogManager getLogManager(final PostgresSqlDb database,
            @Named(Constants.LOGGING_ENABLED) final boolean loggingEnabled, final LocationManager lhm) {
        if (null == logManager) {
            //logManager = new MongoLogManager(database, new ObjectMapper(), loggingEnabled, lhm);
            
            logManager = new PgLogManager(database, new ObjectMapper(), loggingEnabled, lhm);
            
            log.info("Creating singleton of LogManager");
            if (loggingEnabled) {
                log.info("Log manager configured to record logging.");
            } else {
                log.info("Log manager configured NOT to record logging.");
            }
        }

        return logManager;
    }

    /**
     * This provides a singleton of the contentVersionController for the segue facade. 
     * Note: This is a singleton because this content mapper has to use reflection to register all content classes.
     * 
     * @return Content version controller with associated dependencies.
     */
    @Inject
    @Provides
    @Singleton
    private static ContentMapper getContentMapper() {
        if (null == mapper) {
            mapper = new ContentMapper(getReflectionsClass());
            log.info("Creating Singleton of the Content Mapper");
        }

        return mapper;
    }

    /**
     * This provides a singleton of the e-mail manager class.
     *
     * Note: This has to be a singleton because it manages all emails sent using this JVM.
     *
     * @param database
     * 			- the database to access preferences
     * @param properties
     * 			- the properties so we can generate email
     * @param emailCommunicator
     *            the class the queue will send messages with
     * @param emailPreferenceManager
     * 			- the class providing email preferences
     * @param contentVersionController
     * 			- the content so we can access email templates
     * @param authenticator
     * 			- the authenticator
     * @param logManager
     * 			- the logManager to log email sent
     * @return an instance of the queue
     */
    @Inject
    @Provides
    @Singleton
    private static EmailManager getMessageCommunicationQueue(final IUserDataManager database,
            final PropertiesLoader properties, final EmailCommunicator emailCommunicator,
            final AbstractEmailPreferenceManager emailPreferenceManager,
            final ContentVersionController contentVersionController, final SegueLocalAuthenticator authenticator,
            final ILogManager logManager) {
        if (null == emailCommunicationQueue) {
            emailCommunicationQueue = new EmailManager(emailCommunicator, emailPreferenceManager, properties,
            				contentVersionController, logManager);
            log.info("Creating singleton of EmailCommunicationQueue");
        }
        return emailCommunicationQueue;
    }

    /**
     * This provides a singleton of the UserManager for various facades.
     *
     * Note: This has to be a a singleton as the User Manager keeps a temporary cache of anonymous users.
     *
     * @param database
     *            - the user persistence manager.
     * @param questionManager
     *            - IUserManager
     * @param properties
     *            - properties loader
     * @param providersToRegister
     *            - list of known providers.
     * @param emailQueue
     *            - so that we can send e-mails.
     * @param logManager
     *            - so that we can log interesting user based events.
     * @param mapperFacade
     *            - for DO and DTO mapping.
     * @param userAuthenticationManager
     *            - Responsible for handling the various authentication functions.
     * @return Content version controller with associated dependencies.
     */
    @Inject
    @Provides
    @Singleton
    private UserAccountManager getUserManager(final IUserDataManager database, final QuestionManager questionManager,
            final PropertiesLoader properties, final Map<AuthenticationProvider, IAuthenticator> providersToRegister,
            final EmailManager emailQueue, final ILogManager logManager, final MapperFacade mapperFacade,
            final UserAuthenticationManager userAuthenticationManager) {

        if (null == userManager) {
            userManager = new UserAccountManager(database, questionManager, properties, providersToRegister,
                    mapperFacade, emailQueue, logManager, userAuthenticationManager);
            log.info("Creating singleton of UserManager");
        }

        return userManager;
    }

    /**
     * QuestionManager.
     * Note: This has to be a singleton as the question manager keeps anonymous question attempts in memory.
     *
     * @param ds - postgres data source
     * @param objectMapper - mapper
     * @return a singleton for question persistence.
     */
    @Inject
    @Provides
    @Singleton
    private IQuestionAttemptManager getQuestionManager(final PostgresSqlDb ds, final ContentMapper objectMapper) {
        // this needs to be a singleton as it provides a temporary cache for anonymous question attempts.
        if (null == questionPersistenceManager) {
            questionPersistenceManager = new PgQuestionAttempts(ds, objectMapper);
            log.info("Creating singleton of IQuestionAttemptManager");
        }

        return questionPersistenceManager;
    }

    /**
     * This provides a singleton of the GroupManager.
     *
     * Note: This needs to be a singleton as we register observers for groups.
     *
     * @param userGroupDataManager
     *            - user group data manager
     * @param userManager
     *            - user manager
     * @param dtoMapper
     *            - dtoMapper
     * @return group manager
     */
    @Inject
    @Provides
    @Singleton
    private GroupManager getGroupManager(final IUserGroupPersistenceManager userGroupDataManager,
            final UserAccountManager userManager, final MapperFacade dtoMapper) {

        if (null == groupManager) {
            groupManager = new GroupManager(userGroupDataManager, userManager, dtoMapper);
            log.info("Creating singleton of GroupManager");
        }

        return groupManager;
    }

    /**
     * Get singleton of misuseMonitor.
     *
     * Note: this has to be a singleton as it tracks (in memory) the number of misuses.
     *
     * @param emailManager
     *            - so that the monitors can send e-mails.
     * @param properties
     *            - so that the monitors can look up email settings etc.
     * @return gets the singleton of the misuse manager.
     */
    @Inject
    @Provides
    @Singleton
    private IMisuseMonitor getMisuseMonitor(final EmailManager emailManager, final PropertiesLoader properties) {
        if (null == misuseMonitor) {
            misuseMonitor = new InMemoryMisuseMonitor();
            log.info("Creating singleton of MisuseMonitor");

            // TODO: We should automatically register all handlers that implement this interface using reflection?
            // register handlers segue specific handlers
            misuseMonitor.registerHandler(TokenOwnerLookupMisuseHandler.class.toString(),
                    new TokenOwnerLookupMisuseHandler(emailManager, properties));

            misuseMonitor.registerHandler(EmailVerificationMisusehandler.class.toString(),
                    new EmailVerificationMisusehandler());

            misuseMonitor.registerHandler(EmailVerificationRequestMisusehandler.class.toString(),
                    new EmailVerificationRequestMisusehandler());

            misuseMonitor.registerHandler(PasswordResetRequestMisusehandler.class.toString(),
                    new PasswordResetRequestMisusehandler());

            misuseMonitor.registerHandler(SegueLoginMisuseHandler.class.toString(),
                    new SegueLoginMisuseHandler(emailManager, properties));

            misuseMonitor.registerHandler(LogEventMisuseHandler.class.toString(),
                    new LogEventMisuseHandler(emailManager, properties));

            misuseMonitor.registerHandler(QuestionAttemptMisuseHandler.class.toString(),
                    new QuestionAttemptMisuseHandler(emailManager, properties));

            misuseMonitor.registerHandler(AnonQuestionAttemptMisuseHandler.class.toString(),
                    new AnonQuestionAttemptMisuseHandler());
        }

        return misuseMonitor;
    }

    /**
     * Gets the instance of the dozer mapper object.
     * 
     * @return a preconfigured instance of an Auto Mapper. This is specialised for mapping SegueObjects.
     */
    @Provides
    @Singleton
    @Inject
    public static MapperFacade getDOtoDTOMapper() {
        return SegueGuiceConfigurationModule.getContentMapper().getAutoMapper();
    }

    /**
     * @return segue version currently running.
     */
    public static String getSegueVersion() {
        if (configLocationProperties != null) {
            return configLocationProperties.getProperty(Constants.SEGUE_APP_VERSION);
        }
        log.warn("Unable to read segue version property");
        return "UNKNOWN";
    }

    /**
     * Gets the instance of the postgres connection wrapper.
     * 
     * Note: This needs to be a singleton as it contains a connection pool.
     * 
     * @param databaseUrl
     *            - database to connect to.
     * @param username
     *            - port that the mongodb service is running on.
     * @param password
     *            - the name of the database to configure the wrapper to use.
     * @return PostgresSqlDb db object preconfigured to work with the segue database.
     * @throws SQLException
     *             - If we cannot create the connection.
     */
    @Provides
    @Singleton
    @Inject
    private static PostgresSqlDb getPostgresDB(@Named(Constants.POSTGRES_DB_URL) final String databaseUrl,
            @Named(Constants.POSTGRES_DB_USER) final String username,
            @Named(Constants.POSTGRES_DB_PASSWORD) final String password) throws SQLException {

        if (null == postgresDB) {
            try {
                postgresDB = new PostgresSqlDb(databaseUrl, username, password);
                log.info("Created Singleton of PostgresDb wrapper");
            } catch (ClassNotFoundException e) {
                log.error("Unable to locate postgres driver.", e);
            }
        }

        return postgresDB;
    }

    /**
     * Gets the instance of the StatisticsManager. Note: this class is a hack and needs to be refactored.... It is
     * currently only a singleton as it keeps a cache.
     * 
     * @param userManager
     *            - dependency
     * @param logManager
     *            - dependency
     * @param schoolManager
     *            - dependency
     * @param versionManager
     *            - dependency
     * @param locationHistoryManager
     *            - dependency
     * @param groupManager
     *            - dependency
     * @param questionManager
     *            - dependency
     * @return stats manager
     */
    @Provides
    @Singleton
    @Inject
    private static StatisticsManager getStatsManager(final UserAccountManager userManager,
            final ILogManager logManager, final SchoolListReader schoolManager,
            final ContentVersionController versionManager, final LocationManager locationHistoryManager,
            final GroupManager groupManager, final QuestionManager questionManager) {

        if (null == statsManager) {
            statsManager = new StatisticsManager(userManager, logManager, schoolManager, versionManager,
                    locationHistoryManager, groupManager, questionManager);
            log.info("Created Singleton of Statistics Manager");

        }

        return statsManager;
    }
    
    /**
     * This provides a new instance of the location resolver.
     *
     * @param apiKey
     *            - for using the third party service.
     * @return The singleton instance of EmailCommunicator
     */
    @Inject
    @Provides
    private IPLocationResolver getIPLocator(@Named(Constants.IP_INFO_DB_API_KEY) final String apiKey) {
        return new IPInfoDBLocationResolver(apiKey);
    }

    /**
     * Utility method to make the syntax of property bindings clearer.
     * 
     * @param propertyLabel
     *            - Key for a given property
     * @param propertyLoader
     *            - property loader to use
     */
    private void bindConstantToProperty(final String propertyLabel, final PropertiesLoader propertyLoader) {
        bindConstant().annotatedWith(Names.named(propertyLabel)).to(propertyLoader.getProperty(propertyLabel));
    }

    /**
     * Utility method to get a pre-generated reflections class for the uk.ac.cam.cl.dtg.segue package.
     *
     * @return reflections.
     */
    public static Reflections getReflectionsClass() {
        if (null == reflections) {
            log.info("Caching reflections scan on uk.ac.cam.cl.dtg.segue....");
            reflections = new Reflections("uk.ac.cam.cl.dtg.segue");
        }
        return reflections;
    }

    /**
     * Gets the segue classes that should be registered as context listeners.
     *
     * @return the list of context listener classes (these should all be singletons).
     */
    public static Collection<Class<? extends ServletContextListener>> getRegisteredContextListenerClasses() {

        if (null == contextListeners) {
            contextListeners = Lists.newArrayList();

            Set<Class<? extends ServletContextListener>> subTypes = getReflectionsClass()
                    .getSubTypesOf(ServletContextListener.class);

            for (Class<? extends ServletContextListener> contextListener : subTypes) {
                contextListeners.add(contextListener);
            }
        }

        return contextListeners;
    }

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        // nothing needed
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {
        // Close all resources we hold.
        log.info("Segue Config Module notified of shutdown. Releasing resources");
        elasticSearchClient.close();
        elasticSearchClient = null;

        try {
            postgresDB.close();
            postgresDB = null;
        } catch (IOException e) {
            log.error("Unable to close external connection", e);
        }
    }
}

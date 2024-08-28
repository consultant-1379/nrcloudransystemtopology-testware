/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.test.scenarios;

import static com.ericsson.cifwk.taf.scenario.TestScenarios.*;
import static com.ericsson.oss.cm.upgradeindependence.taf.teststep.UnsupportedNodeTestSteps.*;
import static com.ericsson.oss.cm.upgradeindependence.taf.utils.Constants.*;
import static com.ericsson.oss.cm.upgradeindependence.taf.utils.MatcherAssertWithLog.assertThat;
import static com.ericsson.oss.services.test.steps.NodeIntegrationGNBDUFunction.ParameterConstant.*;
import static com.ericsson.oss.services.test.utils.Constant.*;
import static com.ericsson.oss.testware.enmbase.data.CommonDataSources.*;
import static com.ericsson.oss.testware.security.gim.flows.GimCleanupFlows.EnmObjectType;

import javax.inject.Inject;

import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;

import com.ericsson.cifwk.taf.TafTestBase;
import com.ericsson.cifwk.taf.TestContext;
import com.ericsson.cifwk.taf.annotations.TestId;
import com.ericsson.cifwk.taf.data.DataHandler;
import com.ericsson.cifwk.taf.scenario.TestScenario;
import com.ericsson.cifwk.taf.scenario.TestScenarioRunner;
import com.ericsson.cifwk.taf.scenario.TestScenarios;
import com.ericsson.cifwk.taf.scenario.api.ScenarioExceptionHandler;
import com.ericsson.cifwk.taf.scenario.api.TestScenarioBuilder;
import com.ericsson.cifwk.taf.scenario.impl.LoggingScenarioListener;
import com.ericsson.oss.cm.upgradeindependence.taf.flows.UnsupportedNodeFlows;
import com.ericsson.oss.cm.upgradeindependence.taf.teststep.UnsupportedNodeTestSteps;
import com.ericsson.oss.cm.upgradeindependence.taf.utils.FilterNodesToAdd;
import com.ericsson.oss.cm.upgradeindependence.taf.utils.RestApiUri;
import com.ericsson.oss.cm.upgradeindependence.taf.utils.SharedFlows;
import com.ericsson.oss.services.test.flows.NrCloudRanSystemTopologyTestFlow;
import com.ericsson.oss.services.test.steps.NodeIntegrationGNBDUFunction;
import com.ericsson.oss.testware.enmbase.data.CommonDataSources;
import com.ericsson.oss.testware.network.teststeps.NetworkElementTestSteps;
import com.ericsson.oss.testware.networkexplorer.flows.NetworkExplorerFlows;
import com.ericsson.oss.testware.nodeintegration.flows.NodeIntegrationFlows;
import com.ericsson.oss.testware.security.authentication.flows.LoginLogoutRestFlows;
import com.ericsson.oss.testware.security.gim.flows.GimCleanupFlows;
import com.ericsson.oss.testware.security.gim.flows.RoleManagementTestFlows;
import com.ericsson.oss.testware.security.gim.flows.UserManagementTestFlows;

public class NrCloudRanSystemTopologyScenarios extends TafTestBase {
    private static final Logger log = LoggerFactory.getLogger(NrCloudRanSystemTopologyScenarios.class);

    private static final String NRCRAN_USER = "nrclouduser";

    private static final String GROUPS_PROP = DataHandler.getConfiguration().getProperty("groups", "NSS", String.class);
    private static final String MODE_PROP = DataHandler.getConfiguration().getProperty("mode", "", String.class);
    private static final String EXCLUDED_NE_TYPES = DataHandler.getConfiguration().getProperty("excludeNeType", "", String.class);

    private static TestScenarioRunner runner = runner().withListener(new LoggingScenarioListener()).build();

    @Inject
    TestContext context;

    @Inject
    private NodeIntegrationFlows nodeIntegrationFlows;
    @Inject
    private LoginLogoutRestFlows loginLogoutRestFlows;
    @Inject
    private GimCleanupFlows idmCleanupFlows;
    @Inject
    private UserManagementTestFlows userManagementFlows;
    @Inject
    private RoleManagementTestFlows roleManagementFlows;
    @Inject
    private NetworkExplorerFlows networkExplorerFlows;
    @Inject
    private NrCloudRanSystemTopologyTestFlow nrCloudRanTestFlow;
    @Inject
    private NetworkElementTestSteps networkElementTestSteps;
    @Inject
    private SharedFlows sharedFlows;
    @Inject
    private UnsupportedNodeFlows unsupportedNodeFlows;
    @Inject
    private UnsupportedNodeTestSteps unsupportedNodeTestSteps;
    @Inject
    private NodeIntegrationGNBDUFunction nodeIntegrationGNBDUFunction;

    @BeforeSuite(alwaysRun = true, groups = { "NSS" })
    @Parameters({ "netsimInitVals", "createGNBDUFunction" })
    private void beforeSuite(@Optional final String netsimInitVals, @Optional() final String createGNBFunction) {
        CommonDataSources.initializeDataSources();

        createUsers(); // create all users include ri and nr-cran users
        createNodes(); // nodes added and synced pre-ri
        runUpgradeIndependence(netsimInitVals); // run ri to bring in support for Shared-CNF nodes
        getInfoMO();
        createGNBDUFunction(Boolean.parseBoolean(createGNBFunction));
        modifyMO();
        warmUpNetEx(); // this might need to be changed to run nr-cran specific queries
        cleanExistingCollections(false);
    }

    @AfterSuite(alwaysRun = true, groups = { "NSS" })
    @Parameters({"createGNBDUFunction" })
    private void tearDown(@Optional() final String createGNBFunction) {
        deleteGNBDUFunction(Boolean.parseBoolean(createGNBFunction));
        cleanExistingCollections(true);
        deleteUpgradeIndependenceDeployedModels();
        deleteNodes();
        deleteUsers();
    }

    @TestId(id = "TORF-485260", title = "Execute NR-CloudRan python script")
    @Test(groups = { "NSS" }, description = "Check that NR-CloudRan topology successfully created")
    public void executeNrCloudRanScript() {
        final TestScenario scenario = scenario(
                "Execute NR-CloudRan Script")
                .addFlow(loginLogoutRestFlows.loginWithUserName(NRCRAN_USER))
                .addFlow(nrCloudRanTestFlow.navigateToScriptingVm())
                .addFlow(nrCloudRanTestFlow.executeMainScript())
                .addFlow(nrCloudRanTestFlow.verifySystemDefinedCollections())
                .addFlow(loginLogoutRestFlows.logout())
                .build();
        startScenario(scenario);
    }

    private void createUsers() {
        log.info("Executing 'Create Users' Scenario");
        startScenario(scenario("Create users and cleanup any existing user data")
                .addFlow(idmCleanupFlows.cleanUp(EnmObjectType.USER))
                .addFlow(idmCleanupFlows.cleanUp(EnmObjectType.ROLE))
                .addFlow(roleManagementFlows.createRole())
                .addFlow(userManagementFlows.createUser())
                .addFlow(loginLogoutRestFlows.logout())
                .build());
    }

    private void deleteUsers() {
        log.info("Executing 'Delete Users' Scenario");

        startScenario(scenario("Delete users Scenario")
                .addFlow(idmCleanupFlows.cleanUp(EnmObjectType.USER))
                .addFlow(idmCleanupFlows.cleanUp(EnmObjectType.ROLE))
                .withDefaultVusers(1)
                .build());
    }

    private void createNodes() {
        log.info("Executing 'Create Nodes' Scenario");

        startScenario(scenario("Start, Add and Sync nodes")
                .addFlow(loginLogoutRestFlows.loginDefaultUser())
                .addFlow(flow("Start a Node")
                .addTestStep(annotatedMethod(networkElementTestSteps, NetworkElementTestSteps.StepIds.START_NODE))
                .withDataSources(dataSource(CommonDataSources.NODES_TO_ADD))
                .build())
                .addFlow(flow("Node agnostic add & sync node")
                .addSubFlow(nodeIntegrationFlows.addNode())
                .addSubFlow(nodeIntegrationFlows.syncNode())
                .withDataSources(dataSource(CommonDataSources.NODES_TO_ADD))
                .build())
                .addFlow(loginLogoutRestFlows.logout())
                .build());
    }

    private void deleteNodes() {
        log.info("Executing 'Delete Nodes' Scenario");

        startScenario(scenario("Delete nodes")
                .addFlow(loginLogoutRestFlows.loginDefaultUser())
                .addFlow(flow("Node agnostic delete node")
                .addSubFlow(nodeIntegrationFlows.deleteNode())
                .withDataSources(dataSource(CommonDataSources.ADDED_NODES))
                .build())
                .addFlow(loginLogoutRestFlows.logout())
                .withExceptionHandler(ScenarioExceptionHandler.LOGONLY)
                .withDefaultVusers(1)
                .build());
    }

    private void runUpgradeIndependence(final String netsimInitVals) {
        log.info("Executing 'Upgrade Independence' Scenario");

        setupUpgradeIndependence(netsimInitVals);

        startScenario(scenario("Upgrade Independence scenario")
                .addFlow(loginLogoutRestFlows.loginDefaultUser())
                .addFlow(unsupportedNodeFlows.initMultipleNodes())  // sets up node map
                .addFlow(unsupportedNodeFlows.selectMultipleNodes()) // selects nodes that ready for support
                .addFlow(unsupportedNodeFlows.learnSynchedNodes()) // finds synced node in treat as
                .addFlow(unsupportedNodeFlows.verifyCandidated())
                .addFlow(unsupportedNodeFlows.addSupportAndDeployForMultipleNodeVersion()) // does the heavy lifting
                .addFlow(unsupportedNodeFlows.waitForDeploy()) //delay to download the models
                .addFlow(unsupportedNodeFlows.verifyNodeVersionIsAdded())
                .addFlow(unsupportedNodeFlows.deploymentSuccessfullyDone())  // sets a property to say done
                .addFlow(unsupportedNodeFlows.verifyNodeIsAdded())
                .addFlow(flow("Wait until the node is sync after Upgrade Independence")
                        //Flow to check that the nodes are sync or waiting until are sync
                        .addSubFlow(nodeIntegrationFlows.verifySynchNode())
                        .withDataSources(dataSource(CommonDataSources.ADDED_NODES))
                        .build())
                .addFlow(loginLogoutRestFlows.logout())
                .build());
    }

    private void setupUpgradeIndependence(final String netsimInitVals) {
        log.info("*** Setup Scenario Started ***");

        setDataHandlerInitialValues();

        // set rest version V5 for RI
        RestApiUri.setRestV5_0_0();
        log.info("*** REST api version used is {} ***", RestApiUri.restVersion);

        learnFromCreatedNodes();

        log.info("PROPERTY groups: {}", GROUPS_PROP); // Not used at the moment?
        log.info("PROPERTY mode: {}", MODE_PROP); // use to configure if we're in vapp or long loop
        log.info("PROPERTY excludeNeType: {}", EXCLUDED_NE_TYPES);

        final boolean isVapp = MODE_PROP.contains("isVapp");
        log.info("PROPERTY isVapp: {}", isVapp);
        sharedFlows.createDataSourcesForTests(isVapp, false, false, netsimInitVals);

        deleteUpgradeIndependenceDeployedModels();

        log.info("*** Setup Scenario Completed ***");
    }

    private void deleteUpgradeIndependenceDeployedModels() {
        log.info("Executing 'Delete UpgradeIndependence Deployed Models' Scenario");
        final TestScenarioBuilder scenarioBuilder = scenario("Undeploy Models")
                .addFlow(loginLogoutRestFlows.loginDefaultUser())
                .addFlow(flow("Undeploying Models")
                        .addTestStep(annotatedMethod(unsupportedNodeTestSteps, DELETE_MODELS_IF_DEPLOYED))
                        .withDataSources(dataSource(NODES_TO_ADD)
                                .withFilter(new FilterNodesToAdd(false, false, false, EXCLUDED_NE_TYPES))
                                .allowEmpty())
                        .build())
                .addFlow(flow("Checking for Undeploy being successful")
                        .addTestStep(annotatedMethod(unsupportedNodeTestSteps, DELAY_STEP))
                        .addTestStep(annotatedMethod(unsupportedNodeTestSteps, VERIFY_VERSION_HAS_BEEN_UNDEPLOYED))
                        .addTestStep(annotatedMethod(unsupportedNodeTestSteps, VERIFY_NODE_IS_AGAIN_UNSUPPORTED))
                        .withDataSources(dataSource(NODES_TO_ADD)
                                .withFilter(new FilterNodesToAdd(false, false, false, "RadioNode"))
                                .allowEmpty()).build())
                .addFlow(flow("Sync After Removal of RI Models").addSubFlow(nodeIntegrationFlows.syncNode())
                        .withDataSources(dataSource(NODES_TO_ADD))
                        .build())
                .addFlow(loginLogoutRestFlows.logout());

        executeNonStopScenario(scenarioBuilder);
    }

    private void warmUpNetEx() {
        log.info("Executing 'Warm up NetworkExplorer' Scenario");

        startScenario(scenario("Warm up NetworkExplorer cache Scenario")
                .addFlow(networkExplorerFlows.clearSessionAttributes())
                .addFlow(loginLogoutRestFlows.loginDefaultUser())
                .addFlow(networkExplorerFlows.populateBrowser())
                .addFlow(networkExplorerFlows.netexModelServiceWarmup())
                .addFlow(loginLogoutRestFlows.logout())
                .build());
    }

    private void cleanExistingCollections(final boolean inTearDown) {
        log.info("Executing 'Clean existing Collections' Scenario");

        final TestScenarioBuilder cleanExistingCollectionsScenario = scenario(
                String.format("Clean up any Collections and Topologies created by %s user", NRCRAN_USER))
                .addFlow(loginLogoutRestFlows.loginWithUserName(NRCRAN_USER))
                .addFlow(nrCloudRanTestFlow.removeNrCloudRanSpecificCollections());

        if (inTearDown) {
            cleanExistingCollectionsScenario
                .addFlow(nrCloudRanTestFlow.removeNrCloudRanSecureKeyset())
                .addFlow(loginLogoutRestFlows.logout())
                .addFlow(nrCloudRanTestFlow.cleanUpHttpClient());
        } else {
            cleanExistingCollectionsScenario.addFlow(loginLogoutRestFlows.logout());
        }

        startScenario(cleanExistingCollectionsScenario.withExceptionHandler(ScenarioExceptionHandler.LOGONLY)
                .withDefaultVusers(1)
                .build());
    }

    /**
     * Populates NickNameConverter which is used later to build data-sources.
     */
    private void learnFromCreatedNodes() {
        log.info("Executing 'Learn from created Nodes' Scenario");

        final TestScenarioBuilder scenarioBuilder = scenario("Learn from created Nodes")
                .addFlow(loginLogoutRestFlows.loginDefaultUser())
                .addFlow(flow("Learning Nickname")
                    .withDataSources(dataSource(ADDED_NODES).allowEmpty())
                    .addTestStep(annotatedMethod(unsupportedNodeTestSteps, LEARNING_NICK_NAME))
                    .addTestStep(annotatedMethod(unsupportedNodeTestSteps, LEARN_FROM_NODE))
                    .build())
                .addFlow(loginLogoutRestFlows.logout());
        executeScenario(scenarioBuilder);
    }

    private void createGNBDUFunction(final boolean createGNBFunction) {
        if (createGNBFunction) {
            executeScenario(createMO());
        }
    }

    private void deleteGNBDUFunction(final boolean createGNBFunction) {
        if (createGNBFunction && nodeIntegrationGNBDUFunction.isCreatedGNBDUFunction()) {
            executeScenario(removeMO());
        }
    }

    private void getInfoMO() {
        executeScenario(infoMo());
    }

    private void modifyMO() {
        executeScenario(editMO());
    }

    public TestScenarioBuilder createMO() {
        return scenario("Create GNBDU Function")
                .addFlow(loginLogoutRestFlows.loginDefaultUser())
                .addFlow(flow("Create GNBDUFunction")
                        .addTestStep(TestScenarios.annotatedMethod(nodeIntegrationGNBDUFunction,
                                NodeIntegrationGNBDUFunction.StepIds.CREATE_MO))
                        .withDataSources(dataSource(ADDED_NODES).withFilter(filtervDU)))
                .addFlow(loginLogoutRestFlows.logout());
    }

    public TestScenarioBuilder removeMO() {
        return scenario("Remove GNBDU Function")
                .addFlow(loginLogoutRestFlows.loginDefaultUser())
                .addFlow(flow("Remove GNBDUFunction")
                        .addTestStep(TestScenarios.annotatedMethod(nodeIntegrationGNBDUFunction,
                                NodeIntegrationGNBDUFunction.StepIds.REMOVE_MO))
                        .withDataSources(dataSource(ADDED_NODES)))
                .addFlow(loginLogoutRestFlows.logout());
    }

    public TestScenarioBuilder infoMo() {
        return scenario("MO Info")
                .addFlow(loginLogoutRestFlows.loginDefaultUser())
                .addFlow(flow("Take gNBDUId from the vCUCP node")
                        .addTestStep(TestScenarios.annotatedMethod(nodeIntegrationGNBDUFunction,
                                NodeIntegrationGNBDUFunction.StepIds.GET_GNBDU_ID)
                                .withParameter(FDN, COMMA + MANAGED_ELEMENT_EQUAL_1 + COMMA
                                        + GNB_CUCP_FUNCTION_EQUAL_1 + COMMA + TERM_POINT_TO_GNBDU_EQUAL_1))
                        .withDataSources(dataSource(ADDED_NODES).withFilter(filtervCUCP)))
                .addFlow(loginLogoutRestFlows.logout());
    }

    public TestScenarioBuilder editMO() {
        return scenario("Edit MO")
                .addFlow(loginLogoutRestFlows.loginDefaultUser())
                .addFlow(flow("Set gNBId for vCUCP node")
                        .addTestStep(TestScenarios.annotatedMethod(nodeIntegrationGNBDUFunction,
                                NodeIntegrationGNBDUFunction.StepIds.EDIT_MO)
                                .withParameter(ATTRIBUTE_CHANGE, GNBID_ATTRIBUTE)
                                .withParameter(NEW_VALUE, GNBID_VALUE)
                                .withParameter(FDN, COMMA + MANAGED_ELEMENT_EQUAL_1 + COMMA + GNB_CUCP_FUNCTION_EQUAL_1))
                        .withDataSources(dataSource(ADDED_NODES).withFilter(filtervCUCP)))
                .addFlow(flow("Set gNBDUId for Radio node")
                        .addTestStep(TestScenarios.annotatedMethod(nodeIntegrationGNBDUFunction,
                                NodeIntegrationGNBDUFunction.StepIds.EDIT_MO)
                                .withParameter(ATTRIBUTE_CHANGE, GNBDUID_ATTRIBUTE)
                                .withParameter(NEW_VALUE, nodeIntegrationGNBDUFunction.getGnbduid())
                                .withParameter(FDN, COMMA + GNB_DU_FUNCTION_EQUAL_1))
                        .withDataSources(dataSource(ADDED_NODES).withFilter(filterRadioNode)))
                .addFlow(flow("Set gNBId for RadioNode")
                        .addTestStep(TestScenarios.annotatedMethod(nodeIntegrationGNBDUFunction,
                                        NodeIntegrationGNBDUFunction.StepIds.EDIT_MO)
                                .withParameter(ATTRIBUTE_CHANGE, GNBID_ATTRIBUTE)
                                .withParameter(NEW_VALUE, GNBID_VALUE)
                                .withParameter(FDN, COMMA + GNB_DU_FUNCTION_EQUAL_1))
                        .withDataSources(dataSource(ADDED_NODES).withFilter(filterRadioNode)))
                .addFlow(flow("Set values for vDU nodes")
                        .addTestStep(TestScenarios.annotatedMethod(nodeIntegrationGNBDUFunction,
                                NodeIntegrationGNBDUFunction.StepIds.EDIT_MO)
                                .withParameter(ATTRIBUTE_CHANGE, GNBDUID_ATTRIBUTE)
                                .withParameter(NEW_VALUE, nodeIntegrationGNBDUFunction.getGnbduid())
                                .withParameter(FDN, COMMA + MANAGED_ELEMENT_EQUAL_1 + COMMA + GNB_DU_FUNCTION
                                        + EQUAL + nodeIntegrationGNBDUFunction.getGnbduid()))
                        .addTestStep(TestScenarios.annotatedMethod(nodeIntegrationGNBDUFunction,
                                NodeIntegrationGNBDUFunction.StepIds.EDIT_MO)
                                .withParameter(ATTRIBUTE_CHANGE, GNBID_ATTRIBUTE)
                                .withParameter(NEW_VALUE, GNBID_VALUE)
                                .withParameter(FDN, COMMA + MANAGED_ELEMENT_EQUAL_1 + COMMA + GNB_DU_FUNCTION
                                        + EQUAL + nodeIntegrationGNBDUFunction.getGnbduid()))
                        .addTestStep(TestScenarios.annotatedMethod(nodeIntegrationGNBDUFunction,
                                NodeIntegrationGNBDUFunction.StepIds.EDIT_MO)
                                .withParameter(ATTRIBUTE_CHANGE, GNBIDLENGTH_ATTRIBUTE)
                                .withParameter(NEW_VALUE, GNBIDLENGTH_VALUE)
                                .withParameter(FDN, COMMA + MANAGED_ELEMENT_EQUAL_1 + COMMA + GNB_DU_FUNCTION
                                        + EQUAL + nodeIntegrationGNBDUFunction.getGnbduid()))
                        .withDataSources(dataSource(ADDED_NODES).withFilter(filtervDU)))
                .addFlow(loginLogoutRestFlows.logout());
    }

    private static void setDataHandlerInitialValues() {
        final String upgIndRndKeyValue = String.format("%05d", System.nanoTime() % 100000);
        setDataHandlerInitVal(UPGRADE_INDEPENDENCE_RND_KEY, upgIndRndKeyValue);
        setDataHandlerInitVal(UPGRADE_INDEPENDENCE_CNT_KEY, "0");
        setDataHandlerInitVal(UPGRADE_INDEPENDENCE_CNT_KEY_DPL, "0");
        setDataHandlerInitVal(UPGRADE_INDEPENDENCE_REPEATABILITY_KEY, UPGRADE_INDEPENDENCE_POSTPONED_TESTS_NOT_EXECUTED);
        setDataHandlerInitVal(UPGRADE_INDEPENDENCE_FLUSH_KEY, UPGRADE_INDEPENDENCE_POSTPONED_TESTS_NOT_EXECUTED);
        setDataHandlerInitVal(UPGRADE_INDEPENDENCE_UNDEPLOY_KEY, UPGRADE_INDEPENDENCE_POSTPONED_TESTS_NOT_EXECUTED);
        setDataHandlerInitVal(UPGRADE_INDEPENDENCE_DOWNLOAD_MODEL_KEY, UPGRADE_INDEPENDENCE_POSTPONED_TESTS_NOT_EXECUTED);
        setDataHandlerInitVal(UPGRADE_INDEPENDENCE_RECANDIDATE_KEY, UPGRADE_INDEPENDENCE_POSTPONED_TESTS_NOT_EXECUTED);
        setDataHandlerInitVal(UPGRADE_INDEPENDENCE_BATCH_SIZE_KEY, "0");
    }

    private static void setDataHandlerInitVal(final String key, final String val) {
        if (DataHandler.getAttribute(key) == null) {
            DataHandler.setAttribute(key, val);
            log.warn("Re-setting {} to {}", key, val);
        }
    }

    private static void executeScenario(final TestScenarioBuilder scenarioBuilder) {
        final TestScenario scenario;
        scenario = scenarioBuilder.build();
        runner = runner().withListener(new LoggingScenarioListener()).build();
        try {
            runner.start(scenario);
        } catch (final Exception ignore) {
            /* Print stack trace */
            assertThat("Failure inside scenario", true, Matchers.is(false));
        }
    }

    private static void executeNonStopScenario(final TestScenarioBuilder scenarioBuilder) {
        final TestScenario scenario;
        scenario = scenarioBuilder.build();
        runner = runner().withDefaultExceptionHandler(ScenarioExceptionHandler.IGNORE)
            .withListener(new LoggingScenarioListener()).build();
        try {
            runner.start(scenario);
        } catch (final Exception ignore) {
            /* Print stack trace */
            ignore.printStackTrace();
        }
    }

    private static void startScenario(final TestScenario scenario) {
        runner.start(scenario);
    }
}

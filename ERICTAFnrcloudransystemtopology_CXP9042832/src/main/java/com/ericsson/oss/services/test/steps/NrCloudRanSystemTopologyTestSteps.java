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

package com.ericsson.oss.services.test.steps;

import static com.ericsson.oss.testware.enmbase.data.CommonDataSources.USERS_TO_CREATE;

import java.io.IOException;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.ericsson.cifwk.taf.TestContext;
import com.ericsson.cifwk.taf.annotations.Attachment;
import com.ericsson.cifwk.taf.annotations.Input;
import com.ericsson.cifwk.taf.annotations.TestStep;
import com.ericsson.cifwk.taf.tools.http.HttpResponse;
import com.ericsson.cifwk.taf.tools.http.HttpTool;
import com.ericsson.de.tools.cli.CliIntermediateResult;
import com.ericsson.de.tools.cli.CliToolShell;
import com.ericsson.de.tools.cli.CliTools;
import com.ericsson.de.tools.cli.WaitConditions;
import com.ericsson.oss.testware.enmbase.UserSession;
import com.ericsson.oss.testware.enmbase.data.ENMUser;
import com.ericsson.oss.testware.hostconfigurator.HostConfigurator;

/**
 * Class responsible for providing NR-CLOUD-RAN Test Steps.
 */
public class NrCloudRanSystemTopologyTestSteps {
    private static final String SCRIPT_COMPLETE = "NR-CRAN topology completed successfully.";
    private static final String CUSTOM_TOPOLOGY_ENDPOINT = "/object-configuration/custom-topology/v1";
    private static final String COLLECTIONS_V1_ENDPOINT = "/object-configuration/v1/collections";
    private static final String COLLECTIONS_V2_ENDPOINT = "/object-configuration/collections/v2";
    private static final String NAVIGATE_TO_NR_CLOUD_RAN_CMD = "cd /opt/ericsson/nrcloudransystemtopology";
    private static final String NAVIGATE_TO_KEYSET = "cd /ericsson/tor/no_rollback/nrcloudransystemtopology/secure";
    private static final String READ_LOG = "cat /opt/ericsson/nrcloudransystemtopology/log/nrcran_log";
    private static final String EXPECTED = "Expected %s to be created";
    private static final String NRCLOUDRAN = "NR-CLOUD-RAN";
    private static final String VDU = "*vDU*";
    private static final String VCUCP = "*vCUCP*";
    private static final String RADIONODE = "*Radio*";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Inject
    private TestContext testContext;

    private CliToolShell shell;
    private UserSession userSession;
    private HttpTool httpTool;

    private final ObjectMapper mapper = new ObjectMapper();

    private boolean test;

    /**
     * SSH into the scripting vm using the nrcloudran users credentials.
     *
     * @param users ENMUser
     */
    @TestStep(id = TestStepIds.SSH_AS_NRCLOUDRAN)
    public void sshAsNrCloudRan(@Input(USERS_TO_CREATE) final ENMUser users) {
        if (!test) {
            shell = CliTools.sshShell(HostConfigurator.getScriptingVm().getIp())
                    .withUsername(users.getUsername())
                    .withPassword(users.getPassword())
                    .build();
            shell.execute(NAVIGATE_TO_NR_CLOUD_RAN_CMD);
            test = true;
        }
    }

    /**
     * Executes the Main Script, entering the password when prompted.
     *
     * @param users ENMUser
     */
    @TestStep(id = TestStepIds.EXECUTE_MAIN_SCRIPT)
    public void executeMainScript(@Input(USERS_TO_CREATE) final ENMUser users) {
        CliIntermediateResult scriptOutput = null;
        try {
            shell.writeLine("python main.py", WaitConditions.substring("Password:", 60));
            shell.writeLine(users.getPassword(), WaitConditions.substring("Confirm password:", 60));
            scriptOutput = shell.writeLine(users.getPassword(), WaitConditions.substring(SCRIPT_COMPLETE, 300));
        } finally {
            attachLog();
            final String attachOutput = attachOutput(scriptOutput);
            logger.debug("AttachOutput {}", attachOutput);
        }
    }

    /**
     * Attach NR-CRAN script output to allure report.
     *
     * @param scriptOutput The NR-CRAN script output
     */
    @Attachment(value = "nrcran_output.txt", type = "text/plain")
    private String attachOutput(@NotNull final CliIntermediateResult scriptOutput) {
        if (scriptOutput != null) {
            return scriptOutput.getOutput();
        }
        return "Error occurred while retrieving script output";
    }

    /**
     * Attach NR-CRAN log to allure report.
     */
    @Attachment(value = "nrcran_log.txt", type = "text/plain")
    private String attachLog() {
        return shell.execute(READ_LOG).getOutput();
    }

    /**
     * Removes the nrcloudran secure file that could otherwise cause conflict at the next execution.
     */
    @TestStep(id = TestStepIds.REMOVE_NRCLOUDRAN_KEY)
    public void removeNrCloudRanKey() {
        shell.execute(NAVIGATE_TO_KEYSET);
        shell.execute("rm -f nrclouduser");
    }

    /**
     * Verify the System Defined Collections exist.
     */
    @TestStep(id = TestStepIds.CHECK_SYSTEM_DEFINED_COLLECTIONS)
    public void checkSystemDefinedCollections() {
        Assert.assertTrue(isNrCloudRanTopologyCreated(), String.format(EXPECTED, NRCLOUDRAN));
        Assert.assertTrue(isSystemDefinedCollectionCreated(VDU), String.format(EXPECTED, VDU));
        Assert.assertTrue(isSystemDefinedCollectionCreated(VCUCP), String.format(EXPECTED, VCUCP));
        Assert.assertTrue(isSystemDefinedCollectionCreated(RADIONODE), String.format(EXPECTED, RADIONODE));
    }

    /**
     * Delete all collections created via NR-NSA execution.
     */
    @TestStep(id = TestStepIds.REMOVE_NRCLOUDRAN_COLLECTIONS)
    public void removeNrnsaCollections() {
        try {
            deleteSystemCreatedCollections(VDU);
            deleteSystemCreatedCollections(RADIONODE);
            deleteSystemCreatedCollections(VCUCP);
            deleteNrnsa();
        } catch (final Exception a) {
            logger.error(a.getMessage());
        }
    }

    /**
     * Delete NR NSA Topology.
     *
     * @throws IOException IO Exception
     */
    private void deleteNrnsa() throws IOException {
        userSession = testContext.getAttribute(UserSession.SESSION);
        httpTool = userSession.getTool().getAs(HttpTool.class);
        final String query = CUSTOM_TOPOLOGY_ENDPOINT + "?customTopology=true";
        logger.debug("Query: {}", query);
        final HttpResponse response = httpTool.get(query);
        final JsonNode responseData = mapper.readTree(response.getBody());
        for (final JsonNode collection : responseData) {
            if (collection.get("name").getTextValue().equals(NRCLOUDRAN)) {
                final String id = collection.get("id").getTextValue();
                deleteNrnsaCollections(id);
                final HttpResponse responseDelete = httpTool.delete(CUSTOM_TOPOLOGY_ENDPOINT + "/" + id);
                logger.debug("HttpResponse DELETE {}", responseDelete);
            }
        }
    }

    /**
     * This will delete all collections under the NR-NSA Topology.
     *
     * @param parentId The id of the NR-NSA Topology
     * @throws IOException Exception
     */
    private void deleteNrnsaCollections(final String parentId) throws IOException {
        final String query = CUSTOM_TOPOLOGY_ENDPOINT + "?parentId=" + parentId;
        logger.debug("Query={}", query);
        final HttpResponse response = httpTool.get(query);
        final JsonNode responseData = mapper.readTree(response.getBody());
        for (final JsonNode collection : responseData) {
            final String id = collection.get("id").getTextValue();
            final HttpResponse responseDelete = httpTool.delete(CUSTOM_TOPOLOGY_ENDPOINT + "/" + id);
            logger.debug("HttpResponse DELETE {}", responseDelete);
        }
    }

    /**
     * Delete a system defined collection by name.
     *
     * @throws IOException Exception
     */
    private void deleteSystemCreatedCollections(final String collectionName) throws IOException {
        userSession = testContext.getAttribute(UserSession.SESSION);
        httpTool = userSession.getTool().getAs(HttpTool.class);
        final HttpResponse response = httpTool.get(COLLECTIONS_V2_ENDPOINT + "?collectionName=" + collectionName);
        final JsonNode responseData = mapper.readTree(response.getBody());
        for (final JsonNode collection : responseData.get("collections")) {
            if (collection.get("name").getTextValue().contains(collectionName.replace("*",  ""))) {
                final String id = collection.get("id").getTextValue();
                final HttpResponse responseDelete = httpTool.delete(COLLECTIONS_V1_ENDPOINT + "/" + id);
                logger.debug("HttpResponse DELETE {}", responseDelete);
            }
        }
    }

    /**
     * Check if a Topology called NR-CRAN is created by using the get custom topologies endpoint.
     *
     * @return true if a NR-CRAN topology exists
     */
    private boolean isNrCloudRanTopologyCreated() {
        userSession = testContext.getAttribute(UserSession.SESSION);
        httpTool = userSession.getTool().getAs(HttpTool.class);
        final HttpResponse response = httpTool.get(CUSTOM_TOPOLOGY_ENDPOINT + "?customTopology=true");
        logger.debug("HttpResponse {}", response);
        return response.getBody().contains(NRCLOUDRAN);
    }

    /**
     * Check if a collection is created by using the get by name endpoint.
     *
     * @return true if a collection with the passed name exists
     */
    private boolean isSystemDefinedCollectionCreated(final String collectionName) {
        userSession = testContext.getAttribute(UserSession.SESSION);
        httpTool = userSession.getTool().getAs(HttpTool.class);
        final HttpResponse response = httpTool.get(COLLECTIONS_V2_ENDPOINT + "?collectionName=" + collectionName);
        logger.debug("HttpResponse {}", response);
        return response.getBody().contains(collectionName.replace("*", ""));
    }

    /**
     * Clean up the Http Tool, removing cookies and attributes.
     */
    @TestStep(id = TestStepIds.CLEANUP_HTTP_CLIENT)
    public void cleanUpHttpClient() {
        testContext.clearAttributes();
        httpTool.clearCookies();
        httpTool.close();
    }

    /**
     * Class of Test Step Id constants.
     */
    public static final class TestStepIds {
        public static final String SSH_AS_NRCLOUDRAN = "sshAsNrCloudRan";
        public static final String EXECUTE_MAIN_SCRIPT = "executeMainScript";
        public static final String CHECK_SYSTEM_DEFINED_COLLECTIONS = "checkSystemDefinedCollections";
        public static final String CLEANUP_HTTP_CLIENT = "cleanUpHttpClient";
        public static final String REMOVE_NRCLOUDRAN_COLLECTIONS = "removeNrCloudRanCollections";
        public static final String REMOVE_NRCLOUDRAN_KEY = "removeNrCloudRankey";

        private TestStepIds() {
        }
    }
}

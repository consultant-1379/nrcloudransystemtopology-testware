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

package com.ericsson.oss.services.test.flows;

import static com.ericsson.cifwk.taf.scenario.TestScenarios.*;
import static com.ericsson.oss.testware.enmbase.data.CommonDataSources.USERS_TO_CREATE;

import javax.inject.Inject;

import com.ericsson.cifwk.taf.datasource.DataRecord;
import com.ericsson.cifwk.taf.scenario.TestStepFlow;
import com.ericsson.oss.services.test.steps.NrCloudRanSystemTopologyTestSteps;
import com.google.common.base.Predicate;

/**
 * Class responsible for providing Nrnsa Systems Topology Test Steps.
 */
public class NrCloudRanSystemTopologyTestFlow {

    private static final Predicate<DataRecord> nrCloudRanUser = new Predicate<DataRecord>() {
        @Override
        public boolean apply(final DataRecord user) {
            if (user != null) {
                return "nrclouduser".equals(user.getFieldValue("username"));
            }
            return false;
        }

        @Override
        public boolean test(final DataRecord input) {
            return apply(input);
        }
    };

    @Inject
    private NrCloudRanSystemTopologyTestSteps nrCloudRanTestSteps;

    /**
     * SSH's from the management server to the Scripting VM.
     * <p>
     * This Flow has the following Test Steps
     * <ul>
     * <li>{@link NrCloudRanSystemTopologyTestSteps#sshAsNrCloudRan(ENMUser)} ()}}</li>
     * </ul>
     *
     * @return {@link TestStepFlow} a flow which sshs from management server to the scripting server
     */
    public TestStepFlow navigateToScriptingVm() {
        return flow("SSH to the scripting vm from the management server")
                .addTestStep(annotatedMethod(nrCloudRanTestSteps, NrCloudRanSystemTopologyTestSteps.TestStepIds.SSH_AS_NRCLOUDRAN))
                .withDataSources(dataSource(USERS_TO_CREATE).withFilter(nrCloudRanUser))
                .build();
    }

    /**
     * Execute the main python script for NR-CRAN.
     * <p>
     * This Flow has the following Test Steps
     * <ul>
     * <li>{@link NrCloudRanSystemTopologyTestSteps#executeMainScript(ENMUser)} ()}}</li>
     * </ul>
     *
     * @return {@link TestStepFlow} a flow which will execute the main script
     */
    public TestStepFlow executeMainScript() {
        return flow("execute the main python script for NR-NSA")
                .addTestStep(annotatedMethod(nrCloudRanTestSteps, NrCloudRanSystemTopologyTestSteps.TestStepIds.EXECUTE_MAIN_SCRIPT))
                .withDataSources(dataSource(USERS_TO_CREATE).withFilter(nrCloudRanUser))
                .build();
    }

    /**
     * Get the System Defined Collections.
     * <p>
     * This Flow has the following Test Steps
     * <ul>
     * <li>{@link NrCloudRanSystemTopologyTestSteps#checkSystemDefinedCollections()} ()}}</li>
     * </ul>
     *
     * @return {@link TestStepFlow} a flow which will get the system defined collections
     */
    public TestStepFlow verifySystemDefinedCollections() {
        return flow("Get system defined collections")
                .addTestStep(annotatedMethod(nrCloudRanTestSteps, NrCloudRanSystemTopologyTestSteps.TestStepIds.CHECK_SYSTEM_DEFINED_COLLECTIONS))
                .build();
    }

    /**
     * Clean up the HttpTools cookies and attributes when finished.
     * <p>
     * This Flow has the following Test Steps
     * <ul>
     * <li>{@link NrCloudRanSystemTopologyTestSteps#cleanUpHttpClient()}}</li>
     * </ul>
     *
     * @return {@link TestStepFlow} a flow which will clear the cookies and attributes from the HttpTool
     */
    public TestStepFlow cleanUpHttpClient() {
        return flow("Clear HTTP Cookies and end connection")
                .addTestStep(annotatedMethod(nrCloudRanTestSteps, NrCloudRanSystemTopologyTestSteps.TestStepIds.CLEANUP_HTTP_CLIENT))
                .build();
    }

    /**
     * Delete any collection that has been created by the NR-CRAN script.
     * <p>
     * This Flow has the following Test Steps
     * <ul>
     * <li>{@link NrCloudRanSystemTopologyTestSteps#removeNrnsaCollections()}}</li>
     * </ul>
     *
     * @return {@link TestStepFlow} a flow which will remove all of the collections associated with NR-NSA
     */
    public TestStepFlow removeNrCloudRanSpecificCollections() {
        return flow("Remove NR-CRAN Specific Collections")
                .addTestStep(annotatedMethod(nrCloudRanTestSteps, NrCloudRanSystemTopologyTestSteps.TestStepIds.REMOVE_NRCLOUDRAN_COLLECTIONS))
                .build();
    }

    /**
     * Delete the keyset and stored password for the NR-CRAN user.
     * <p>
     * This Flow has the following Test Steps
     * <ul>
     * <li>{@link NrCloudRanSystemTopologyTestSteps#removeNrCloudRanKey()}}</li>
     * </ul>
     *
     * @return {@link TestStepFlow} a flow which will remove all of the collections associated with NR-NSA
     */
    public TestStepFlow removeNrCloudRanSecureKeyset() {
        return flow("Remove the Keyset and secure file generated for the NR-CRAN user")
                .addTestStep(annotatedMethod(nrCloudRanTestSteps, NrCloudRanSystemTopologyTestSteps.TestStepIds.REMOVE_NRCLOUDRAN_KEY))
                .build();
    }
}

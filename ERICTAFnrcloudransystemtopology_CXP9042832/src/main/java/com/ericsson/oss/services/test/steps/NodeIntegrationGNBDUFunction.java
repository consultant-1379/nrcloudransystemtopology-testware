/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.test.steps;

import static org.junit.Assert.assertTrue;

import static com.ericsson.oss.services.test.utils.Constant.*;

import java.util.Iterator;
import java.util.Objects;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.cifwk.taf.annotations.Input;
import com.ericsson.cifwk.taf.annotations.TestStep;
import com.ericsson.oss.services.test.utils.Constant;
import com.ericsson.oss.testware.enm.cli.EnmCliResponse;
import com.ericsson.oss.testware.enmbase.data.CommonDataSources;
import com.ericsson.oss.testware.enmbase.data.NetworkNode;
import com.ericsson.oss.testware.nodeintegration.common.ManagedObject;
import com.ericsson.oss.testware.nodeintegration.operators.impl.NodeIntegrationOperatorCbpOi;
import com.ericsson.oss.testware.nodeintegration.utilities.CmEditorCommandUtil;
import com.ericsson.oss.testware.nodeintegration.utilities.NamingUtil;
import com.ericsson.oss.testware.security.authentication.tool.TafToolProvider;
import com.google.common.base.Preconditions;

/**
 * NodeIntegrationGNBDUFunction class.
 */
public class NodeIntegrationGNBDUFunction extends NodeIntegrationOperatorCbpOi {
    private static final Logger logger = LoggerFactory.getLogger(NodeIntegrationGNBDUFunction.class);
    private static final String GNBDUFUNCTION = "GNBDUFunction";
    private static final String GNBDUFUNCTION_ID = "Id";
    private static final String GNBIDLENGTH_ATTRIBUTE = "gNBIdLength";
    private static final String GNBDUID_STRING = "gNBDUId";
    private String gnbduid;
    private boolean createdGNBDUFunction;

    @Inject
    private TafToolProvider tafToolProvider;

    public String getGnbduid() {
        return gnbduid;
    }

    public boolean isCreatedGNBDUFunction() {
        return createdGNBDUFunction;
    }

    @TestStep(id = StepIds.CREATE_MO)
    public void createGNBDUFunction(@Input(CommonDataSources.ADDED_NODES) final NetworkNode node) {
        if (!checkGNBDUFunctionExists(node, GNBDUFUNCTION)) {
            final ManagedObject managedObject = new ManagedObject(node) {
                @Override
                public String getCliAttributeList() {
                    final StringBuilder attributes = new StringBuilder();
                    attributes.append(CmEditorCommandUtil.appendAttribute(GNBDUID_ATTRIBUTE, gnbduid));
                    //The ID for gnbduid is setup from the vCUCP node, because is read only in vCUCP
                    attributes.append(CmEditorCommandUtil.appendAttribute(GNBID_ATTRIBUTE, Constant.GNBID_VALUE));
                    attributes.append(CmEditorCommandUtil.appendAttribute(GNBIDLENGTH_ATTRIBUTE, "22"));
                    attributes.append(CmEditorCommandUtil.appendLastAttribute(GNBDUFUNCTION_ID, Constant.GNBDUID_VALUE));
                    return attributes.toString();
                }

                @Override
                public String getFdn() {
                    return NamingUtil.getMeContextFdnForCppBasedNodes(ossPrefix, networkElementId).concat(COMMA + MANAGED_ELEMENT_EQUAL_1 + COMMA)
                            .concat(GNBDUFUNCTION).concat(EQUAL + gnbduid);
                }
            };
            final String command = CmEditorCommandUtil.createMo(managedObject.getFdn(),
                    managedObject.getCliAttributeList());

            final EnmCliResponse enmCliResponse = executeRestCall(command, tafToolProvider.getHttpTool());
            assertTrue("MO create Successfull, with response: " + enmCliResponse.toString(),
                    enmCliResponse.isCommandSuccessful());
            createdGNBDUFunction = true;
        } else {
            logger.debug("GNBDUFunction exists so it's not needed create it");
            createdGNBDUFunction = false;
        }
    }

    @TestStep(id = StepIds.GET_GNBDU_ID)
    public void getGnbDuId(@Input(CommonDataSources.ADDED_NODES) final NetworkNode node,
                           @Input(ParameterConstant.FDN) final String fdn) {
        final EnmCliResponse enmCliResponse = getMOInfo(node, fdn);
        assertTrue("get MO Info Successfull, with response: " + enmCliResponse.toString(),
                enmCliResponse.isCommandSuccessful());
        this.gnbduid = enmCliResponse
                .getAllDtos()
                .stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(n->n.contains(GNBDUID_STRING))
                .findFirst()
                .map(n->n.substring(n.indexOf(":") + 1).trim()).get();
        logger.debug(GNBDUID_STRING + ":" +  this.gnbduid);
    }

    @TestStep(id = StepIds.EDIT_MO)
    public void editMO(@Input(CommonDataSources.ADDED_NODES) final NetworkNode node,
                       @Input(ParameterConstant.ATTRIBUTE_CHANGE) final String attributeChange,
                       @Input(ParameterConstant.NEW_VALUE) final String newValue,
                       @Input(ParameterConstant.FDN) final String fdn) {
        final ManagedObject managedObject = new ManagedObject(node) {
            @Override
            public String getCliAttributeList() {
                final StringBuilder attributes = new StringBuilder();
                attributes.append(CmEditorCommandUtil.appendLastAttribute(attributeChange, newValue));
                return attributes.toString();
            }

            @Override
            public String getFdn() {
                if (networkElementId.contains("NodeBRadio")) {
                    return "ManagedElement=".concat(networkElementId).concat(fdn);
                } else {
                    return NamingUtil.getMeContextFdnForCppBasedNodes(ossPrefix, networkElementId).concat(fdn);
                }
            }
        };

        final String command = CmEditorCommandUtil.updateMO(managedObject.getFdn(),
                managedObject.getCliAttributeList());

        final EnmCliResponse enmCliResponse = executeRestCall(command, tafToolProvider.getHttpTool());

        assertTrue("MO edit/update Successfull, with response: " + enmCliResponse.toString(),
                enmCliResponse.isCommandSuccessful());
    }

    @TestStep(id = StepIds.REMOVE_MO)
    public void removeMO(@Input(CommonDataSources.ADDED_NODES) final NetworkNode node) {
        final ManagedObject managedObject = new ManagedObject(node) {
            @Override
            public String getCliAttributeList() {
                return null;
            }

            @Override
            public String getFdn() {
                return NamingUtil.getMeContextFdnForCppBasedNodes(ossPrefix, networkElementId).concat(COMMA + MANAGED_ELEMENT_EQUAL_1 + COMMA)
                        .concat(GNBDUFUNCTION).concat(EQUAL + GNBDUID_VALUE);
            }
        };

        final String command = CmEditorCommandUtil.deleteMo(managedObject.getFdn());

        final EnmCliResponse enmCliResponse = executeRestCall(command, tafToolProvider.getHttpTool());

        assertTrue("MO remove Successfull, with response: " + enmCliResponse.toString(),
                enmCliResponse.isCommandSuccessful());
    }

    private void checkDataSource(final NetworkNode node, final String dsName) {
        Preconditions.checkNotNull(node, "Data Source with name " + dsName + " is not specified in flow");
    }

    private EnmCliResponse getMOInfo(final NetworkNode node, final String fdn) {
        final ManagedObject managedObject = new ManagedObject(node) {
            @Override
            public String getCliAttributeList() {
                return null;
            }

            @Override
            public String getFdn() {
                return NamingUtil.getMeContextFdnForCppBasedNodes(ossPrefix, networkElementId).concat(fdn);
            }
        };
        final String command = CmEditorCommandUtil.getFdn(managedObject.getFdn());
        final EnmCliResponse enmCliResponse = executeRestCall(command, tafToolProvider.getHttpTool());
        return enmCliResponse;
    }

    private boolean checkGNBDUFunctionExists(@Input(CommonDataSources.ADDED_NODES) final NetworkNode node,
                                             @Input(ParameterConstant.FDN) final String fdn) {
        final EnmCliResponse enmCliResponse = getMOInfo(node, " " + fdn);
        int numberInstances = 0;
        if (enmCliResponse.isCommandSuccessful()) {
            final Iterator it = enmCliResponse.getAllDtos().iterator();
            while (it.hasNext()) {
                final String value = it.next().toString();
                if (value != null && value.contains("instance")) {
                    numberInstances = Integer.parseInt(value.substring(0, 1));
                    return numberInstances != 0;
                }
            }
        }
        return false;
    }

    public static final class StepIds {
        public static final String CREATE_MO = "create_mo";
        public static final String EDIT_MO = "edit_mo";
        public static final String GET_GNBDU_ID = "get_gnbdu_id";
        public static final String REMOVE_MO = "remove_mo";

        private StepIds() {
        }
    }

    public static final class ParameterConstant {
        public static final String ATTRIBUTE_CHANGE = "attributeChange";
        public static final String NEW_VALUE = "newValue";
        public static final String FDN = "FDN";

        private ParameterConstant() {
        }
    }

}

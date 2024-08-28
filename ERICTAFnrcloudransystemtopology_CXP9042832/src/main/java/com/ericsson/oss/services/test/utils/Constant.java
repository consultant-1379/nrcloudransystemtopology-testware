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

package com.ericsson.oss.services.test.utils;

import com.ericsson.cifwk.taf.datasource.DataRecord;
import com.google.common.base.Predicate;

/**
 * Constant class.
 */
public final class Constant {
    public static final String NETWORK_ELEMENT_ID = "networkElementId";
    public static final String VCUCP = "vCUCP";
    public static final String VDU = "vDU";
    public static final String NODEBRADIO = "NodeBRadio";

    public static final String GNBDUID_ATTRIBUTE = "gNBDUId";
    public static final String GNBID_ATTRIBUTE = "gNBId";
    public static final String GNBIDLENGTH_ATTRIBUTE = "gNBIdLength";
    public static final String GNBDUID_VALUE = "100";
    public static final String GNBID_VALUE = "127001";
    public static final String GNBIDLENGTH_VALUE = "22";
    public static final String COMMA = ",";
    public static final String EQUAL = "=";
    public static final String EQUAL_1 = EQUAL + "1";
    public static final String MANAGED_ELEMENT = "ManagedElement";
    public static final String MANAGED_ELEMENT_EQUAL_1 = MANAGED_ELEMENT + EQUAL_1;
    public static final String GNB_CUCP_FUNCTION_EQUAL_1 = "GNBCUCPFunction" + EQUAL_1;
    public static final String GNB_DU_FUNCTION = "GNBDUFunction";
    public static final String GNB_DU_FUNCTION_EQUAL_1 = GNB_DU_FUNCTION + EQUAL_1;
    public static final String TERM_POINT_TO_GNBDU_EQUAL_1 = "TermPointToGNBDU" + EQUAL_1;

    public static final Predicate filterRadioNode = new Predicate<DataRecord>() {
        @Override
        public boolean apply(final DataRecord nodes) {
            if (nodes != null) {
                return nodes.getFieldValue(NETWORK_ELEMENT_ID).toString().contains(NODEBRADIO);
            }
            return false;
        }

        @Override
        public boolean test(final DataRecord input) {
            return apply(input);
        }
    };

    public static final Predicate filtervDU = new Predicate<DataRecord>() {
        @Override
        public boolean apply(final DataRecord nodes) {
            if (nodes != null) {
                return nodes.getFieldValue(NETWORK_ELEMENT_ID).toString().contains(VDU);
            }
            return false;
        }

        @Override
        public boolean test(final DataRecord input) {
            return apply(input);
        }
    };
    public static final Predicate filtervCUCP = new Predicate<DataRecord>() {
        @Override
        public boolean apply(final DataRecord nodes) {
            if (nodes != null) {
                return nodes.getFieldValue(NETWORK_ELEMENT_ID).toString().contains(VCUCP);
            }
            return false;
        }

        @Override
        public boolean test(final DataRecord input) {
            return apply(input);
        }
    };

    private Constant(){
    }
}

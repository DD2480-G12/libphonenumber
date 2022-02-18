/*
 * Copyright (C) 2012 The Libphonenumber Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.i18n.phonenumbers;

import junit.framework.TestCase;

/**
 * Root class for PhoneNumberUtil tests that depend on the test metadata file.
 * <p>
 * Note since tests that extend this class do not use the normal metadata file, they should not be
 * used for regression test purposes.
 * <p>
 * Ideally the {@code phoneUtil} field (which uses test metadata) would be the only way that tests
 * need to interact with a PhoneNumberUtil instance. However as some static methods in the library
 * invoke "getInstance()" internally, we must also inject the test instance as the PhoneNumberUtil
 * singleton. This means it is unsafe to run tests derived from this class in parallel with each
 * other or at the same time as other tests which might require the singleton instance.
 *
 * @author Shaopeng Jia
 */
public class TestMetadataTestCase extends TestCase {
  private static final String TEST_METADATA_FILE_PREFIX =
      "/com/google/i18n/phonenumbers/data/PhoneNumberMetadataProtoForTesting";

  /** An instance of PhoneNumberUtil that uses test metadata. */
  protected final PhoneNumberUtil phoneUtil;

  public TestMetadataTestCase() {
    phoneUtil = new PhoneNumberUtil(new MultiFileMetadataSourceImpl(TEST_METADATA_FILE_PREFIX,
        MetadataManager.DEFAULT_METADATA_LOADER),
        CountryCodeToRegionCodeMapForTesting.getCountryCodeToRegionCodeMap());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    PhoneNumberUtil.setInstance(phoneUtil);
  }

  private boolean[] coveredBranchesOverTime = new boolean[23];
  @Override
  protected void tearDown() throws Exception {
    boolean calledFormatInOriginalFormat = phoneUtil.calledFormatInOriginalFormat;
    if (!calledFormatInOriginalFormat) {
      return;
    }
    boolean[] coveredBranches = phoneUtil.branches;
    int totalBranches = coveredBranches.length;
    int missedBranches = 0;
    for (int i = 0; i < totalBranches; i++) {
      if (!coveredBranchesOverTime[i]) {
        if (coveredBranches[i]) {
          coveredBranchesOverTime[i] = true;
        } else {
          missedBranches++;
        }
      }
    }
    int numCoveredBranches = totalBranches - missedBranches;
    int coveragePercentage = (int)(((double) numCoveredBranches / (double) totalBranches) * 100);
    StringBuilder nonCoveredBranches = new StringBuilder();
    for (int i = 0; i < totalBranches; i++) {
      if (!coveredBranchesOverTime[i]) {
        nonCoveredBranches.append(i).append(", ");
      }
    }
    System.out.println("##################################### Branch Coverage #######################################");
    System.out.println("Function: PhoneNumberUtil::formatInOriginalFormat");
    System.out.println("Total branches       " + totalBranches);
    System.out.println("Covered branches     " + numCoveredBranches);
    System.out.println("Missed branches      " + missedBranches);
    System.out.println("Coverage percentage  " + coveragePercentage + "%");
    System.out.println("Non-covered branches " + nonCoveredBranches);
    System.out.println("#############################################################################################");

    PhoneNumberUtil.setInstance(null);
    super.tearDown();
  }
}

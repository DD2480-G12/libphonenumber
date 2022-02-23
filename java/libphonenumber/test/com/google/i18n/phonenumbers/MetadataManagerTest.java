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

import com.google.i18n.phonenumbers.Phonemetadata.PhoneMetadata;

import java.io.*;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import junit.framework.TestCase;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;


/**
 * Some basic tests to check that metadata can be correctly loaded.
 */
public class MetadataManagerTest extends TestCase {
  public void testAlternateFormatsLoadCorrectly() {
    // We should have some data for Germany.
    PhoneMetadata germanyMetadata = MetadataManager.getAlternateFormatsForCountry(49);
    assertNotNull(germanyMetadata);
    assertTrue(germanyMetadata.getNumberFormatCount() > 0);
  }

  public void testAlternateFormatsFailsGracefully() throws Exception {
    PhoneMetadata noAlternateFormats = MetadataManager.getAlternateFormatsForCountry(999);
    assertNull(noAlternateFormats);
  }

  public void testShortNumberMetadataLoadCorrectly() throws Exception {
    // We should have some data for France.
    PhoneMetadata franceMetadata = MetadataManager.getShortNumberMetadataForRegion("FR");
    assertNotNull(franceMetadata);
    assertTrue(franceMetadata.hasShortCode());
  }

  public void testShortNumberMetadataFailsGracefully() throws Exception {
    PhoneMetadata noShortNumberMetadata = MetadataManager.getShortNumberMetadataForRegion("XXX");
    assertNull(noShortNumberMetadata);
  }

  public void testGetMetadataFromMultiFilePrefix_regionCode() {
    ConcurrentHashMap<String, PhoneMetadata> map = new ConcurrentHashMap<String, PhoneMetadata>();
    PhoneMetadata metadata = MetadataManager.getMetadataFromMultiFilePrefix("CA", map,
        "/com/google/i18n/phonenumbers/data/PhoneNumberMetadataProtoForTesting",
        MetadataManager.DEFAULT_METADATA_LOADER);
    assertEquals(metadata, map.get("CA"));
  }

  public void testGetMetadataFromMultiFilePrefix_countryCallingCode() {
    ConcurrentHashMap<Integer, PhoneMetadata> map = new ConcurrentHashMap<Integer, PhoneMetadata>();
    PhoneMetadata metadata = MetadataManager.getMetadataFromMultiFilePrefix(800, map,
        "/com/google/i18n/phonenumbers/data/PhoneNumberMetadataProtoForTesting",
        MetadataManager.DEFAULT_METADATA_LOADER);
    assertEquals(metadata, map.get(800));
  }

  public void testGetMetadataFromMultiFilePrefix_missingMetadataFileThrowsRuntimeException() {
    // In normal usage we should never get a state where we are asking to load metadata that doesn't
    // exist. However if the library is packaged incorrectly in the jar, this could happen and the
    // best we can do is make sure the exception has the file name in it.
    try {
      MetadataManager.getMetadataFromMultiFilePrefix("XX",
          new ConcurrentHashMap<String, PhoneMetadata>(), "no/such/file",
          MetadataManager.DEFAULT_METADATA_LOADER);
      fail("expected exception");
    } catch (RuntimeException e) {
      assertTrue("Unexpected error: " + e, e.getMessage().contains("no/such/file_XX"));
    }
    try {
      MetadataManager.getMetadataFromMultiFilePrefix(123,
          new ConcurrentHashMap<Integer, PhoneMetadata>(), "no/such/file",
          MetadataManager.DEFAULT_METADATA_LOADER);
      fail("expected exception");
    } catch (RuntimeException e) {
      assertTrue("Unexpected error: " + e, e.getMessage().contains("no/such/file_123"));
    }
  }

  // Test that leading digit patterns are written when using writeExternal
  public void testNumberFormatWriteExternalWithLeadingDigits() {
    MultiFileMetadataSourceImpl source =
            new MultiFileMetadataSourceImpl(MetadataManager.DEFAULT_METADATA_LOADER);
    try {

      PhoneMetadata pm1 = source.getMetadataForRegion("US");

      Phonemetadata.NumberFormat nf1 = pm1.getIntlNumberFormat(0);

      ByteArrayOutputStream buffer1 = new ByteArrayOutputStream();
      ObjectOutput out = new ObjectOutputStream(buffer1);

      nf1.writeExternal(out);
      out.close();

      ByteArrayInputStream buffer2 = new ByteArrayInputStream(buffer1.toByteArray());
      ObjectInput in = new ObjectInputStream(buffer2);

      Phonemetadata.NumberFormat nf2 = new Phonemetadata.NumberFormat();
      nf2.readExternal(in);
      in.close();

      ByteArrayOutputStream buffer3 = new ByteArrayOutputStream();
      ObjectOutput out2 = new ObjectOutputStream(buffer3);

      nf2.writeExternal(out2);
      out2.close();

      byte[] output1 = buffer1.toByteArray();
      byte[] output2 = buffer3.toByteArray();

      // assert that writeExternal + readExternal don't lose any information
      assertTrue(Arrays.equals(output1, output2));

      // assert that leading digit pattern counts are equal
      // may be false even if the above assert is true
      assertTrue(nf1.getLeadingDigitsPatternCount() == nf2.getLeadingDigitsPatternCount());


    } catch (Exception e) {
      fail("Did not expect exception: " + e);
    }
  }

  // Test that the national prefix formatting rule is written if it exists
  public void testNumberFormatWriteExternalNationalPrefixRule() {
    Phonemetadata.NumberFormat nf1 = new Phonemetadata.NumberFormat();
    // Set prefix formatting rule, then assert it is equal in the object
    // created from it using writeExternal + readExternal
    nf1.setNationalPrefixFormattingRule("$NP ($FG)");

    try {
      ByteArrayOutputStream buffer1 = new ByteArrayOutputStream();

      ObjectOutput out1 = new ObjectOutputStream(buffer1);

      nf1.writeExternal(out1);
      out1.close();

      ByteArrayInputStream buffer2 = new ByteArrayInputStream(buffer1.toByteArray());
      ObjectInput in = new ObjectInputStream(buffer2);
      Phonemetadata.NumberFormat nf2 = new Phonemetadata.NumberFormat();
      nf2.readExternal(in);
      in.close();

      ByteArrayOutputStream buffer3 = new ByteArrayOutputStream();
      ObjectOutput out2 = new ObjectOutputStream(buffer3);

      nf2.writeExternal(out2);
      out2.close();

      byte[] output1 = buffer1.toByteArray();
      byte[] output2 = buffer3.toByteArray();

      // assert that writeExternal + readExternal don't lose any information
      assertTrue(Arrays.equals(output1, output2));

      // assert that writeExternal does actually write the national prefix rule
      // and writes the condition that signals it exists
      assertTrue(nf1.hasNationalPrefixFormattingRule() && nf2.hasNationalPrefixFormattingRule());
      assertTrue(nf1.getNationalPrefixFormattingRule().equals(nf2.getNationalPrefixFormattingRule()));

    } catch (Exception e) {
      fail("Did not expect exception: " + e);
    }
  }

  // Test that the domestic carrier formatting rule is written if it exists
  public void testNumberFormatWriteExternalDomesticCarrierFormattingRule() {
    Phonemetadata.NumberFormat nf1 = new Phonemetadata.NumberFormat();
    nf1.setDomesticCarrierCodeFormattingRule("$NP ($FG)");

    try {
      ByteArrayOutputStream buffer1 = new ByteArrayOutputStream();

      ObjectOutput out1 = new ObjectOutputStream(buffer1);

      nf1.writeExternal(out1);
      out1.close();

      ByteArrayInputStream buffer2 = new ByteArrayInputStream(buffer1.toByteArray());
      ObjectInput in = new ObjectInputStream(buffer2);
      Phonemetadata.NumberFormat nf2 = new Phonemetadata.NumberFormat();
      nf2.readExternal(in);
      in.close();

      ByteArrayOutputStream buffer3 = new ByteArrayOutputStream();
      ObjectOutput out2 = new ObjectOutputStream(buffer3);

      nf2.writeExternal(out2);
      out2.close();

      byte[] output1 = buffer1.toByteArray();
      byte[] output2 = buffer3.toByteArray();

      // assert that writeExternal + readExternal don't lose any information
      assertTrue(Arrays.equals(output1, output2));

      // assert that writeExternal does actually write the domestic carrier code formatting rule
      // and writes the condition that signals it exists
      assertTrue(nf1.hasDomesticCarrierCodeFormattingRule() && nf2.hasDomesticCarrierCodeFormattingRule());
      assertTrue(nf1.getDomesticCarrierCodeFormattingRule().equals(nf2.getDomesticCarrierCodeFormattingRule()));

    } catch (Exception e) {
      fail("Did not expect exception: " + e);
    }
  }

  // This test tests compatibility between writeExternal and readExternal
  // These are used in conjunction to store/load objects from a stream (e.g. a file)
  // For it to pass any mistakes in writeExternal would have to be in readExternal, too
  // To test it, multiple significantly different PhoneMetadata objects are stored/loaded
  public void testWriteExternalEmptyPhoneMetadataCanBeRead() {

    PhoneMetadata pm1 = new PhoneMetadata();
    try {
      ByteArrayOutputStream buffer1 = new ByteArrayOutputStream();

      ObjectOutput out1 = new ObjectOutputStream(buffer1);

      pm1.writeExternal(out1);
      out1.close();

      ByteArrayInputStream buffer2 = new ByteArrayInputStream(buffer1.toByteArray());
      ObjectInput in = new ObjectInputStream(buffer2);
      PhoneMetadata pm2 = new PhoneMetadata();
      pm2.readExternal(in);
      in.close();

      ByteArrayOutputStream buffer3 = new ByteArrayOutputStream();
      ObjectOutput out2 = new ObjectOutputStream(buffer3);

      pm2.writeExternal(out2);
      out2.close();

      byte[] output1 = buffer1.toByteArray();
      byte[] output2 = buffer3.toByteArray();

      assertTrue(Arrays.equals(output1, output2));

    } catch (Exception e) {
      fail("Did not expect exception..." + e);
    }
  }

  public void testWriteExternalOnShortNumberMetadataCanBeRead() {

    PhoneMetadata pm1 = MetadataManager.getShortNumberMetadataForRegion("US");

    try {
      ByteArrayOutputStream buffer1 = new ByteArrayOutputStream();

      ObjectOutput out1 = new ObjectOutputStream(buffer1);

      pm1.writeExternal(out1);
      out1.close();

      ByteArrayInputStream buffer2 = new ByteArrayInputStream(buffer1.toByteArray());
      ObjectInput in = new ObjectInputStream(buffer2);
      PhoneMetadata pm2 = new PhoneMetadata();
      pm2.readExternal(in);
      in.close();

      ByteArrayOutputStream buffer3 = new ByteArrayOutputStream();
      ObjectOutput out2 = new ObjectOutputStream(buffer3);

      pm2.writeExternal(out2);
      out2.close();

      byte[] output1 = buffer1.toByteArray();
      byte[] output2 = buffer3.toByteArray();

      // assert that writeExternal and readExternal are compatible
      assertTrue(Arrays.equals(output1, output2));

    } catch (Exception e) {
      fail("Did not expect exception..." + e);
    }
  }

  public void testWriteExternalOnTypicalMetadataCanBeRead() {
    ConcurrentHashMap<String, PhoneMetadata> map = new ConcurrentHashMap<String, PhoneMetadata>();
    PhoneMetadata pm1 = MetadataManager.getMetadataFromMultiFilePrefix("CA", map,
            "/com/google/i18n/phonenumbers/data/PhoneNumberMetadataProtoForTesting",
            MetadataManager.DEFAULT_METADATA_LOADER);
    try {
      ByteArrayOutputStream buffer1 = new ByteArrayOutputStream();

      ObjectOutput out1 = new ObjectOutputStream(buffer1);

      pm1.writeExternal(out1);
      out1.close();

      ByteArrayInputStream buffer2 = new ByteArrayInputStream(buffer1.toByteArray());
      ObjectInput in = new ObjectInputStream(buffer2);
      PhoneMetadata pm2 = new PhoneMetadata();
      pm2.readExternal(in);
      in.close();

      ByteArrayOutputStream buffer3 = new ByteArrayOutputStream();
      ObjectOutput out2 = new ObjectOutputStream(buffer3);

      pm2.writeExternal(out2);
      out2.close();

      byte[] output1 = buffer1.toByteArray();
      byte[] output2 = buffer3.toByteArray();

      // assert that writeExternal and readExternal are compatible
      assertTrue(Arrays.equals(output1, output2));

    } catch (Exception e) {
      fail("Did not expect exception..." + e);
    }
  }

  public void testWriteExternalOnNonTypicalMetadataCanBeRead() {
    MultiFileMetadataSourceImpl source =
            new MultiFileMetadataSourceImpl(MetadataManager.DEFAULT_METADATA_LOADER);
    PhoneMetadata pm1 = source.getMetadataForNonGeographicalRegion(800);

    try {
      ByteArrayOutputStream buffer1 = new ByteArrayOutputStream();

      ObjectOutput out1 = new ObjectOutputStream(buffer1);

      pm1.writeExternal(out1);
      out1.close();

      ByteArrayInputStream buffer2 = new ByteArrayInputStream(buffer1.toByteArray());
      ObjectInput in = new ObjectInputStream(buffer2);
      PhoneMetadata pm2 = new PhoneMetadata();
      pm2.readExternal(in);
      in.close();

      ByteArrayOutputStream buffer3 = new ByteArrayOutputStream();
      ObjectOutput out2 = new ObjectOutputStream(buffer3);

      pm2.writeExternal(out2);
      out2.close();

      byte[] output1 = buffer1.toByteArray();
      byte[] output2 = buffer3.toByteArray();

      // assert that writeExternal and readExternal are compatible
      assertTrue(Arrays.equals(output1, output2));

    } catch (Exception e) {
      fail("Did not expect exception..." + e);
    }
  }

  public void testWriteExternalOnDetailedMetadataCanBeReadAndHasRightProperties() {

    ConcurrentHashMap<String, PhoneMetadata> map = new ConcurrentHashMap<String, PhoneMetadata>();
    PhoneMetadata pm1 = MetadataManager.getMetadataFromMultiFilePrefix("US", map,
            "/com/google/i18n/phonenumbers/data/PhoneNumberMetadataProtoForTesting",
            MetadataManager.DEFAULT_METADATA_LOADER);

    // Some properties are sampled, and it is asserted that they are written when calling writeExternal
    pm1.setInternationalPrefix("001");
    pm1.setPreferredInternationalPrefix("002");
    pm1.setNationalPrefixTransformRule("5$15");
    pm1.setLeadingDigits("000");

    try {
      ByteArrayOutputStream buffer1 = new ByteArrayOutputStream();

      ObjectOutput out1 = new ObjectOutputStream(buffer1);

      pm1.writeExternal(out1);
      out1.close();

      ByteArrayInputStream buffer2 = new ByteArrayInputStream(buffer1.toByteArray());
      ObjectInput in = new ObjectInputStream(buffer2);
      PhoneMetadata pm2 = new PhoneMetadata();
      pm2.readExternal(in);
      in.close();

      ByteArrayOutputStream buffer3 = new ByteArrayOutputStream();
      ObjectOutput out2 = new ObjectOutputStream(buffer3);

      pm2.writeExternal(out2);
      out2.close();

      byte[] output1 = buffer1.toByteArray();
      byte[] output2 = buffer3.toByteArray();

      // assert that writeExternal and readExternal are compatible
      assertTrue(Arrays.equals(output1, output2));

      // assert that some properties are written when using writeExternal
      assertTrue(pm2.getInternationalPrefix().equals(pm1.getInternationalPrefix()));
      assertTrue(pm2.getPreferredInternationalPrefix().equals(pm1.getPreferredInternationalPrefix()));
      assertTrue(pm2.getNationalPrefixTransformRule().equals(pm1.getNationalPrefixTransformRule()));
      assertTrue(pm2.getLeadingDigits().equals(pm1.getLeadingDigits()));

    } catch (Exception e) {
      fail("Did not expect exception..." + e);
    }
  }

}

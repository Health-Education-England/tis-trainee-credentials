/*
 * The MIT License (MIT)
 *
 * Copyright 2023 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.trainee.credentials;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

/**
 * A test utility with helper methods for data signatures.
 */
public class SignatureTestUtil {

  private static final String SIGNATURE_FIELD = "signature";
  private static final String HMAC_FIELD = "hmac";

  private static final ObjectMapper MAPPER;

  static {
    MAPPER = new ObjectMapper();
    MAPPER.registerModule(new JavaTimeModule());
  }

  /**
   * A helper method to remove a field from the data and then sign it with a valid HMAC.
   *
   * @param dataToSign       The data to sign.
   * @param secretKey        The secret key to sign the data with.
   * @param fieldToOverwrite The field to overwrite in the data before signing.
   * @param newFieldValue    The new value of the overwritten field.
   * @return A string representation of the signed data.
   * @throws JsonProcessingException If the dataToSign was not valid JSON.
   */
  public static String overwriteFieldAndSignData(String dataToSign, String secretKey,
      String fieldToOverwrite, String newFieldValue) throws JsonProcessingException {
    ObjectNode nodeToSign = (ObjectNode) MAPPER.readTree(dataToSign);
    nodeToSign.put(fieldToOverwrite, newFieldValue);
    return signData(nodeToSign, secretKey);
  }

  /**
   * A helper method to remove a field from the data and then sign it with a valid HMAC.
   *
   * @param dataToSign    The data to sign.
   * @param secretKey     The secret key to sign the data with.
   * @param fieldToRemove The field to remove from the data before signing.
   * @return A string representation of the signed data.
   * @throws JsonProcessingException If the dataToSign was not valid JSON.
   */
  public static String removeFieldAndSignData(String dataToSign, String secretKey,
      String fieldToRemove) throws JsonProcessingException {
    ObjectNode nodeToSign = (ObjectNode) MAPPER.readTree(dataToSign);
    nodeToSign.remove(fieldToRemove);

    return signData(nodeToSign, secretKey);
  }

  /**
   * A helper method to sign test data with a valid HMAC.
   *
   * @param dataToSign The data to sign.
   * @param secretKey  The secret key to sign the data with.
   * @return A string representation of the signed data.
   * @throws JsonProcessingException If the dataToSign was not valid JSON.
   */
  public static String signData(String dataToSign, String secretKey)
      throws JsonProcessingException {
    JsonNode nodeToSign = MAPPER.readTree(dataToSign);
    return signData(nodeToSign, secretKey);
  }

  /**
   * A helper method to sign test data with a valid HMAC.
   *
   * @param nodeToSign The data to sign.
   * @param secretKey  The secret key to sign the data with.
   * @return A string representation of the signed data.
   * @throws JsonProcessingException If the dataToSign was not valid JSON.
   */
  private static String signData(JsonNode nodeToSign, String secretKey)
      throws JsonProcessingException {
    ObjectNode signatureNode = (ObjectNode) nodeToSign.get(SIGNATURE_FIELD);

    byte[] bytesToSign = MAPPER.writeValueAsBytes(nodeToSign);
    String hmac = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secretKey).hmacHex(
        bytesToSign);
    signatureNode.put(HMAC_FIELD, hmac);

    return nodeToSign.toString();
  }
}

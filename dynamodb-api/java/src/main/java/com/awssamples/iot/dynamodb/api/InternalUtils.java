/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.awssamples.iot.dynamodb.api;

import static software.amazon.awssdk.utils.BinaryUtils.copyAllBytesFrom;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.util.SdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructMap;
import software.amazon.awssdk.core.util.VersionInfo;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.ExpectedAttributeValue;

/**
 * Internal utilities.  Not meant for general use.  May change without notice.
 */
public class InternalUtils {
    /**
     * Converts a list of low-level <code>AttributeValue</code> into a list of
     * simple values. Each value in the returned list can be one of the
     * followings:
     *
     * <ul>
     * <li>String</li>
     * <li>Set&lt;String></li>
     * <li>Number (including any subtypes and primitive types)</li>
     * <li>Set&lt;Number></li>
     * <li>byte[]</li>
     * <li>Set&lt;byte[]></li>
     * <li>ByteBuffer</li>
     * <li>Set&lt;ByteBuffer></li>
     * <li>Boolean or boolean</li>
     * <li>null</li>
     * <li>Map&lt;String,T>, where T can be any type on this list but must not
     * induce any circular reference</li>
     * <li>List&lt;T>, where T can be any type on this list but must not induce
     * any circular reference</li>
     * </ul>
     */
    public static List<Object> toSimpleList(List<AttributeValue> attrValues) {
        if (attrValues == null) {
            return null;
        }
        List<Object> result = new ArrayList<Object>(attrValues.size());
        for (AttributeValue attrValue : attrValues) {
            Object value = toSimpleValue(attrValue);
            result.add(value);
        }
        return result;
    }

    /**
     * Convenient method to convert a list of low-level
     * <code>AttributeValue</code> into a list of values of the same type T.
     * Each value in the returned list can be one of the followings:
     * <ul>
     * <li>String</li>
     * <li>Set&lt;String></li>
     * <li>Number (including any subtypes and primitive types)</li>
     * <li>Set&lt;Number></li>
     * <li>byte[]</li>
     * <li>Set&lt;byte[]></li>
     * <li>ByteBuffer</li>
     * <li>Set&lt;ByteBuffer></li>
     * <li>Boolean or boolean</li>
     * <li>null</li>
     * <li>Map&lt;String,T>, where T can be any type on this list but must not
     * induce any circular reference</li>
     * <li>List&lt;T>, where T can be any type on this list but must not induce
     * any circular reference</li>
     * </ul>
     */
    public static <T> List<T> toSimpleListValue(List<AttributeValue> values) {
        if (values == null) {
            return null;
        }

        List<T> result = new ArrayList<T>(values.size());
        for (AttributeValue v : values) {
            T t = toSimpleValue(v);
            result.add(t);
        }
        return result;
    }

    public static <T> Map<String, T> toSimpleMapValue(
            Map<String, AttributeValue> values) {
        if (values == null) {
            return null;
        }

        Map<String, T> result = new LinkedHashMap<String, T>(values.size());
        for (Map.Entry<String, AttributeValue> entry : values.entrySet()) {
            T t = toSimpleValue(entry.getValue());
            result.put(entry.getKey(), t);
        }
        return result;
    }

    /**
     * Returns the string representation of the given value; or null if the
     * value is null. For <code>BigDecimal</code> it will be the string
     * representation without an exponent field.
     */
    public static String valToString(Object val) {
        if (val instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal) val;
            return bd.toPlainString();
        }
        if (val == null) {
            return null;
        }
        if (val instanceof String
                || val instanceof Boolean
                || val instanceof Number) {
            return val.toString();
        }
        throw new RuntimeException("Cannot convert " + val.getClass() + " into a string");
    }

    /**
     * Converts a low-level <code>AttributeValue</code> into a simple value,
     * which can be one of the followings:
     *
     * <ul>
     * <li>String</li>
     * <li>Set&lt;String></li>
     * <li>Number (including any subtypes and primitive types)</li>
     * <li>Set&lt;Number></li>
     * <li>byte[]</li>
     * <li>Set&lt;byte[]></li>
     * <li>ByteBuffer</li>
     * <li>Set&lt;ByteBuffer></li>
     * <li>Boolean or boolean</li>
     * <li>null</li>
     * <li>Map&lt;String,T>, where T can be any type on this list but must not
     * induce any circular reference</li>
     * <li>List&lt;T>, where T can be any type on this list but must not induce
     * any circular reference</li>
     * </ul>
     *
     * @throws IllegalArgumentException if an empty <code>AttributeValue</code> value is specified
     */
    static <T> T toSimpleValue(AttributeValue value) {
        if (value == null) {
            return null;
        }
        if (Boolean.TRUE.equals(value.nul())) {
            return null;
        } else if (Boolean.FALSE.equals(value.nul())) {
            throw new UnsupportedOperationException("False-NULL is not supported in DynamoDB");
        } else if (value.bool() != null) {
            @SuppressWarnings("unchecked")
            T t = (T) value.bool();
            return t;
        } else if (value.s() != null) {
            @SuppressWarnings("unchecked")
            T t = (T) value.s();
            return t;
        } else if (value.n() != null) {
            @SuppressWarnings("unchecked")
            T t = (T) new BigDecimal(value.n());
            return t;
        } else if (value.b() != null) {
            @SuppressWarnings("unchecked")
            T t = (T) value.b().asByteArray();
            return t;
        } else if (value.ss() != null && !(value.ss() instanceof SdkAutoConstructList)) {
            @SuppressWarnings("unchecked")
            T t = (T) new LinkedHashSet<String>(value.ss());
            return t;
        } else if (value.ns() != null && !(value.ns() instanceof SdkAutoConstructList)) {
            Set<BigDecimal> set = new LinkedHashSet<BigDecimal>(value.ns().size());
            for (String s : value.ns()) {
                set.add(new BigDecimal(s));
            }
            @SuppressWarnings("unchecked")
            T t = (T) set;
            return t;
        } else if (value.bs() != null && !(value.bs() instanceof SdkAutoConstructList)) {
            Set<byte[]> set = new LinkedHashSet<byte[]>(value.bs().size());
            for (SdkBytes bb : value.bs()) {
                set.add(copyAllBytesFrom(bb.asByteBuffer()));
            }
            @SuppressWarnings("unchecked")
            T t = (T) set;
            return t;
        } else if (value.l() != null && !(value.l() instanceof SdkAutoConstructList)) {
            @SuppressWarnings("unchecked")
            T t = (T) toSimpleList(value.l());
            return t;
        } else if (value.m() != null && !(value.m() instanceof SdkAutoConstructMap)) {
            @SuppressWarnings("unchecked")
            T t = (T) toSimpleMapValue(value.m());
            return t;
        } else {
            throw new IllegalArgumentException(
                    "Attribute value must not be empty: " + value);
        }
    }

    /**
     * Returns the minimum of the two input integers taking null into account.
     * Returns null if both integers are null. Otherwise, a null Integer is
     * treated as infinity.
     */
    public static Integer minimum(Integer one, Integer two) {
        if (one == null) {
            return two;
        } else if (two == null) {
            return one;
        } else if (one < two) {
            return one;
        } else {
            return two;
        }
    }
}

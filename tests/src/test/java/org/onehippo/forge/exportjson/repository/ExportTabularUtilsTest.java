/*
 * Copyright 2024 Bloomreach B.V. (http://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.exportjson.repository;

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Value;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for ExportTabularUtils utility methods
 */
public class ExportTabularUtilsTest {

    private Property mockProperty;
    private Value mockValue;
    private Value[] mockValues;

    @Before
    public void setUp() {
        mockProperty = createMock(Property.class);
        mockValue = createMock(Value.class);
        mockValues = new Value[0];
    }

    @Test
    public void testPropertyValueToStringSingleValue() throws Exception {
        expect(mockProperty.isMultiple()).andReturn(false);
        expect(mockProperty.getValue()).andReturn(mockValue);
        expect(mockValue.getString()).andReturn("test value");
        replay(mockProperty, mockValue);

        String result = ExportTabularUtils.propertyValueToString(mockProperty);
        assertEquals("Should return string value for single-valued property", "test value", result);
        verify(mockProperty, mockValue);
    }

    @Test
    public void testPropertyValueToStringMultipleValues() throws Exception {
        Value value1 = createMock(Value.class);
        Value value2 = createMock(Value.class);
        Value value3 = createMock(Value.class);
        Value[] values = {value1, value2, value3};

        expect(mockProperty.isMultiple()).andReturn(true);
        expect(mockProperty.getValues()).andReturn(values);
        expect(value1.getString()).andReturn("value1");
        expect(value2.getString()).andReturn("value2");
        expect(value3.getString()).andReturn("value3");
        replay(mockProperty, value1, value2, value3);

        String result = ExportTabularUtils.propertyValueToString(mockProperty);
        assertEquals("Should join multiple values with pipe delimiter", "value1|value2|value3", result);
        verify(mockProperty, value1, value2, value3);
    }

    @Test
    public void testPropertyValueToStringMultipleValuesWithEmptyStrings() throws Exception {
        Value value1 = createMock(Value.class);
        Value value2 = createMock(Value.class);
        Value[] values = {value1, value2};

        expect(mockProperty.isMultiple()).andReturn(true);
        expect(mockProperty.getValues()).andReturn(values);
        expect(value1.getString()).andReturn("");
        expect(value2.getString()).andReturn("nonempty");
        replay(mockProperty, value1, value2);

        String result = ExportTabularUtils.propertyValueToString(mockProperty);
        assertEquals("Should preserve empty strings in joined values", "|nonempty", result);
        verify(mockProperty, value1, value2);
    }

    @Test
    public void testPropertyValueToStringMultipleValuesWithSpecialCharacters() throws Exception {
        Value value1 = createMock(Value.class);
        Value value2 = createMock(Value.class);
        Value[] values = {value1, value2};

        expect(mockProperty.isMultiple()).andReturn(true);
        expect(mockProperty.getValues()).andReturn(values);
        expect(value1.getString()).andReturn("hello, world");
        expect(value2.getString()).andReturn("test\nvalue");
        replay(mockProperty, value1, value2);

        String result = ExportTabularUtils.propertyValueToString(mockProperty);
        assertEquals("Should preserve special characters in joined values", "hello, world|test\nvalue", result);
        verify(mockProperty, value1, value2);
    }

    @Test
    public void testPropertyValueToStringMultipleValuesWithExceptionInValue() throws Exception {
        Value value1 = createMock(Value.class);
        Value value2 = createMock(Value.class);
        Value value3 = createMock(Value.class);
        Value[] values = {value1, value2, value3};

        expect(mockProperty.isMultiple()).andReturn(true);
        expect(mockProperty.getValues()).andReturn(values);
        expect(value1.getString()).andReturn("value1");
        expect(value2.getString()).andThrow(new RuntimeException("Unable to get value"));
        expect(value3.getString()).andReturn("value3");
        replay(mockProperty, value1, value2, value3);

        String result = ExportTabularUtils.propertyValueToString(mockProperty);
        assertEquals("Should handle exceptions in individual values gracefully",
            "value1|[Unable to serialize]|value3", result);
        verify(mockProperty, value1, value2, value3);
    }

    @Test
    public void testPropertyValueToStringEmptyMultipleValues() throws Exception {
        Value[] values = {};

        expect(mockProperty.isMultiple()).andReturn(true);
        expect(mockProperty.getValues()).andReturn(values);
        replay(mockProperty);

        String result = ExportTabularUtils.propertyValueToString(mockProperty);
        assertEquals("Should return empty string for array with no values", "", result);
        verify(mockProperty);
    }

    @Test
    public void testGetPropertyTypeNameString() {
        String result = ExportTabularUtils.getPropertyTypeName(PropertyType.STRING);
        assertEquals("Should return 'String' for PropertyType.STRING", "String", result);
    }

    @Test
    public void testGetPropertyTypeNameLong() {
        String result = ExportTabularUtils.getPropertyTypeName(PropertyType.LONG);
        assertEquals("Should return 'Long' for PropertyType.LONG", "Long", result);
    }

    @Test
    public void testGetPropertyTypeNameDouble() {
        String result = ExportTabularUtils.getPropertyTypeName(PropertyType.DOUBLE);
        assertEquals("Should return 'Double' for PropertyType.DOUBLE", "Double", result);
    }

    @Test
    public void testGetPropertyTypeNameBoolean() {
        String result = ExportTabularUtils.getPropertyTypeName(PropertyType.BOOLEAN);
        assertEquals("Should return 'Boolean' for PropertyType.BOOLEAN", "Boolean", result);
    }

    @Test
    public void testGetPropertyTypeNameDate() {
        String result = ExportTabularUtils.getPropertyTypeName(PropertyType.DATE);
        assertEquals("Should return 'Date' for PropertyType.DATE", "Date", result);
    }

    @Test
    public void testGetPropertyTypeNameBinary() {
        String result = ExportTabularUtils.getPropertyTypeName(PropertyType.BINARY);
        assertEquals("Should return 'Binary' for PropertyType.BINARY", "Binary", result);
    }

    @Test
    public void testGetPropertyTypeNameReference() {
        String result = ExportTabularUtils.getPropertyTypeName(PropertyType.REFERENCE);
        assertEquals("Should return 'Reference' for PropertyType.REFERENCE", "Reference", result);
    }

    @Test
    public void testGetPropertyTypeNameUndefined() {
        String result = ExportTabularUtils.getPropertyTypeName(PropertyType.UNDEFINED);
        assertEquals("Should return 'undefined' for PropertyType.UNDEFINED", "undefined", result);
    }
}

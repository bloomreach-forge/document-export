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

import java.util.Arrays;
import java.util.stream.Collectors;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Value;

public final class ExportTabularUtils {

    /**
     * Converts a JCR property value to a string, handling multi-valued properties
     * by joining values with pipe delimiter.
     *
     * @param property the JCR property
     * @return string representation of the property value
     * @throws Exception if unable to extract property value
     */
    public static String propertyValueToString(Property property) throws Exception {
        if (property.isMultiple()) {
            Value[] values = property.getValues();
            return Arrays.stream(values)
                .map(ExportTabularUtils::getValueAsString)
                .collect(Collectors.joining(ExportTabularConstants.DELIMITER_PIPE));
        } else {
            return property.getValue().getString();
        }
    }

    /**
     * Extracts a string representation from a JCR Value.
     *
     * @param value the JCR Value
     * @return string representation of the value
     */
    private static String getValueAsString(Value value) {
        try {
            return value.getString();
        } catch (Exception e) {
            return "[Unable to serialize]";
        }
    }

    /**
     * Gets the readable name of a JCR property type constant.
     *
     * @param propertyType the PropertyType constant
     * @return readable property type name
     */
    public static String getPropertyTypeName(int propertyType) {
        return PropertyType.nameFromValue(propertyType);
    }

    private ExportTabularUtils() {
    }

}

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.converter.dozer;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConversionException;
import org.apache.camel.TypeConverter;
import org.apache.camel.support.TypeConverterSupport;
import org.dozer.DozerBeanMapper;
import org.dozer.Mapper;
import org.dozer.metadata.ClassMappingMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>DozerTypeConverter</code> is a standard {@link TypeConverter} that
 * delegates to a {@link Mapper} from the Dozer framework to convert between
 * types. <code>DozerTypeConverter</code>s are created and installed into a
 * {@link CamelContext} by an instance of {@link DozerTypeConverterLoader}.
 * <p>
 * See <a href="http://dozer.sourceforge.net">dozer project page</a> or more information on configuring Dozer
 *
 * @see DozerTypeConverterLoader
 */
public class DozerTypeConverter extends TypeConverterSupport {

    private static final Logger LOG = LoggerFactory.getLogger(DozerTypeConverter.class);

    private final DozerBeanMapper mapper;

    public DozerTypeConverter(DozerBeanMapper mapper) {
        this.mapper = mapper;
    }

    public DozerBeanMapper getMapper() {
        return mapper;
    }

    @Override
    public <T> T convertTo(Class<T> type, Exchange exchange, Object value) throws TypeConversionException {

        CamelContext context = exchange != null ? exchange.getContext() : null;
        ClassLoader appcl = context != null ? context.getApplicationContextClassLoader() : null;

        T result;

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            if (appcl != null && appcl != tccl) {
                LOG.debug("Switching TCCL to: {}", appcl);
                Thread.currentThread().setContextClassLoader(appcl);
            }

            // find the map id, so we can provide that when trying to map from source to destination
            String mapId = null;
            if (value != null) {
                Class<?> sourceType = value.getClass();
                Class<?> destType = type;
                ClassMappingMetadata metadata = mapper.getMappingMetadata().getClassMapping(sourceType, destType);
                if (metadata != null) {
                    mapId = metadata.getMapId();
                }
            }

            result = mapper.map(value, type, mapId);

        } finally {
            if (appcl != null && appcl != tccl) {
                Thread.currentThread().setContextClassLoader(tccl);
                LOG.debug("Restored TCCL to: {}", tccl);
            }
        }

        return result;
    }
}

/*
 * Copyright 2015 Jo Rabin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.linguafranca.xml;

import javax.xml.stream.events.XMLEvent;

/**
 * An interface for allowing XML events to be transformed by {@link XmlOutputStreamFilter}
 * and {@link XmlInputStreamFilter}.
 *
 * @author jo
 */
public interface XmlEventTransformer {
    XMLEvent transform(XMLEvent event);

    @SuppressWarnings("unused")
    class None implements XmlEventTransformer {
        @Override
        public XMLEvent transform(XMLEvent event) {
            return event;
        }
    }
}
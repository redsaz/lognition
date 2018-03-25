/*
 * Copyright 2016 Redsaz <redsaz@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redsaz.lognition;

import com.redsaz.lognition.view.Templater;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.testng.Assert;
import org.testng.annotations.DataProvider;

/**
 * @author Redsaz <redsaz@gmail.com>
 */
public class BaseResourceTest extends Assert {

    public static final String DEFAULT_DP = "mocksForNotesService";

    public static class Context {

        public Dispatcher dispatcher;
        public Templater templater;

        public HttpResponse invoke(HttpRequest request) {
            MockHttpResponse response = new MockHttpResponse();
            dispatcher.invoke(request, response);
            return response;
        }
    }

    private static Context setup() {
        Context context = new Context();
        context.dispatcher = createDispatcher();
        context.templater = createTemplater();

        context.dispatcher.getProviderFactory().registerProvider(BasicExceptionMapper.class);

        return context;
    }

    private static Dispatcher createDispatcher() {
        Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
// NOTE: Add these back when we have actual tests.
//        ResteasyProviderFactory
//                .getContextDataMap()
//                .put(HttpServletRequest.class, new MockHttpServletRequest());
        return dispatcher;
    }

    private static Templater createTemplater() {
        Templater mockedTemplater = mock(Templater.class);
        when(mockedTemplater.buildFromTemplate(any(), any(String.class))).thenReturn("Well done.");

        return mockedTemplater;
    }

    @DataProvider(name = DEFAULT_DP)
    public static Object[][] mocksForNotesService() {
        Context context = setup();
        return new Object[][]{new Object[]{context}};
    }

}

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
package com.redsaz.debitage.view;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * Allows for static content and REST endpoints to be served without needing to
 * put the REST endpoints in a sub-directory. As seen in
 * http://stackoverflow.com/a/3593513
 *
 * @author Redsaz <redsaz@gmail.com>
 */
public class StaticContentFilter implements Filter {

    @Override
    public void init(FilterConfig fc) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getRequestURI().substring(req.getContextPath().length());

        if (path.startsWith("/dist") || path.endsWith("index.html") || path.endsWith("favicon.ico")) {
            // Use the default servlet, which knows how to handle static content.
            request.getServletContext().getNamedDispatcher("default").forward(request, response);
        } else {
            request.getRequestDispatcher(path).forward(request, response);
        }
    }

    @Override
    public void destroy() {
    }

}

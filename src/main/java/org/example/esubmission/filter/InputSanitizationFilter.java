package org.example.esubmission.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.example.esubmission.util.SecurityUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter that sanitizes all incoming request parameters to prevent XSS and injection attacks.
 * It uses {@link SecurityUtil#sanitizeInput(String)} to clean parameter values.
 */
public class InputSanitizationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            chain.doFilter(new SanitizedRequestWrapper((HttpServletRequest) request), response);
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * Request wrapper that performs sanitization on parameter retrieval.
     */
    private static class SanitizedRequestWrapper extends HttpServletRequestWrapper {

        private Map<String, String[]> sanitizedParameters;

        public SanitizedRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            if (isSensitive(name)) return value;
            return SecurityUtil.sanitizeInput(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) return null;
            if (isSensitive(name)) return values;

            String[] sanitizedValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                sanitizedValues[i] = SecurityUtil.sanitizeInput(values[i]);
            }
            return sanitizedValues;
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            if (sanitizedParameters == null) {
                Map<String, String[]> rawMap = super.getParameterMap();
                Map<String, String[]> newMap = new HashMap<>();

                for (Map.Entry<String, String[]> entry : rawMap.entrySet()) {
                    String[] values = entry.getValue();
                    if (isSensitive(entry.getKey())) {
                        newMap.put(entry.getKey(), values);
                        continue;
                    }
                    String[] sanitizedValues = new String[values.length];
                    for (int i = 0; i < values.length; i++) {
                        sanitizedValues[i] = SecurityUtil.sanitizeInput(values[i]);
                    }
                    newMap.put(entry.getKey(), sanitizedValues);
                }
                sanitizedParameters = Collections.unmodifiableMap(newMap);
            }
            return sanitizedParameters;
        }

        /**
         * Sensitive parameters such as passwords must never be altered.
         */
        private boolean isSensitive(String name) {
            return name != null && (name.toLowerCase().contains("password"));
        }
    }
}

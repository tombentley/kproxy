/*
 * Copyright Kroxylicious Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package io.kroxylicious.proxy.internal;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.Principal;
import java.util.List;

import io.kroxylicious.proxy.authentication.ClientPrincipalAware;
import io.kroxylicious.proxy.filter.Filter;
import io.kroxylicious.proxy.filter.FilterAndInvoker;

public class FilterChainDispatch {
    private final List<FilterAndInvoker> filters;

    public FilterChainDispatch(List<FilterAndInvoker> filters) {
        this.filters = filters;
    }

    void clientSaslAuthenticationSuccess(Filter caller, Principal principal) {
        boolean seenCaller = true;
        for (var filterAndInvoker : filters) {
            Filter filter = filterAndInvoker.filter();
            if (seenCaller) {
                if (filter instanceof ClientPrincipalAware<?> clientPrincipalAware) {
                    Type type = clientPrincipalAware.getClass();
                    Type reifiedPrincipalType = null;
                    OUTER: do {
                        Type[] genericInterfaces;
                        Type genericSuperclass;
                        if (type instanceof Class c) {
                            genericInterfaces = c.getGenericInterfaces();
                            genericSuperclass = c.getGenericSuperclass();
                        }
                        // else if (type instanceof ParameterizedType pt) {
                        // genericInterfaces = pt.getGenericInterfaces();
                        // genericSuperclass = pt.getGenericSuperclass();
                        // }
                        else {
                            throw new IllegalStateException();
                        }
                        for (Type t : genericInterfaces) {
                            if (t instanceof ParameterizedType pt) {
                                if (ClientPrincipalAware.class.equals(pt.getRawType())) {
                                    reifiedPrincipalType = pt.getActualTypeArguments()[0];
                                    break OUTER;
                                }
                            }
                        }
                        type = genericSuperclass;
                    } while (type != null);

                    if (reifiedPrincipalType instanceof Class<?> principalClass) {
                        if (principalClass.isInstance(principal)) {
                            principalClass.cast(principal);
                            ((ClientPrincipalAware) clientPrincipalAware).onClientAuthentication(principal);
                        }
                    }
                    else {
                        throw new RuntimeException();
                    }
                }
            }
            else {
                if (caller.equals(filter)) {
                    seenCaller = true;
                }
            }
        }
    }

    void clientSaslAuthenticationFailure(Filter caller, Exception exception) {

    }
}

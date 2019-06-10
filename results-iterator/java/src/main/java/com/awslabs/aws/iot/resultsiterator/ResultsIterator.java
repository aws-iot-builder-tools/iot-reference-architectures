package com.awslabs.aws.iot.resultsiterator;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.AmazonWebServiceResult;
import com.amazonaws.SdkClientException;
import com.google.common.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ResultsIterator<T> {
    private final Logger log = LoggerFactory.getLogger(ResultsIterator.class);
    private final AmazonWebServiceClient amazonWebServiceClient;
    private final Class<? extends AmazonWebServiceRequest> requestClass;
    private final Class<? extends AmazonWebServiceResult> resultClass;
    private final List<String> getTokenMethodNames = new ArrayList<>(Arrays.asList("getNextToken", "getMarker", "getNextMarker"));
    private final List<String> setTokenMethodNames = new ArrayList<>(Arrays.asList("setNextToken", "setMarker", "setNextMarker"));
    private AmazonWebServiceRequest request;
    private AmazonWebServiceResult result;
    private Method clientMethodReturningResult;
    private Method clientMethodReturningListT;
    private Method clientGetMethodReturningString;
    private Method clientSetMethodAcceptingString;

    public ResultsIterator(AmazonWebServiceClient amazonWebServiceClient, Class<? extends AmazonWebServiceRequest> requestClass, Class<? extends AmazonWebServiceResult> resultClass) {
        this.amazonWebServiceClient = amazonWebServiceClient;
        this.requestClass = requestClass;
        this.resultClass = resultClass;
    }

    public ResultsIterator(AmazonWebServiceClient amazonWebServiceClient, AmazonWebServiceRequest request, Class<? extends AmazonWebServiceResult> resultClass) {
        this.amazonWebServiceClient = amazonWebServiceClient;
        this.requestClass = request.getClass();
        this.resultClass = resultClass;
        this.request = request;
    }

    private AmazonWebServiceResult queryNextResults() {
        if (clientMethodReturningResult == null) {
            // Look for a public method in the client (AWSIot, etc) that takes a AmazonWebServiceRequest and returns a V.  If zero or more than one exists, fail.
            clientMethodReturningResult = getMethodWithParameterAndReturnType(amazonWebServiceClient.getClass(), requestClass, resultClass);
        }

        try {
            return (AmazonWebServiceResult) clientMethodReturningResult.invoke(amazonWebServiceClient, request);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof SdkClientException) {
                SdkClientException sdkClientException = (SdkClientException) e.getTargetException();

                if (sdkClientException.getMessage().contains("Unable to execute HTTP request")) {
                    log.error("Unable to connect to the API.  Do you have an Internet connection?");
                    return null;
                }
            }

            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        }
    }

    private List<T> getResultData() {
        if (clientMethodReturningListT == null) {
            // Look for a public method that takes no arguments and returns a List<T>.  If zero or more than one exists, fail.
            clientMethodReturningListT = getMethodWithParameterAndReturnType(resultClass, null, new TypeToken<List<T>>(getClass()) {
            }.getRawType());
        }

        try {
            return (List<T>) clientMethodReturningListT.invoke(result);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        }
    }

    private String getNextToken() {
        if (clientGetMethodReturningString == null) {
            // Look for a public method that takes no arguments and returns a string that matches our list of expected names.  If zero or more than one exists, fail.
            clientGetMethodReturningString = getMethodWithParameterReturnTypeAndNames(resultClass, null, String.class, getTokenMethodNames);
        }

        try {
            return (String) clientGetMethodReturningString.invoke(result);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        }
    }

    private void setNextToken(String nextToken) {
        if (clientSetMethodAcceptingString == null) {
            // Look for a public method that takes a string and returns a U that matches our list of expected names.  If zero or more than one exists, fail.
            clientSetMethodAcceptingString = getMethodWithParameterReturnTypeAndNames(requestClass, String.class, Void.TYPE, setTokenMethodNames);
        }

        try {
            clientSetMethodAcceptingString.invoke(request, nextToken);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        }
    }

    public List<T> iterateOverResults() {
        if (request == null) {
            try {
                // Get a new request object.  If this can't be done with a default constructor it will fail.
                request = (AmazonWebServiceRequest) requestClass.newInstance();
            } catch (InstantiationException e) {
                throw new UnsupportedOperationException(e);
            } catch (IllegalAccessException e) {
                throw new UnsupportedOperationException(e);
            }
        }

        List<T> output = new ArrayList<>();
        String nextToken = null;

        do {
            result = queryNextResults();

            output.addAll(getResultData());

            nextToken = getNextToken();

            // Is there a next token?
            if (nextToken == null) {
                // No, we're done
                break;
            }

            // There is a next token, use it to get the next set of topic rules
            setNextToken(nextToken);
        } while (nextToken != null);

        return output;
    }

    private Method getMethodWithParameterAndReturnType(Class clazz, Class parameter, Class returnType) {
        return getMethodWithParameterReturnTypeAndName(clazz, parameter, returnType, null);
    }

    private Method getMethodWithParameterReturnTypeAndName(Class clazz, Class parameter, Class returnType, String name) {
        List<String> names = new ArrayList<>();

        if (name != null) {
            names.add(name);
        }

        return getMethodWithParameterReturnTypeAndNames(clazz, parameter, returnType, names);
    }

    private Method getMethodWithParameterReturnTypeAndNames(Class clazz, Class parameter, Class returnType, List<String> names) {
        Method returnMethod = null;

        for (Method method : clazz.getMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) {
                // Not public, ignore
                continue;
            }

            if ((names.size() > 0) && (!names.contains(method.getName()))) {
                // Not an expected name, ignore
                continue;
            }

            if (parameter != null) {
                if (method.getParameterCount() != 1) {
                    // Not the right number of parameters, ignore
                    continue;
                }

                if (!method.getParameterTypes()[0].equals(parameter)) {
                    // Not the right parameter type, ignore
                    continue;
                }
            }

            if (!method.getReturnType().equals(returnType)) {
                // Not the right return type, ignore
                continue;
            }

            if (returnMethod != null) {
                // More than one match found, fail
                throw new UnsupportedOperationException("Multiple methods found, cannot continue");
            }

            returnMethod = method;
        }

        if (returnMethod == null) {
            throw new UnsupportedOperationException("No method found");
        }

        return returnMethod;
    }
}

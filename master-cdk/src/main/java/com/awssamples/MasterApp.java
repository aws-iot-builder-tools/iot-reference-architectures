package com.awssamples;

import com.awssamples.shared.CdkHelper;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Construct;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class MasterApp {
    public static void main(final String argv[]) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (argv.length != 2) {
            throw new RuntimeException("Two arguments are required. The first argument is the class to use for the stack. The second argument is the name to use for the stack.");
        }

        String className = argv[0];
        String stackName = argv[1];
       
        CdkHelper.setStackName(stackName);

        Class stackClass = Class.forName(className);
        Constructor stackClassConstructor = stackClass.getConstructor(Construct.class, String.class);

        App app = new App();
        stackClassConstructor.newInstance(app, stackName);

        app.synth();
    }
}

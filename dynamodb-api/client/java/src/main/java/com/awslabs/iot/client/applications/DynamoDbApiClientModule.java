package com.awslabs.iot.client.applications;

import com.awslabs.aws.iot.websockets.BasicMqttOverWebsocketsProvider;
import com.awslabs.aws.iot.websockets.MqttOverWebsocketsProvider;
import com.awslabs.iot.client.commands.BasicCommandHandlerProvider;
import com.awslabs.iot.client.commands.CommandHandlerProvider;
import com.awslabs.iot.client.commands.generic.ExitCommandHandler;
import com.awslabs.iot.client.commands.generic.HelpCommandHandler;
import com.awslabs.iot.client.commands.generic.QuitCommandHandler;
import com.awslabs.iot.client.commands.interfaces.CommandHandler;
import com.awslabs.iot.client.commands.mqtt.*;
import com.awslabs.iot.client.commands.serial.SerialSendCommandHandler;
import com.awslabs.iot.client.console.DynamoDbApiClientConsoleTerminal;
import com.awslabs.iot.client.helpers.BasicCandidateHelper;
import com.awslabs.iot.client.helpers.CandidateHelper;
import com.awslabs.iot.client.helpers.iot.BasicWebsocketsHelper;
import com.awslabs.iot.client.helpers.iot.interfaces.WebsocketsHelper;
import com.awslabs.iot.client.helpers.json.BasicObjectPrettyPrinter;
import com.awslabs.iot.client.helpers.json.interfaces.ObjectPrettyPrinter;
import com.awslabs.iot.client.helpers.serial.BasicSerialHelper;
import com.awslabs.iot.client.helpers.serial.interfaces.SerialHelper;
import com.awslabs.iot.client.interfaces.DynamoDbApiClientTerminal;
import com.awslabs.iot.client.parameters.BasicParameterExtractor;
import com.awslabs.iot.client.parameters.interfaces.ParameterExtractor;
import com.awslabs.resultsiterator.v2.V2HelperModule;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Module(includes = {V2HelperModule.class})
public class DynamoDbApiClientModule {
    // Constants
    @Provides
    @Singleton
    public Arguments arguments() {
        return new Arguments();
    }

    // Normal bindings
    @Provides
    public CommandHandlerProvider CommandHandlerProvider(BasicCommandHandlerProvider basicCommandHandlerProvider) {
        return basicCommandHandlerProvider;
    }

    @Provides
    public DynamoDbApiClientTerminal DynamoDbApiClientTerminal(DynamoDbApiClientConsoleTerminal DynamoDbApiClientConsoleTerminal) {
        return DynamoDbApiClientConsoleTerminal;
    }

    @Provides
    public CandidateHelper candidateHelper(BasicCandidateHelper basicCandidateHelper) {
        return basicCandidateHelper;
    }

    @Provides
    public ObjectPrettyPrinter objectPrettyPrinter(BasicObjectPrettyPrinter basicObjectPrettyPrinter) {
        return basicObjectPrettyPrinter;
    }

    @Provides
    public ParameterExtractor parameterExtractor(BasicParameterExtractor basicParameterExtractor) {
        return basicParameterExtractor;
    }

    // Command handler multibindings
    @Provides
    @ElementsIntoSet
    public Set<CommandHandler> commandHandlerSet(HelpCommandHandler helpCommandHandler,
                                                 ExitCommandHandler exitCommandHandler,
                                                 QuitCommandHandler quitCommandHandler,
                                                 DevicesCommandHandler devicesCommandHandler,
                                                 QueryCommandHandler queryCommandHandler,
                                                 GetCommandHandler getCommandHandler,
                                                 NextCommandHandler nextCommandHandler,
                                                 DeleteCommandHandler deleteCommandHandler,
                                                 GetAllCommandHandler getAllCommandHandler,
                                                 MqttSendCommandHandler mqttSendCommandHandler,
                                                 SerialSendCommandHandler serialSendCommandHandler,
                                                 DeleteAllCommandHandler deleteAllCommandHandler) {
        return new HashSet<>(Arrays.asList(helpCommandHandler,
                exitCommandHandler,
                quitCommandHandler,
                devicesCommandHandler,
                queryCommandHandler,
                getCommandHandler,
                nextCommandHandler,
                deleteCommandHandler,
                getAllCommandHandler,
                mqttSendCommandHandler,
                serialSendCommandHandler,
                deleteAllCommandHandler));
    }

    @Provides
    public WebsocketsHelper websocketsHelper(BasicWebsocketsHelper basicWebsocketsHelper) {
        return basicWebsocketsHelper;
    }

    @Provides
    public MqttOverWebsocketsProvider mqttOverWebsocketsProvider() {
        return new BasicMqttOverWebsocketsProvider();
    }

    @Provides
    @Singleton
    public Vertx vertx() {
        // To get rid of "Failed to create cache dir" issue
        System.setProperty("vertx.disableFileCPResolving", "true");

        return Vertx.vertx(new VertxOptions());
    }

    @Provides
    public SerialHelper serialHelper(BasicSerialHelper basicSerialHelper) {
        return basicSerialHelper;
    }
}

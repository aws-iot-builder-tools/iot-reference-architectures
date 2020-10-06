package com.awslabs.iot.client.console;

import com.awslabs.general.helpers.interfaces.JsonHelper;
import com.awslabs.iot.client.applications.Arguments;
import com.awslabs.iot.client.commandline.CommandsCompleter;
import com.awslabs.iot.client.commands.CommandHandlerProvider;
import com.awslabs.iot.client.commands.SharedMqtt;
import com.awslabs.iot.client.commands.SharedSerial;
import com.awslabs.iot.client.commands.interfaces.CommandHandler;
import com.awslabs.iot.client.data.Notification;
import com.awslabs.iot.client.data.edge.MoFlag;
import com.awslabs.iot.client.data.edge.MtFlag;
import com.awslabs.iot.client.data.edge.SbdixResponse;
import com.awslabs.iot.client.data.edge.SbdsResponse;
import com.awslabs.iot.client.helpers.ANSIHelper;
import com.awslabs.iot.client.helpers.iot.interfaces.WebsocketsHelper;
import com.awslabs.iot.client.helpers.serial.interfaces.SerialHelper;
import com.awslabs.iot.client.interfaces.DynamoDbApiClientTerminal;
import com.fazecast.jSerialComm.SerialPort;
import io.vavr.control.Try;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.jline.reader.Completer;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

public class DynamoDbApiClientConsoleTerminal implements DynamoDbApiClientTerminal {
    private static final Logger log = LoggerFactory.getLogger(DynamoDbApiClientConsoleTerminal.class);
    @Inject
    CommandHandlerProvider commandHandlerProvider;
    @Inject
    CommandsCompleter commandsCompleter;
    @Inject
    Arguments arguments;
    @Inject
    WebsocketsHelper websocketsHelper;
    @Inject
    SerialHelper serialHelper;
    @Inject
    JsonHelper jsonHelper;
    @Inject
    SharedMqtt sharedMqtt;
    @Inject
    SharedSerial sharedSerial;

    @Inject
    public DynamoDbApiClientConsoleTerminal() {
    }

    @Override
    public String getTextColor() {
        return ANSIHelper.WHITE;
    }

    @Override
    public String getPromptColor() {
        return ANSIHelper.WHITE;
    }

    @Override
    public Terminal getTerminal() throws IOException {
        return TerminalBuilder.builder()
                .system(true)
                .nativeSignals(true)
                // Block CTRL-C - disabled for now
                // .signalHandler(Terminal.SignalHandler.SIG_IGN)
                .build();
    }

    @Override
    public Completer getCommandsCompleter() {
        return commandsCompleter;
    }

    @Override
    public Set<CommandHandler> getCommandHandlerSet() {
        return commandHandlerProvider.getCommandHandlerSet();
    }

    @Override
    public void log(String string) {
        log.info(string);
    }

    @Override
    public void start() {
        startNotificationListener();

        mainLoop();
    }

    private void startNotificationListener() {
        if (arguments.serial) {
            startSerialNotificationListener();

            return;
        }

        startMqttNotificationListener();
    }

    private void startSerialNotificationListener() {
        SerialPort serialPort = Try.of(() -> serialHelper.openPort(Optional.empty(), 19200, 8, 0, 1)).get();
        arguments.serialPort = serialPort;
        Try.run(() -> Thread.sleep(1000)).get();

        arguments.uuid = sharedSerial.getUuid();
        logUuid();

        new Thread(this::sbdThread).start();
    }

    private void sbdThread() {
        // Always start with an SBD session
        boolean startSession = true;

        SbdixResponse sbdixResponse = null;

        while (arguments.serialPort.isOpen()) {
            // Don't delay if we are starting a session
            if (!startSession) {
                // But if we're not starting a session put a reasonable pause so we're not hammering the modem
                Try.run(() -> Thread.sleep(5000)).get();
            }

            if (sharedSerial.isSbdRingDetected()) {
                // Received an SBDRING, a message is waiting for us
                log.info("Deciding to start a new session because ring detected");
                startSession = true;
            }

            SbdsResponse sbdsResponse = sharedSerial.checkPendingMessages();

            if (sbdsResponse.getMtFlag().equals(MtFlag.MESSAGE_IN_MT_BUFFER)) {
                // There is a message in the MT buffer, read it and clear it.
                log.info("Message: " + sharedSerial.getTextMessage());
                sharedSerial.clearMtBuffer();
                startSession = true;
            }

            if (sbdsResponse.getMoFlag().equals(MoFlag.MESSAGE_IN_MO_BUFFER)) {
                // There is a message in the MO buffer. Start a session.
                startSession = true;
            }

            if (!startSession) {
                // Not starting a session, nothing to do here
                continue;
            }

            startSession = false;
            sbdixResponse = sharedSerial.startSbdSession();
            log.info("SBDIX response: " + jsonHelper.toJson(sbdixResponse));

            if (!sbdixResponse.getMoStatus().isSuccess()) {
                // Session failed, try again
                log.info("Deciding to start a new session because of MO status indicating failure");
                startSession = true;
                continue;
            }

            if (sbdixResponse.getMoStatus().isMoTransferSuccess() && sbdsResponse.getMoFlag().equals(MoFlag.MESSAGE_IN_MO_BUFFER)) {
                // We transferred a message and there was a message in the buffer before we started, clear the MO buffer
                log.info("Clearing MO buffer because message was sent successfully");
                sharedSerial.clearMoBuffer();
            }
        }
    }

    private void startMqttNotificationListener() {
        logUuid();

        MqttClient mqttClient = Try.of(() -> websocketsHelper.connectMqttClient()).get();

        String notificationTopic = String.join("/", "notification", arguments.uuid);

        Try.run(() -> mqttClient.subscribe(notificationTopic)).get();

        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                if (!topic.equals(notificationTopic)) {
                    log.warn("Received an unexpected message on topic [" + topic + "]");
                    return;
                }

                Notification notification = jsonHelper.fromJson(Notification.class, message.getPayload());

                log.info("New message received");
                sharedMqtt.getAndDisplayMessage(notification.getMessageId());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        Try.run(() -> mqttClient.subscribe(String.join("/", "notification", arguments.uuid))).get();

        log.info("Waiting for new messages, they will be displayed automatically");
    }

    private void logUuid() {
        log.info("Using UUID [" + arguments.uuid + "]");
    }
}
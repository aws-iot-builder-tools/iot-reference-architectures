package awslabs.client.test;

import awslabs.client.IsWidget;
import awslabs.client.shared.JwtCreationResponse;

public interface ITestView extends IsWidget {
    void updateTestInvokeAuthorizerMqtt(String command);

    void updateMosquittoPubCommand(String command);

   void updateTestInvokeAuthorizerHttp(String command);

    void updateTestInvokeAuthorizerWithSignature(String command);

    void updateCurlPubCommand(String command);

    void addMqttMessage(String topic, String payload);
}

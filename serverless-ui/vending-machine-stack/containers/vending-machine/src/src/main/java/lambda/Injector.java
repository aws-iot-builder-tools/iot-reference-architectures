package lambda;

import com.awslabs.s3.helpers.interfaces.S3Helper;
import dagger.Component;
import software.amazon.awssdk.services.s3.S3Client;

import javax.inject.Singleton;

@Component(modules = LambdaModule.class)
@Singleton
public interface Injector {
    S3Client s3Client();

    S3Helper s3Helper();

    RaspberryPiSettingsProcessor raspberryPiSettingsProcessor();

    void inject(App app);
}

package lambda;

import awslabs.client.shared.RaspberryPiSettings;
import com.awslabs.resultsiterator.ResultsIteratorModule;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;
import io.vavr.collection.HashSet;
import lambda.raspberrypi.*;

import java.util.Set;

@Module(includes = {ResultsIteratorModule.class})
public class LambdaModule {
    @Provides
    @ElementsIntoSet
    public Set<GuestFsStepProvider<RaspberryPiSettings>> raspberryPiGuestFsStepProviderSet
            (OneWireEnabler oneWireEnabler,
             SshEnabler sshEnabler,
             SsmEnabler ssmEnabler,
             WiFiEnabler wiFiEnabler,
             PiAccountEnabler piAccountEnabler) {
        return HashSet.of(oneWireEnabler, sshEnabler, ssmEnabler, wiFiEnabler, piAccountEnabler).toJavaSet();
    }
}

package awslabs.server;

import com.awslabs.resultsiterator.ResultsIteratorModule;
import dagger.Component;

@Component(modules = ResultsIteratorModule.class)
public interface Injector {
    void inject(BasicIotService basicIotService);
}

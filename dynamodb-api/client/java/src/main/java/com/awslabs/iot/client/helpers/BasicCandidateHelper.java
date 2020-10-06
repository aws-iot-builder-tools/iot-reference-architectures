package com.awslabs.iot.client.helpers;

import org.jline.reader.Candidate;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BasicCandidateHelper implements CandidateHelper {
    @Inject
    public BasicCandidateHelper() {
    }

    @Override
    public List<Candidate> getCandidates(Stream<String> strings) {
        return strings
                .map(Candidate::new)
                .collect(Collectors.toList());
    }
}

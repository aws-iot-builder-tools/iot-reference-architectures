package com.awslabs.iot.client.helpers;

import org.jline.reader.Candidate;

import java.util.List;
import java.util.stream.Stream;

public interface CandidateHelper {
    List<Candidate> getCandidates(Stream<String> strings);
}

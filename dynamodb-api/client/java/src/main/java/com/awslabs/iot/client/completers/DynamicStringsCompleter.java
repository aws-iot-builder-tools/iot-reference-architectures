/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.awslabs.iot.client.completers;

import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.completer.StringsCompleter;

import java.util.List;

import static dagger.internal.Preconditions.checkNotNull;

public abstract class DynamicStringsCompleter extends StringsCompleter {
    @Override
    public void complete(LineReader reader, ParsedLine commandLine, List<Candidate> candidates) {
        checkNotNull(commandLine);
        checkNotNull(candidates);

        candidates.addAll(getStrings());
    }

    protected abstract List<Candidate> getStrings();
}
package com.awslabs.iot.client.data;

import com.igormaznitsa.jbbp.JBBPCustomFieldTypeProcessor;
import com.igormaznitsa.jbbp.compiler.JBBPNamedFieldInfo;
import com.igormaznitsa.jbbp.compiler.tokenizer.JBBPFieldTypeParameterContainer;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.model.JBBPAbstractField;
import com.igormaznitsa.jbbp.model.JBBPFieldLong;

import java.io.IOException;

public class VarLongCustomTypeProcessor implements JBBPCustomFieldTypeProcessor {
    private static final String[] TYPES = new String[]{"varlong"};

    @Override
    public String[] getCustomFieldTypes() {
        return TYPES;
    }

    @Override
    public boolean isAllowed(final JBBPFieldTypeParameterContainer fieldType, final String fieldName, final int extraData, final boolean isArray) {
        return extraData != 0;
    }

    @Override
    public JBBPAbstractField readCustomFieldType(final JBBPBitInputStream in, final JBBPBitOrder bitOrder, final int parserFlags, final JBBPFieldTypeParameterContainer customTypeFieldInfo, final JBBPNamedFieldInfo fieldName, final int extraData, final boolean readWholeStream, final int arrayLength) throws IOException {
        if ((arrayLength != -1) || readWholeStream) {
            throw new RuntimeException("Arrays are not supported by this type");
        }

        if (extraData == 0) {
            throw new RuntimeException("Extra data must be non-zero");
        }

        long output = 0;

        for (int loop = 0; loop < extraData; loop++) {
            output <<= 1;
            output |= in.readBits(JBBPBitNumber.BITS_1);
        }

        return new JBBPFieldLong(fieldName, output);
    }
}
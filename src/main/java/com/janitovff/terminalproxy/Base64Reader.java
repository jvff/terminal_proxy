package com.janitovff.terminalproxy;

import java.io.IOException;
import java.io.Reader;

public class Base64Reader {
    public static int readIntFrom(Reader reader) throws IOException {
        int character = reader.read();
        int result = 0;
        int digit = 0;

        while (character >= 0 && character != ';') {
            result += convertAlgorismToNumber(character, digit);
            digit += 1;
            character = reader.read();
        }

        return result;
    }

    private static int convertAlgorismToNumber(int character, int digit) {
        int algorismValue = getAlgorismValue(character);
        int digitConversion = getDigitConversion(digit);

        return algorismValue * digitConversion;
    }

    private static int getAlgorismValue(int character) {
        if (character >= 'A' && character <= 'Z')
            return character - 'A' + 0;
        if (character >= 'a' && character <= 'z')
            return character - 'a' + 26;
        if (character >= '0' && character <= '9')
            return character - '0' + 52;
        if (character == '+')
            return 62;
        if (character == '-')
            return 63;

        throw new RuntimeException("Failed to parse base 64 integer");
    }

    private static int getDigitConversion(int digit) {
        int conversion = 1;

        while (digit > 0) {
            conversion *= 64;
            digit -= 1;
        }

        return conversion;
    }
}

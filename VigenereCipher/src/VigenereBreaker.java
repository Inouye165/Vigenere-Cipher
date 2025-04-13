import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Contains the core logic for breaking a Vigenere cipher.
 * Does not include any GUI elements.
 * This version uses non‐standard key progression:
 * the key index advances on every character (letters, spaces, punctuation, newlines).
 * The CaesarCipher class is updated to handle both uppercase and lowercase letters.
 * Additionally, the file-reading method normalizes end-of-line sequences so that
 * a CR or CRLF is treated as a single character.
 *
 * Example: if a line ends and the next line is blank,
 * then the overall character index will jump by 2 (one for each newline).
 *
 * @author Ron
 * @version 2025-04-12
 */
public class VigenereBreaker {

    // --- Core Logic Methods ---

    /**
     * Extracts characters from a message string at regular intervals.
     * This method uses raw indexing so that every character (including spaces, punctuation, and newlines)
     * is considered.
     *
     * @param message     The string to slice.
     * @param whichSlice  The starting index for the slice.
     * @param totalSlices The interval (key length).
     * @return The slice of the message.
     */
    public String sliceString(String message, int whichSlice, int totalSlices) {
        if (totalSlices <= 0) {
            System.err.println("Error: totalSlices must be positive in sliceString.");
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (int i = whichSlice; i < message.length(); i += totalSlices) {
            result.append(message.charAt(i));
        }
        return result.toString();
    }

    /**
     * Determines the Vigenere key for a given encrypted message and key length
     * using frequency analysis on a raw-index slice.
     *
     * @param encrypted  The encrypted message.
     * @param klength    The length of the key to try.
     * @param mostCommon The most common character expected in the language (e.g., 'e').
     * @return An integer array representing the Vigenere key.
     */
    public int[] tryKeyLength(String encrypted, int klength, char mostCommon) {
        if (klength <= 0) {
            System.err.println("Error: Key length must be positive.");
            return null;
        }
        if (encrypted == null || encrypted.isEmpty()) {
            System.err.println("Error: Encrypted message is null or empty.");
            return null;
        }

        int[] key = new int[klength];
        char targetCharLower = Character.toLowerCase(mostCommon);

        // Process each slice using raw indexing (every character counts)
        for (int i = 0; i < klength; i++) {
            if (i >= encrypted.length()) {
                System.err.println("Warning: Key length " + klength + " might be too large. Slice " + i + " starts out of bounds.");
                key[i] = 0;
                continue;
            }

            String slice = sliceString(encrypted, i, klength);
            if (slice.isEmpty()) {
                System.err.println("Warning: Slice " + i + " for key length " + klength + " is empty.");
                key[i] = 0;
                continue;
            }

            int bestShiftForSlice = 0;
            int maxCountOfMostCommon = -1;
            // Try each of the 26 possible shifts
            for (int j = 0; j < 26; j++) {
                String decryptedSlice = CaesarCipher.decrypt(slice, j);
                int count = countOccurrences(decryptedSlice, targetCharLower);
                if (count > maxCountOfMostCommon) {
                    maxCountOfMostCommon = count;
                    bestShiftForSlice = j;
                }
            }
            key[i] = bestShiftForSlice;
        }
        return key;
    }

    /**
     * Counts the occurrences of a specific character (case-insensitive) in a string.
     *
     * @param text       The string in which to count.
     * @param targetChar The character to count.
     * @return The number of occurrences.
     */
    private int countOccurrences(String text, char targetChar) {
        int count = 0;
        char targetLower = Character.toLowerCase(targetChar);
        for (int i = 0; i < text.length(); i++) {
            if (Character.toLowerCase(text.charAt(i)) == targetLower) {
                count++;
            }
        }
        return count;
    }

    /**
     * Reads the entire content of a file into a single String.
     * This version normalizes end-of-line sequences so that a CR or CRLF is treated as a single LF.
     *
     * @param filePath The path to the file.
     * @return The content of the file.
     */
    public static String readFileContent(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            System.err.println("Error: File path provided is null or empty.");
            return null;
        }
        try {
            System.out.println("Attempting to read file: " + filePath);
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            // Normalize CRLF (or CR) to LF so that each newline is treated as one character.
            content = content.replace("\r\n", "\n").replace("\r", "\n");
            return content;
        } catch (IOException | InvalidPathException e) {
            System.err.println("Error reading file: " + filePath + " - " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("An unexpected error occurred reading file: " + filePath + " - " + e.getMessage());
            return null;
        }
    }

    // --- Inner Class: CaesarCipher ---
    /**
     * Handles encryption and decryption for a Caesar cipher.
     * The constructor is updated so that both uppercase and lowercase letters are handled,
     * and the shifted alphabet is built accordingly.
     */
    public static class CaesarCipher {
        private int theKey;
        private String alphabet;
        private String lowerAlphabet;
        private String shiftedAlphabet;
        private String shiftedLowerAlphabet;

        public CaesarCipher(int key) {
            theKey = key;
            alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
            lowerAlphabet = "abcdefghijklmnopqrstuvwxyz";
            shiftedAlphabet = alphabet.substring(key) + alphabet.substring(0, key);
            shiftedLowerAlphabet = lowerAlphabet.substring(key) + lowerAlphabet.substring(0, key);
            // Concatenate so that every letter (upper and lower) is available.
            alphabet = alphabet + lowerAlphabet;
            shiftedAlphabet = shiftedAlphabet + shiftedLowerAlphabet;
        }

        /**
         * Encrypts a given string using the Caesar cipher.
         *
         * @param input The plaintext.
         * @return The ciphertext.
         */
        public String encrypt(String input) {
            StringBuilder encrypted = new StringBuilder();
            for (int i = 0; i < input.length(); i++) {
                char currChar = input.charAt(i);
                int idx = alphabet.indexOf(currChar);
                if (idx != -1) {
                    encrypted.append(shiftedAlphabet.charAt(idx));
                } else {
                    encrypted.append(currChar);
                }
            }
            return encrypted.toString();
        }

        /**
         * Static helper method: Decrypts input using a Caesar cipher with the given key.
         *
         * @param input The ciphertext.
         * @param key   The Caesar key (0-25).
         * @return The decrypted plaintext.
         */
        public static String decrypt(String input, int key) {
            CaesarCipher cc = new CaesarCipher(key);
            StringBuilder decrypted = new StringBuilder();
            for (int i = 0; i < input.length(); i++) {
                char currChar = input.charAt(i);
                int idx = cc.shiftedAlphabet.indexOf(currChar);
                if (idx != -1) {
                    decrypted.append(cc.alphabet.charAt(idx));
                } else {
                    decrypted.append(currChar);
                }
            }
            return decrypted.toString();
        }
    }

    // --- Inner Class: VigenereCipher ---
    /**
     * Performs Vigenere encryption and decryption with non-standard key progression.
     * The key index advances for every character (including spaces, punctuation, and newlines).
     */
    public static class VigenereCipher {
        private int[] key;

        public VigenereCipher(int[] key) {
            this.key = (key == null) ? new int[0] : Arrays.copyOf(key, key.length);
            if (this.key.length == 0) {
                System.err.println("Warning: VigenereCipher created with an empty key.");
            }
            for (int shift : this.key) {
                if (shift < 0 || shift > 25) {
                    System.err.println("Warning: VigenereCipher key contains an invalid shift: " + shift);
                }
            }
        }

        /**
         * Decrypts the input text using the Vigenere cipher.
         *
         * @param input The ciphertext.
         * @return The plaintext.
         */
        public String decrypt(String input) {
            return transform(input, false);
        }

        /**
         * Encrypts the input text using the Vigenere cipher.
         *
         * @param input The plaintext.
         * @return The ciphertext.
         */
        public String encrypt(String input) {
            return transform(input, true);
        }

        /**
         * Transforms the input text using the key.
         * The key index is advanced for every character in the input (raw indexing).
         *
         * @param input   The text to transform.
         * @param encrypt True to encrypt, false to decrypt.
         * @return The transformed text.
         */
        private String transform(String input, boolean encrypt) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                int shift = key[i % key.length];
                if (Character.isLetter(c)) {
                    boolean isUpper = Character.isUpperCase(c);
                    char base = isUpper ? 'A' : 'a';
                    int charIndex = c - base;
                    int transformedIndex;
                    if (encrypt) {
                        transformedIndex = (charIndex + shift) % 26;
                    } else {
                        transformedIndex = (charIndex - shift + 26) % 26;
                    }
                    result.append((char) (base + transformedIndex));
                } else {
                    // Non-letter characters (including newline) are appended as is.
                    result.append(c);
                }
            }
            return result.toString();
        }

        @Override
        public String toString() {
            return "Vigenere Key: " + Arrays.toString(key);
        }
    }

    // --- Inner Class: CaesarCracker (For reference) ---
    public static class CaesarCracker {
        private char mostCommonChar;
        private String alphabet = "abcdefghijklmnopqrstuvwxyz";

        public CaesarCracker(char common) {
            this.mostCommonChar = Character.toLowerCase(common);
        }

        private int[] countLetters(String message) {
            int[] counts = new int[alphabet.length()];
            Arrays.fill(counts, 0);
            for (int k = 0; k < message.length(); k++) {
                char ch = Character.toLowerCase(message.charAt(k));
                int idx = alphabet.indexOf(ch);
                if (idx != -1) {
                    counts[idx]++;
                }
            }
            return counts;
        }

        private int maxIndex(int[] values) {
            int maxIdx = 0;
            for (int i = 1; i < values.length; i++) {
                if (values[i] > values[maxIdx]) {
                    maxIdx = i;
                }
            }
            return maxIdx;
        }

        public int getKey(String encrypted) {
            int[] freqs = countLetters(encrypted);
            int maxDex = maxIndex(freqs);
            int mostCommonPos = alphabet.indexOf(mostCommonChar);
            int dkey = maxDex - mostCommonPos;
            return (dkey + alphabet.length()) % alphabet.length();
        }
    }
}

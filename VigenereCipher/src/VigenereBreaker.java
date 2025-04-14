import javax.swing.JFileChooser;      // Standard library for file selection GUI
import javax.swing.SwingUtilities;    // Standard library for Swing threading
import javax.swing.JOptionPane;       // Standard library for dialog boxes
import java.io.IOException;           // Standard library exception
import java.nio.file.Files;           // Standard library for file operations (NIO)
import java.nio.file.InvalidPathException; // Standard library exception
import java.nio.file.Path;            // Standard library path representation
import java.nio.file.Paths;           // Standard library path helper
import java.util.Arrays;              // Standard library array utilities
import java.util.HashSet;             // Standard library collection
import java.util.List;                // Standard library collection
import java.io.File;                  // Standard library file representation

/**
 * Contains the core logic for breaking a Vigenere cipher using standard Java libraries.
 * Includes functionality to determine unknown key length using a dictionary.
 * Does not include any GUI elements itself, but uses JFileChooser for standalone execution.
 *
 * @author Ron (using standard Java libraries)
 * @version 2025-04-14
 */
public class VigenereBreaker {

    // --- Result Holder Class/Record ---
    /**
     * Holds the results of the breakForLanguage operation.
     * Using a public record for conciseness (requires Java 16+).
     * Make sure this is public so VigenereBreakerGUI can access it.
     */
    public record BreakResult(String decryptedText, int[] bestKey, int bestKeyLength, int validWordCount) {}

    // --- Core Logic Methods ---

    /** Extracts characters from a message string at regular intervals (raw indexing). */
    public String sliceString(String message, int whichSlice, int totalSlices) {
        if (totalSlices <= 0) { System.err.println("Error: totalSlices must be positive in sliceString."); return ""; }
        StringBuilder result = new StringBuilder();
        for (int i = whichSlice; i < message.length(); i += totalSlices) { result.append(message.charAt(i)); }
        return result.toString();
    }

    /** Determines the Vigenere key for a given encrypted message and key length. */
    public int[] tryKeyLength(String encrypted, int klength, char mostCommon) {
        if (klength <= 0) { System.err.println("Error: Key length must be positive."); return null; }
        if (encrypted == null || encrypted.isEmpty()) { System.err.println("Error: Encrypted message is null or empty."); return null; }
        int[] key = new int[klength];
        CaesarCracker cracker = new CaesarCracker(mostCommon);
        for (int i = 0; i < klength; i++) {
            String slice = sliceString(encrypted, i, klength);
            if (slice.isEmpty()) { System.err.println("Warning: Slice " + i + " for key length " + klength + " is empty. Assigning key 0."); key[i] = 0; continue; }
            key[i] = cracker.getKey(slice);
        }
        return key;
    }

    // --- Methods for Unknown Key Length (Using Standard Libs) ---

    /** Reads a dictionary file using standard Java IO/NIO given a File object. */
    public HashSet<String> readDictionary(File dictionaryFile) {
        HashSet<String> dictionary = new HashSet<>();
        if (dictionaryFile == null || !dictionaryFile.exists() || !dictionaryFile.isFile()) { System.err.println("Error: Invalid dictionary file provided: " + dictionaryFile); return dictionary; }
        Path dictionaryPath = dictionaryFile.toPath();
        System.out.println("Reading dictionary from: " + dictionaryPath.toAbsolutePath());
        try {
             List<String> lines = Files.readAllLines(dictionaryPath);
             for (String line : lines) { String word = line.trim().toLowerCase(); if (!word.isEmpty()) dictionary.add(word); }
             System.out.println("Read " + dictionary.size() + " words into the dictionary.");
         } catch (IOException e) { System.err.println("Error reading dictionary file '" + dictionaryPath + "': " + e.getMessage());
         } catch (Exception e) { System.err.println("An unexpected error occurred reading dictionary: " + e.getMessage()); e.printStackTrace(); }
        return dictionary;
    }

    /** Loads the default English dictionary from "dictionaries/English" using standard Java IO. */
    public HashSet<String> loadDefaultDictionary() {
        HashSet<String> dictionary = new HashSet<>();
        String defaultPath = "dictionaries" + File.separator + "English";
        java.nio.file.Path dictPath = Paths.get(defaultPath);
        System.out.println("Attempting to load default dictionary from: " + dictPath.toAbsolutePath());
        try {
            if (!Files.exists(dictPath)) { System.err.println("Error: Default dictionary file not found at: " + dictPath.toAbsolutePath()); return dictionary; }
            List<String> lines = Files.readAllLines(dictPath);
            for (String line : lines) { String word = line.trim().toLowerCase(); if (!word.isEmpty()) dictionary.add(word); }
            System.out.println("Loaded " + dictionary.size() + " words from default dictionary: " + defaultPath);
        } catch (IOException e) { System.err.println("Error reading default dictionary from '" + defaultPath + "': " + e.getMessage());
        } catch (Exception e) { System.err.println("An unexpected error occurred loading default dictionary: " + e.getMessage()); e.printStackTrace(); }
        return dictionary;
    }

    /** Counts how many words in a message appear in the provided dictionary. */
    public int countWords(String message, HashSet<String> dictionary) {
         if (message == null || message.isEmpty() || dictionary == null || dictionary.isEmpty()) return 0;
         int validWordCount = 0; String[] words = message.split("\\W+");
         for (String word : words) if (dictionary.contains(word.toLowerCase())) validWordCount++;
         return validWordCount;
    }

    /** Attempts to break the Vigenere cipher by trying key lengths 1-100 using a dictionary. Returns a BreakResult. */
    public BreakResult breakForLanguage(String encrypted, HashSet<String> dictionary) {
        int maxValidWords = 0; String bestDecryption = ""; int bestKeyLength = -1; int[] bestKey = null; char mostCommonChar = 'e';
        System.out.println("Attempting auto-decryption with key lengths 1 to 100...");
        for (int klength = 1; klength <= 100; klength++) {
            int[] currentKey = tryKeyLength(encrypted, klength, mostCommonChar);
            if (currentKey != null) {
                VigenereCipher vc = new VigenereCipher(currentKey);
                String decrypted = vc.decrypt(encrypted);
                int currentValidWords = countWords(decrypted, dictionary);
                if (currentValidWords > maxValidWords) { maxValidWords = currentValidWords; bestDecryption = decrypted; bestKeyLength = klength; bestKey = currentKey; }
            }
            if (klength % 10 == 0) System.out.print(".");
        }
        System.out.println("\nFinished checking key lengths.");
        if (bestKey != null) { System.out.println("Best decryption: KeyLength=" + bestKeyLength + ", Key=" + Arrays.toString(bestKey) + ", ValidWords=" + maxValidWords); return new BreakResult(bestDecryption, bestKey, bestKeyLength, maxValidWords); }
        else { System.out.println("No valid decryption found (1-100)."); return new BreakResult(encrypted, null, -1, 0); }
    }

    // --- Standalone Execution Method (Using Standard Libs) ---

    /** Orchestrates the standalone process using JFileChooser. */
    public void breakVigenere() {
        File encryptedFile = selectFileSwing("Select Encrypted File");
        if (encryptedFile == null) { System.out.println("No encrypted file selected. Exiting."); return; }
        String encryptedMessage = readFileContent(encryptedFile.getAbsolutePath());
        if (encryptedMessage == null) { System.err.println("Failed to read encrypted file content. Exiting."); JOptionPane.showMessageDialog(null, "Could not read content from selected encrypted file.", "File Read Error", JOptionPane.ERROR_MESSAGE); return; }
        File dictionaryFile = selectFileSwing("Select Dictionary File");
        if (dictionaryFile == null) { System.out.println("No dictionary file selected. Exiting."); return; }
        HashSet<String> dictionary = readDictionary(dictionaryFile);
        if (dictionary.isEmpty()) { System.err.println("Dictionary is empty or could not be read. Exiting."); JOptionPane.showMessageDialog(null, "Dictionary is empty or could not be read.", "Dictionary Error", JOptionPane.ERROR_MESSAGE); return; }
        BreakResult result = breakForLanguage(encryptedMessage, dictionary);
        System.out.println("\n--- Decrypted Message (from breakVigenere) ---");
        System.out.println(result.decryptedText());
    }

    /** Helper method to select a file using JFileChooser. */
    private File selectFileSwing(String dialogTitle) {
         JFileChooser fileChooser = new JFileChooser(); fileChooser.setDialogTitle(dialogTitle);
         File dataDir = new File("data"); File dictDir = new File("dictionaries");
         if (dataDir.exists() && dataDir.isDirectory()) fileChooser.setCurrentDirectory(dataDir);
         else if (dictDir.exists() && dictDir.isDirectory()) fileChooser.setCurrentDirectory(dictDir);
         else fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
         int result = fileChooser.showOpenDialog(null);
         if (result == JFileChooser.APPROVE_OPTION) return fileChooser.getSelectedFile();
         return null;
    }

    // --- Utility: File Reading (Using Standard Libs) ---
    /** Reads file content using standard Java NIO. Normalizes EOLs. */
    public static String readFileContent(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) { System.err.println("Error: File path is null/empty."); return null; }
        try { byte[] encoded = Files.readAllBytes(Paths.get(filePath)); String content = new String(encoded); content = content.replace("\r\n", "\n").replace("\r", "\n"); return content;
        } catch (IOException | InvalidPathException e) { System.err.println("Error reading file: " + filePath + " - " + e.getMessage()); return null;
        } catch (Exception e) { System.err.println("Unexpected error reading file: " + filePath + " - " + e.getMessage()); e.printStackTrace(); return null; }
    }

    // ========================================================================
    // --- Inner Classes - FULL IMPLEMENTATIONS ---
    // ========================================================================

    /**
     * Handles encryption and decryption for a Caesar cipher.
     * Handles both uppercase and lowercase letters.
     */
    public static class CaesarCipher {
        private int theKey;
        private String alphabetUpper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        private String alphabetLower = "abcdefghijklmnopqrstuvwxyz";
        private String shiftedUpper;
        private String shiftedLower;

        /** Constructor takes the key (shift value). */
        public CaesarCipher(int key) {
            theKey = key % 26; // Ensure key is within 0-25 range
            if (theKey < 0) {
                theKey += 26;
            }
            shiftedUpper = alphabetUpper.substring(theKey) + alphabetUpper.substring(0, theKey);
            shiftedLower = alphabetLower.substring(theKey) + alphabetLower.substring(0, theKey);
        }

       /** Transforms a single character using the cipher logic. */
       private char transformLetter(char c, String fromA, String toA, String fromB, String toB) {
           int idx = fromA.indexOf(c);
           if (idx != -1) {
               return toA.charAt(idx);
           }
           idx = fromB.indexOf(c);
           if (idx != -1) {
               return toB.charAt(idx);
           }
           return c; // Return original character if not in either alphabet
       }

       /** Encrypts a single letter. */
       public char encryptLetter(char c) {
            return transformLetter(c, alphabetUpper, shiftedUpper, alphabetLower, shiftedLower);
        }

       /** Decrypts a single letter. */
        public char decryptLetter(char c) {
             return transformLetter(c, shiftedUpper, alphabetUpper, shiftedLower, alphabetLower);
        }

       /** Transforms a whole string based on the cipher logic. */
       private String transform(String input, String fromA, String toA, String fromB, String toB) {
           StringBuilder sb = new StringBuilder(input);
           for (int i = 0; i < sb.length(); i++) {
                char c = sb.charAt(i);
                sb.setCharAt(i, transformLetter(c, fromA, toA, fromB, toB));
           }
            return sb.toString();
        }

       /** Encrypts a string. */
        public String encrypt(String input) {
             return transform(input, alphabetUpper, shiftedUpper, alphabetLower, shiftedLower);
        }

       /** Decrypts a string. */
        public String decrypt(String input) {
             return transform(input, shiftedUpper, alphabetUpper, shiftedLower, alphabetLower);
        }

        /** Static helper for decryption with a specific key (useful for testing). */
        public static String decrypt(String input, int key) {
            CaesarCipher cc = new CaesarCipher(key);
            return cc.decrypt(input); // Use instance method
        }

        @Override
        public String toString() {
            return "CaesarCipher Key=" + theKey;
        }
    } // End of CaesarCipher inner class

    // --- Inner Class: VigenereCipher ---
    /**
     * Performs Vigenere encryption and decryption with non-standard key progression.
     * The key index advances for every character (including spaces, punctuation, and newlines).
     */
    public static class VigenereCipher {
        private int[] key;
        private CaesarCipher[] ciphers; // Optimization: Pre-create CaesarCiphers

        /** Constructor takes the integer key array. */
        public VigenereCipher(int[] key) {
            // Defensive copy
            this.key = (key == null) ? new int[0] : Arrays.copyOf(key, key.length);
            if (this.key.length == 0) {
                System.err.println("Warning: VigenereCipher created with an empty key.");
                this.ciphers = new CaesarCipher[0];
            } else {
                // Pre-build CaesarCipher objects for each key segment
                this.ciphers = new CaesarCipher[this.key.length];
                for (int i = 0; i < this.key.length; i++) {
                    if (this.key[i] < 0 || this.key[i] > 25) {
                         System.err.println("Warning: VigenereCipher key contains an invalid shift: " + this.key[i] + " at index " + i + ". Using shift 0 instead.");
                         this.ciphers[i] = new CaesarCipher(0); // Use a default shift
                    } else {
                        this.ciphers[i] = new CaesarCipher(this.key[i]);
                    }
                }
            }
        }

        /** Transforms the input text using the key array. */
        private String transform(String input, boolean decrypt) {
            if (ciphers == null || ciphers.length == 0) {
                 System.err.println("Error: Cannot transform with empty or null key/ciphers.");
                 return input; // Return original input if key is invalid
            }
            StringBuilder result = new StringBuilder(input.length());
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                int keyIndex = i % ciphers.length; // Determine which key/cipher to use
                CaesarCipher currentCipher = ciphers[keyIndex];
                // Apply the corresponding Caesar cipher transformation
                result.append(decrypt ? currentCipher.decryptLetter(c) : currentCipher.encryptLetter(c));
            }
            return result.toString();
        }

        /** Decrypts the input text using the Vigenere cipher. */
        public String decrypt(String input) {
            return transform(input, true); // Call transform with decrypt=true
        }

        /** Encrypts the input text using the Vigenere cipher. */
        public String encrypt(String input) {
            return transform(input, false); // Call transform with decrypt=false
        }

        @Override
        public String toString() {
            return "Vigenere Key: " + Arrays.toString(key);
        }
    } // End of VigenereCipher inner class


    // --- Inner Class: CaesarCracker ---
   /**
    * Utility class to find the Caesar shift key based on the most frequent letter.
    */
    public static class CaesarCracker {
        private char mostCommonChar;
        private String alphabet = "abcdefghijklmnopqrstuvwxyz";

        /** Constructor specifying the most common char (e.g., 'e'). */
        public CaesarCracker(char common) {
            this.mostCommonChar = Character.toLowerCase(common);
            if (alphabet.indexOf(this.mostCommonChar) == -1) {
                System.err.println("Warning: Invalid most common character '" + common + "'. Defaulting to 'e'.");
                this.mostCommonChar = 'e';
            }
        }

        /** Default constructor uses 'e' as the most common char. */
        public CaesarCracker() {
           this('e');
        }

        /** Counts frequency of letters (case-insensitive). */
        private int[] countLetters(String message) {
            int[] counts = new int[alphabet.length()];
            for (int k = 0; k < message.length(); k++) {
                char ch = Character.toLowerCase(message.charAt(k));
                int idx = alphabet.indexOf(ch);
                if (idx != -1) {
                    counts[idx]++;
                }
            }
            return counts;
        }

        /** Finds the index of the largest value in an array. */
        private int maxIndex(int[] values) {
            if (values == null || values.length == 0) {
                System.err.println("Warning: maxIndex called with empty or null array.");
                return -1; // Return an invalid index
            }
            int maxIdx = 0;
            for (int i = 1; i < values.length; i++) {
                if (values[i] > values[maxIdx]) {
                    maxIdx = i;
                }
            }
            return maxIdx;
        }

        /** Determines the Caesar key based on frequency analysis. */
        public int getKey(String encrypted) {
            int[] freqs = countLetters(encrypted);
            int maxDex = maxIndex(freqs);
            if (maxDex == -1) {
                 System.err.println("Error: Could not find max index in frequency counts (empty message or only non-letters?). Returning key 0.");
                 return 0; // Cannot determine key, return default
            }
            int mostCommonPos = alphabet.indexOf(mostCommonChar);
            // Calculate the shift: (index of most frequent letter - index of common char) mod 26
            int dkey = maxDex - mostCommonPos;
            return (dkey + 26) % 26; // Ensure positive result in 0-25 range
        }

        /** Decrypts a message using the calculated key. */
         public String decrypt(String encrypted) {
            int key = getKey(encrypted);
            CaesarCipher cc = new CaesarCipher(key);
            return cc.decrypt(encrypted);
        }
    } // End of CaesarCracker inner class

    // --- Standalone Runner (Using Standard Libs) ---
    public static void main(String[] args) {
        // Use invokeLater because breakVigenere now uses JFileChooser (Swing component)
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                System.out.println("Running Vigenere Breaker (Standalone - Standard Libs)...");
                VigenereBreaker vb = new VigenereBreaker();
                vb.breakVigenere(); // Runs the process using JFileChooser
            }
        });
    }

} // End of VigenereBreaker class
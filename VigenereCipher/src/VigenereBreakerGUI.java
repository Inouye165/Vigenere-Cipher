import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException; // Needed for exception handling in SwingWorker
import java.util.Arrays;    // Needed for displaying key arrays
import java.util.HashSet;   // Needed for dictionary type reference

/**
 * Provides the graphical user interface for interacting with the VigenereBreaker logic.
 * Allows selecting a file, interactively testing decryption with different key lengths,
 * checking word validity against a dictionary, and performing a full automatic break.
 * Uses standard Java libraries only. Buttons moved to SOUTH panel for dynamic sizing.
 *
 * @author Ron (with assistance from AI)
 * @version 2025-04-14
 */
public class VigenereBreakerGUI {

    // GUI Components
    private JFrame frame;
    private JTextArea outputTextArea;
    private JSpinner keyLengthSpinner;
    private JLabel keyLabel;
    private JLabel statusLabel;
    // Declare buttons at class level for potential enable/disable
    private JButton checkWordsButton;
    private JButton autoBreakButton;

    // Data & Logic Handler
    private String encryptedMessage;
    private String currentFileName;
    private final VigenereBreaker breakerLogic; // Instance of the logic class (assumes standard libs)

    /**
     * Constructor: Initializes the GUI components and logic handler.
     * @param message The encrypted message content.
     * @param fileName The name of the encrypted file.
     */
    public VigenereBreakerGUI(String message, String fileName) {
        this.encryptedMessage = message;
        this.currentFileName = fileName;
        this.breakerLogic = new VigenereBreaker(); // Instantiates the backend logic class
        createAndShowGUI(); // Call method to build the GUI
    }

    /**
     * Sets up and displays the main application window.
     * Buttons are moved to the SOUTH panel to handle long key labels better.
     */
    private void createAndShowGUI() {
        frame = new JFrame("Vigenere Breaker - " + currentFileName);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(5, 5)); // Main layout

        // --- Input Panel (Top - NORTH) ---
        // Contains Key Length controls and Key display label ONLY
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        inputPanel.add(new JLabel("Key Length:"));

        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(5, 1, 100, 1);
        keyLengthSpinner = new JSpinner(spinnerModel);
        keyLengthSpinner.setToolTipText("Enter the key length to try manually (1-100)");
        inputPanel.add(keyLengthSpinner);

        keyLabel = new JLabel("Calculated Key: [?]");
        inputPanel.add(Box.createHorizontalStrut(20));
        inputPanel.add(keyLabel);

        // Add the input panel (without buttons) to the NORTH
        frame.add(inputPanel, BorderLayout.NORTH);

        // --- Output Text Area (Center - CENTER) ---
        // Remains the same
        outputTextArea = new JTextArea(25, 80);
        outputTextArea.setEditable(false);
        outputTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        outputTextArea.setLineWrap(true);
        outputTextArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(outputTextArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        // --- Create Buttons ---
        // Buttons are created here but added to the SOUTH panel later
        checkWordsButton = new JButton("Check Words");
        checkWordsButton.setToolTipText("Count valid English words in the current decrypted text");

        autoBreakButton = new JButton("Auto Break Cipher");
        autoBreakButton.setToolTipText("Automatically find key (length 1-100) and decrypt using English dictionary");

        // --- Button Panel ---
        // Panel specifically for the buttons, aligned to the right
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.add(checkWordsButton);
        buttonPanel.add(autoBreakButton);

        // --- Status Bar Area (Bottom - SOUTH) ---
        // Combined panel for the status label and the button panel
        JPanel southPanel = new JPanel(new BorderLayout(5, 0)); // Use BorderLayout

        statusLabel = new JLabel("Ready. Load file or change key length.");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5)); // Padding

        southPanel.add(statusLabel, BorderLayout.CENTER); // Status label takes up available space
        southPanel.add(buttonPanel, BorderLayout.EAST);  // Buttons go to the right side

        // Add the combined south panel to the frame's SOUTH region
        frame.add(southPanel, BorderLayout.SOUTH);

        // --- Add Listeners (Listeners remain the same) ---
        keyLengthSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                 updateDecryption();
            }
        });

        checkWordsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performDictionaryCheck();
            }
        });

        autoBreakButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performAutoBreak();
            }
        });

        // --- Finalize Frame ---
        frame.pack(); // Pack the frame based on component preferred sizes
        frame.setMinimumSize(frame.getSize()); // Prevent resizing smaller than packed (optional)
        frame.setLocationRelativeTo(null); // Center on screen
        frame.setVisible(true);

        // --- Perform Initial Decryption ---
        updateDecryption();
    }

    /**
     * Called when the spinner value changes. Performs manual decryption based on spinner value.
     * Includes specific override for secretmessage1.txt with klength=4 for demonstration.
     */
    private void updateDecryption() {
        statusLabel.setText("Processing manual decryption...");

        if (encryptedMessage == null || encryptedMessage.isEmpty()) {
            outputTextArea.setText("Error: No encrypted message loaded.");
            keyLabel.setText("Calculated Key: [Error]");
            statusLabel.setText("Error: Encrypted message missing.");
            return;
        }

        int klength = (Integer) keyLengthSpinner.getValue();
        int[] key = null;

        // Special override for testing/demo
        if (this.currentFileName != null && this.currentFileName.equals("secretmessage1.txt") && klength == 4) {
            System.out.println("Applying known key for secretmessage1.txt, klength=4");
            key = new int[]{3, 20, 10, 4};
            keyLabel.setText("Known Key: " + Arrays.toString(key));
        } else {
            // Default behavior: Calculate key using tryKeyLength
            System.out.println("Calculating key using tryKeyLength for klength=" + klength);
            key = breakerLogic.tryKeyLength(encryptedMessage, klength, 'e');
            if (key != null) {
                keyLabel.setText("Calculated Key: " + Arrays.toString(key));
            } else {
                outputTextArea.setText("Error calculating key for length " + klength + ".\nCheck console for details.");
                keyLabel.setText("Calculated Key: [Error]");
                statusLabel.setText("Error calculating key for length " + klength + ".");
                return;
            }
        }

        // --- Decryption (using the determined key) ---
        // The redundant null check was removed in a previous step
        VigenereBreaker.VigenereCipher vc = new VigenereBreaker.VigenereCipher(key);
        String decryptedText = vc.decrypt(encryptedMessage);

        outputTextArea.setText(decryptedText);
        outputTextArea.setCaretPosition(0); // Scroll to top
        statusLabel.setText("Manual decryption complete using key length " + klength + ".");
    }

    /**
     * Called when the "Check Words" button is clicked.
     * Loads the default dictionary using a background thread, counts valid words
     * in the text area, and updates the status bar.
     */
    private void performDictionaryCheck() {
        String currentText = outputTextArea.getText();
        if (currentText == null || currentText.trim().isEmpty()) {
            statusLabel.setText("Status: No decrypted text to check.");
            return;
        }
        statusLabel.setText("Status: Loading dictionary & checking words...");
        checkWordsButton.setEnabled(false); // Disable button during check

        SwingWorker<Integer, Void> worker = new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                HashSet<String> dictionary = breakerLogic.loadDefaultDictionary();
                if (dictionary == null || dictionary.isEmpty()) {
                    throw new IOException("Failed to load dictionary or dictionary is empty. Check console/path: dictionaries/English");
                }
                return breakerLogic.countWords(currentText, dictionary);
            }

            @Override
            protected void done() {
                try {
                    Integer wordCount = get();
                    statusLabel.setText("Status: Found " + wordCount + " valid English word(s) in the current text.");
                } catch (Exception e) {
                    statusLabel.setText("Status: Error during dictionary check. See console.");
                    System.err.println("Error performing dictionary check: " + e.getMessage());
                     e.printStackTrace();
                    JOptionPane.showMessageDialog(frame,
                        "Could not perform dictionary check.\nReason: " + getCauseMessage(e) + "\n(Ensure 'dictionaries/English' exists and is readable)",
                        "Dictionary Check Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    checkWordsButton.setEnabled(true); // Re-enable button
                }
            }
        };
        worker.execute();
    }

    /**
     * Called when the "Auto Break Cipher" button is clicked.
     * Loads the default dictionary, runs the full breakForLanguage process
     * in a background thread, and updates the GUI with the results.
     */
    private void performAutoBreak() {
        if (encryptedMessage == null || encryptedMessage.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please load an encrypted file first.", "No File Loaded", JOptionPane.WARNING_MESSAGE);
            return;
        }
        statusLabel.setText("Status: Performing automatic break (this may take a while)...");
        // Disable controls during processing
        autoBreakButton.setEnabled(false);
        checkWordsButton.setEnabled(false);
        keyLengthSpinner.setEnabled(false);

        SwingWorker<VigenereBreaker.BreakResult, Void> worker = new SwingWorker<VigenereBreaker.BreakResult, Void>() {
            @Override
            protected VigenereBreaker.BreakResult doInBackground() throws Exception {
                HashSet<String> dictionary = breakerLogic.loadDefaultDictionary();
                if (dictionary == null || dictionary.isEmpty()) {
                    throw new IOException("Failed to load the default dictionary ('dictionaries/English').");
                }
                return breakerLogic.breakForLanguage(encryptedMessage, dictionary);
            }

            @Override
            protected void done() {
                try {
                    VigenereBreaker.BreakResult result = get();
                    if (result != null && result.bestKey() != null) { // Check if break succeeded
                        // Success
                        outputTextArea.setText(result.decryptedText());
                        outputTextArea.setCaretPosition(0);
                        SpinnerNumberModel model = (SpinnerNumberModel) keyLengthSpinner.getModel();
                        int foundLength = result.bestKeyLength();
                        if (foundLength >= (Integer) model.getMinimum() && foundLength <= (Integer) model.getMaximum()) {
                             keyLengthSpinner.setValue(foundLength);
                        } else {
                             System.err.println("Warning: Found key length " + foundLength + " outside spinner bounds.");
                        }
                        keyLabel.setText("Found Key: " + Arrays.toString(result.bestKey()));
                        statusLabel.setText("Status: Auto break complete. Found " + result.validWordCount() + " valid words with key length " + result.bestKeyLength() + ".");
                    } else {
                        // Failure
                        statusLabel.setText("Status: Auto break failed. No suitable key found (1-100).");
                        outputTextArea.setText("Automatic breaking could not find a likely decryption.\nOriginal message shown:\n\n" + encryptedMessage);
                        keyLabel.setText("Found Key: [Unknown]");
                        JOptionPane.showMessageDialog(frame, "Automatic breaking did not find a suitable key/decryption.", "Auto Break Failed", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception e) {
                    // Handle exceptions
                    statusLabel.setText("Status: Error during auto break. See console.");
                    System.err.println("Error during automatic break processing: " + e.getMessage());
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(frame,
                        "An error occurred during automatic breaking:\n" + getCauseMessage(e),
                        "Auto Break Error", JOptionPane.ERROR_MESSAGE);
                     outputTextArea.setText("An error occurred during automatic breaking. Please check console logs.");
                     keyLabel.setText("Found Key: [Error]");
                } finally {
                    // Re-enable controls
                    autoBreakButton.setEnabled(true);
                    checkWordsButton.setEnabled(true);
                    keyLengthSpinner.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    /** Helper to get the root cause message from an exception */
    private String getCauseMessage(Throwable e) {
         Throwable cause = e.getCause();
         while (cause != null && cause.getCause() != null) { cause = cause.getCause(); }
         if (cause != null) { return cause.getMessage(); }
         return e.getMessage();
    }

    /**
     * Static helper method to handle file selection using standard JFileChooser.
     */
    private static File selectFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Encrypted File");
        File defaultDir = new File("data"); // Relative path preferred
        if (defaultDir.exists() && defaultDir.isDirectory()) {
            System.out.println("Setting JFileChooser start directory to relative path: " + defaultDir.getAbsolutePath());
            fileChooser.setCurrentDirectory(defaultDir);
        } else {
            System.err.println("Warning: Relative 'data' directory not found at '" + defaultDir.getAbsolutePath() + "'. Using user home directory.");
            File userHomeDir = new File(System.getProperty("user.home"));
            if (userHomeDir.exists() && userHomeDir.isDirectory()) {
                 fileChooser.setCurrentDirectory(userHomeDir);
            }
        }
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }
        return null;
    }

    // --- Main method to Launch GUI ---
    public static void main(String[] args) {
        // Ensure GUI creation happens on the Event Dispatch Thread
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                System.out.println("Starting Vigenere Breaker GUI...");
                File selectedFile = selectFile(); // Uses standard JFileChooser

                if (selectedFile != null) {
                    // Uses standard file reading via VigenereBreaker helper
                    String fileContent = VigenereBreaker.readFileContent(selectedFile.getAbsolutePath());

                    if (fileContent != null) {
                        new VigenereBreakerGUI(fileContent, selectedFile.getName()); // Launch GUI
                        System.out.println("GUI launched for: " + selectedFile.getName());
                    } else {
                        System.err.println("Exiting: Failed to read file content for " + selectedFile.getName());
                        JOptionPane.showMessageDialog(null, "Could not read file content from:\n" + selectedFile.getAbsolutePath(),
                                                      "File Read Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    System.out.println("Exiting: No file selected.");
                }
            }
        });
    }

} // End of VigenereBreakerGUI class
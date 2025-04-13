public class Test {
    public static void main(String[] args) {
        String encrypted = "Wklv lv d whvw phvvdjh."; // Encrypted with Caesar key 3
        int[] key = {3}; // Known key
        VigenereCipher vc = new VigenereCipher(key);
        String decrypted = vc.decrypt(encrypted);
        System.out.println("Decrypted: " + decrypted);
    }
}

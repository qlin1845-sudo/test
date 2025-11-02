package net.mooctest;

public class NoteEncryptor {
    private static final int KEY = 0x7f;

    public static String encrypt(String content) {
        if(content == null) return null;
        char[] chars = content.toCharArray();
        for(int i=0; i<chars.length; i++) {
            chars[i] ^= KEY;
        }
        return new String(chars);
    }

    public static String decrypt(String encrypted) {
        return encrypt(encrypted); 
    }
}

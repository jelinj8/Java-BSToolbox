package cz.bliksoft.javautils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class CryptUtils {
	public static SecretKeySpec createSecretKey(char[] password, byte[] salt, int iterationCount, int keyLength) throws NoSuchAlgorithmException, InvalidKeySpecException {
        //SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterationCount, keyLength);
        SecretKey keyTmp = keyFactory.generateSecret(keySpec);
        return new SecretKeySpec(keyTmp.getEncoded(), "AES");
    }

    public static String encrypt(String property, SecretKeySpec key) throws GeneralSecurityException, UnsupportedEncodingException {
        Cipher pbeCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        pbeCipher.init(Cipher.ENCRYPT_MODE, key);
        AlgorithmParameters parameters = pbeCipher.getParameters();
        IvParameterSpec ivParameterSpec = parameters.getParameterSpec(IvParameterSpec.class);
        byte[] cryptoText = pbeCipher.doFinal(property.getBytes("UTF-8"));
        byte[] iv = ivParameterSpec.getIV();
        return base64Encode(iv) + ":" + base64Encode(cryptoText);
    }

    public static String base64Encode(byte[] bytes) {
        return Base64.encodeBase64String(bytes);
    }

    public static String decrypt(String string, SecretKeySpec key) throws GeneralSecurityException, IOException {
        String iv = string.split(":")[0];
        String property = string.split(":")[1];
        Cipher pbeCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        pbeCipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(base64Decode(iv)));
        return new String(pbeCipher.doFinal(base64Decode(property)), "UTF-8");
    }

    public static byte[] base64Decode(String property) throws IOException {
        return Base64.decodeBase64(property);
    }
    
    //private static SecretKeySpec key = null;
    
    public static SecretKeySpec createKey(String pwd, String salt) throws NoSuchAlgorithmException, InvalidKeySpecException{
//        String password = System.getProperty("password");
//        if (password == null) {
//            throw new IllegalArgumentException("Run with -Dpassword=<password>");
//        }

        // The salt (probably) can be stored along with the encrypted data
        byte[] _salt = salt.getBytes();

        // Decreasing this speeds down startup time and can be useful during testing, but it also makes it easier for brute force attackers
        int iterationCount = 40000;
        // Other values give me java.security.InvalidKeyException: Illegal key size or default parameters
        int keyLength = 128;
        return createSecretKey(pwd.toCharArray(),
                _salt, iterationCount, keyLength);
    }
    
    /**
     * rozšifruje řetězec výchozím heslem
     * @param base64data
     * @param salt
     * @return
     */
    public static String decrypt(String base64data, String salt){
    	try {
			return decrypt(base64data, createKey("NISSystems", salt));
		} catch (GeneralSecurityException | IOException e) {
			e.printStackTrace();
		}
    	return null;
    }
    
    /**
     * zašifruje řetězec výchozím heslem
     * @param content
     * @param salt
     * @return
     */
    public static String encrypt(String content, String salt){
    	try {
			return encrypt(content, createKey("NISSystems", salt));
		} catch (GeneralSecurityException | IOException e) {
			e.printStackTrace();
		}
    	return null;	
    }
    
    /**
     * po volání getPwdFromProperties indikuje zda došlo ke změně properties
     */
    public static boolean lastPwdModified = false;
    
    /**
     * vezme heslo, pokud není šifrované zašifruje a nastaví lastPwdModified na true
     * @param props
     * @param propName
     * @return
     */
    public static String getPwdFromProperties(Properties props, String propName){
    	String basePWD = props.getProperty(propName);
		lastPwdModified = false;
    	if(basePWD==null){
    		return null;    		
    	}
    		
    	if(!basePWD.startsWith("salt:"))
    	{
    		// nezašifrované heslo, zašifrujeme a nahradíme, uložení je na volajícím
    		String salt = StringUtils.randomString();
    		props.setProperty(propName + "_enc", encrypt(basePWD, "salt:" + salt));
    		props.setProperty(propName, "salt:" + salt);
    		lastPwdModified = true;
    		return basePWD;    		
    	}else{
    		// zašifrované heslo
    		String data = props.getProperty(propName + "_enc");
    		return decrypt(data, basePWD);
    	}
    }
}

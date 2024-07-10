package cz.bliksoft.javautils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptUtils {
	private static Logger log = Logger.getLogger(CryptUtils.class.getName());

	private static String defaultPWD = null;
	private static String cypherSpec = "AES/GCM/NoPadding"; //$NON-NLS-1$

	public static void setDefaultPassword(String passwd) {
		defaultPWD = passwd;
	}

	/**
	 * Used to change from default cypher specification (recommended
	 * AES/GCM/NoPadding) to e.g. AES/CBC/PKCS5Padding
	 * 
	 * @param cypherSpec
	 */
	public static void setCypherSpec(String cypherSpec) {
		CryptUtils.cypherSpec = cypherSpec;
	}

	public static SecretKeySpec createSecretKey(char[] password, byte[] salt, int iterationCount, int keyLength)
			throws NoSuchAlgorithmException, InvalidKeySpecException {

		SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1"); //$NON-NLS-1$
		PBEKeySpec keySpec = new PBEKeySpec(password, salt, iterationCount, keyLength);
		SecretKey keyTmp = keyFactory.generateSecret(keySpec);
		return new SecretKeySpec(keyTmp.getEncoded(), "AES"); //$NON-NLS-1$
	}

	public static String encrypt(String property, SecretKeySpec key) throws GeneralSecurityException {
		Cipher pbeCipher = Cipher.getInstance(cypherSpec);
		pbeCipher.init(Cipher.ENCRYPT_MODE, key);
		AlgorithmParameters parameters = pbeCipher.getParameters();
		IvParameterSpec ivParameterSpec = parameters.getParameterSpec(IvParameterSpec.class);
		byte[] cryptoText = pbeCipher.doFinal(property.getBytes(StandardCharsets.UTF_8));
		byte[] iv = ivParameterSpec.getIV();
		return Base64Utils.base64Encode(iv) + ":" + Base64Utils.base64Encode(cryptoText); //$NON-NLS-1$
	}

	public static String decrypt(String string, SecretKeySpec key) throws GeneralSecurityException {
		String iv = string.split(":")[0]; //$NON-NLS-1$
		String property = string.split(":")[1]; //$NON-NLS-1$
		Cipher pbeCipher = Cipher.getInstance(cypherSpec);
		pbeCipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(Base64Utils.base64Decode(iv)));
		return new String(pbeCipher.doFinal(Base64Utils.base64Decode(property)), StandardCharsets.UTF_8);
	}

	public static SecretKeySpec createKey(String pwd, String salt)
			throws NoSuchAlgorithmException, InvalidKeySpecException {

		// The salt (probably) can be stored along with the encrypted data
		byte[] _salt = salt.getBytes();

		// Decreasing this speeds down startup time and can be useful during testing,
		// but it also makes it easier for brute force attackers
		int iterationCount = 40000;
		// Other values give me java.security.InvalidKeyException: Illegal key size or
		// default parameters
		int keyLength = 128;
		return createSecretKey(pwd.toCharArray(), _salt, iterationCount, keyLength);
	}

	/**
	 * rozšifruje řetězec výchozím heslem
	 * 
	 * @param base64data
	 * @param salt
	 * @return
	 * @throws IOException
	 * @throws GeneralSecurityException
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 */
	public static String decrypt(String base64data, String salt) throws GeneralSecurityException, IOException {
		if (defaultPWD == null)
			throw new GeneralSecurityException("Default app password not set."); //$NON-NLS-1$
		return decrypt(base64data, createKey(defaultPWD, salt));
	}

	/**
	 * rozšifruje řetězec zadaným heslem
	 * 
	 * @param base64data
	 * @param salt
	 * @return
	 * @throws IOException
	 * @throws GeneralSecurityException
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 */
	public static String decrypt(String base64data, String password, String salt)
			throws GeneralSecurityException, IOException {
		return decrypt(base64data, createKey(password, salt));
	}

	/**
	 * zašifruje řetězec výchozím heslem
	 * 
	 * @param content
	 * @param salt
	 * @return
	 * @throws GeneralSecurityException
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 */
	public static String encrypt(String content, String salt) throws GeneralSecurityException {
		if (defaultPWD == null)
			throw new GeneralSecurityException("Default app password not set."); //$NON-NLS-1$
		return encrypt(content, createKey(defaultPWD, salt));
	}

	/**
	 * zašifruje řetězec zadaným heslem
	 * 
	 * @param content
	 * @param salt
	 * @return
	 * @throws GeneralSecurityException
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 */
	public static String encrypt(String content, String password, String salt) throws GeneralSecurityException {
		return encrypt(content, createKey(password, salt));
	}

	private static boolean lastPwdModified = false;

	/**
	 * po volání getPwdFromProperties indikuje zda došlo ke změně properties
	 */
	public static boolean passwordRewritten() {
		return lastPwdModified;
	}

	/**
	 * vezme heslo, pokud není šifrované zašifruje a nastaví lastPwdModified na true
	 * 
	 * @param props
	 * @param propName
	 * @return
	 * @throws GeneralSecurityException
	 */
	public static String getPwdFromProperties(Properties props, String propName) throws GeneralSecurityException {
		return getPwdFromProperties(props, propName, null, null);
	}

	/**
	 * vezme heslo, pokud není šifrované zašifruje a nastaví lastPwdModified na true
	 * 
	 * @param props
	 * @param propName
	 * @param defaultPwd - hodnota, pokud v properties dané heslo není
	 * @return
	 * @throws GeneralSecurityException
	 */
	public static String getPwdFromProperties(Properties props, String propName, String defaultPwd) throws GeneralSecurityException {
		return getPwdFromProperties(props, propName, null, defaultPwd);
	}

	/**
	 * vezme heslo, pokud není šifrované zašifruje a nastaví lastPwdModified na true
	 * 
	 * @param props
	 * @param propName
	 * @param password šifrovací heslo
	 * @param defaultPwd hodnota hesla, pokud v properties není
	 * @return
	 * @throws GeneralSecurityException
	 */
	public static String getPwdFromProperties(Properties props, String propName, String password, String defaultPwd)
			throws GeneralSecurityException {

		String pwdPropValue = props.getProperty(propName);
		if (pwdPropValue == null && defaultPwd != null) {
			log.info("Using specified default password for " + propName);
			return defaultPwd;
		}

		String newPwdPropValue = props.getProperty(propName + "_new");

		if (newPwdPropValue != null)
			pwdPropValue = newPwdPropValue;

		lastPwdModified = false;
		if (pwdPropValue == null) {
			return null;
		}

		if (!pwdPropValue.startsWith("salt:")) { //$NON-NLS-1$
			try {
				// nezašifrované heslo, zašifrujeme a nahradíme, uložení je na volajícím
				String salt = "salt:" + StringUtils.randomString(); //$NON-NLS-1$
				if (password == null)
					props.setProperty(propName + "_enc", encrypt(pwdPropValue, salt)); //$NON-NLS-1$
				else
					props.setProperty(propName + "_enc", encrypt(pwdPropValue, password, salt)); //$NON-NLS-1$
				props.setProperty(propName, salt);

				if (newPwdPropValue != null)
					props.remove(propName + "_new");

				lastPwdModified = true;
			} catch (Exception e) {
				log.log(Level.SEVERE, "Failed to encrypt saved password.", e); //$NON-NLS-1$
			}
			return pwdPropValue;
		} else {
			// zašifrované heslo
			String data = props.getProperty(propName + "_enc"); //$NON-NLS-1$
			try {
				if (password == null)
					return decrypt(data, pwdPropValue);
				else
					return decrypt(data, password, pwdPropValue);
			} catch (Exception e) {
				throw new GeneralSecurityException("Failed to decrypt password property.", e); //$NON-NLS-1$
			}
		}
	}

	public static long getCRC32Checksum(String value) {
		return getCRC32Checksum(value.getBytes());
	}

	public static String getNumericChecksum(String value, int length) {
		String crc32 = String.valueOf(getCRC32Checksum(value.getBytes()));
		if (crc32.length() < length) {
			while (crc32.length() < length) {
				crc32 = "9" + crc32; //$NON-NLS-1$
			}
			return crc32;
		} else {
			return "9" + crc32.substring(crc32.length() - length + 1); //$NON-NLS-1$
		}
	}

	public static long getCRC32Checksum(byte[] bytes) {
		Checksum crc32 = new CRC32();
		crc32.update(bytes, 0, bytes.length);
		return crc32.getValue();
	}
}

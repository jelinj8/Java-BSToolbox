package cz.bliksoft.javautils.barcodes;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.text.AbstractDocument.BranchElement;

import cz.bliksoft.javautils.logging.LogUtils;

/**
 * https://grandzebu.net/informatique/codbar-en/code128.htm Jean-Luc BLOECHLE:
 * https://grandzebu.net/informatique/codbar/code128_java2.asc
 * 
 * @author jakub
 *
 */
public class Code128 {

	static Logger log = Logger.getLogger(Code128.class.getName());

	private static final int CODE_OFFSET = -5;
	private static final int CODE_A = 206 + CODE_OFFSET;
	private static final int CODE_B = 205 + CODE_OFFSET;
	private static final int CODE_C = 204 + CODE_OFFSET;
	private static final int START_A = 208 + CODE_OFFSET;
	private static final int START_B = 209 + CODE_OFFSET;
	private static final int START_C = 210 + CODE_OFFSET;
	private static final int STOP = 211 + CODE_OFFSET;
	
	private static Font code128Font = null;

	{
		getFont();
	}

	public static Font getFont() {
		if (code128Font != null)
			return code128Font;

		try {
			code128Font = Font.createFont(Font.TRUETYPE_FONT, Code128.class.getResourceAsStream("code128_fixed.ttf"));
					//.deriveFont(Font.TRUETYPE_FONT, 50);
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			ge.registerFont(code128Font);
			log.info("Loaded and registered font: " + code128Font.getFontName());
		} catch (FontFormatException | IOException e) {
			log.severe("Failed to load code128 font.");
			e.printStackTrace();
			code128Font = new JLabel().getFont();
		}

		return code128Font;
	}

	private static int testNumeric(char[] text, int i, int mini) {
		mini--;
		if (i + mini < text.length)
			for (; mini >= 0; mini--)
				if ((text[i + mini] < 48) || (text[i + mini] > 57))
					break;
		return mini;
	}

	/**
	 * Barcode 128 encoder...</br>
	 * Example: System.out.println(Code128.codeIt("Hello World"));
	 *
	 * @param textToCode the text you want to code
	 * @return the encoded text
	 */
	public static String codeIt(String textToCode) {
		char[] text = textToCode.toCharArray();
		int checksum = 0; // caractère de vérification du texte codé
		int mini; // nb de caractères numériques suivants
		int char2; // traitement de 2 caractères à la fois
		boolean tableB = true; // booléen pour vérifier si on doit utiliser la table B du code 128

		String code128 = "";

		for (char c : text)
			if ((c < 32) || (c > 126))
				return null;

		for (int i = 0; i < text.length;) {
			if (tableB) {
				// intéressant de passer en table C pour 4 chiffres au début ou a la fin ou pour
				// 6 chiffres
				mini = ((i == 0) || (i + 3 == text.length - 1) ? 4 : 6);

				// si les mini caractères à partir de index sont numériques, alors mini = 0
				mini = testNumeric(text, i, mini);

				// si mini < 0 on passe en table C
				if (mini < 0) {
					code128 += (char) (i == 0 ? START_C : CODE_C); // débuter sur la table C ou commuter sur la table C
					tableB = false;
				} else if (i == 0)
					code128 += (char) START_B; // débuter sur la table B
			}

			if (!tableB) {
				// on est sur la table C, on va essayer de traiter 2 chiffres
				mini = testNumeric(text, i, 2);

				if (mini < 0) {
					// ok pour 2 chiffres, les traiter
					char2 = Integer.parseInt("" + text[i] + text[i + 1]);
					char2 += (char2 < 95 ? 32 : 100);
					code128 += (char) char2;
					i += 2;
				} else {
					// on n'a pas deux chiffres, retourner en table B
					code128 += (char) CODE_B;
					tableB = true;
				}
			}

			if (tableB)
				code128 += text[i++];
		}

		// calcul de la clef de controle
		for (int i = 0; i < code128.length(); i++) {
			char2 = code128.charAt(i);
			char2 -= (char2 < 127 ? 32 : 100);
			checksum = ((i == 0 ? char2 : checksum) + i * char2) % 103;
		}

		// calcul du code ascii de la clef de controle
		checksum += (checksum < 95 ? 32 : 100);

		// ajout de la clef et du stop à la fin du texte codé.
		return code128 += ("" + (char) checksum + (char) STOP);
	}

	public static BufferedImage getImage(String value, int size) {
		String code = codeIt(value);
		BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);

		getFont();//.deriveFont(Font.TRUETYPE_FONT, size);
		Font fnt = new Font("Code 128", Font.TRUETYPE_FONT, size);


		Graphics2D g2d = img.createGraphics();
		g2d.setFont(fnt);
		FontMetrics fm = g2d.getFontMetrics();
		int w = fm.stringWidth(code);
		int h = fm.getHeight();

		img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		g2d = img.createGraphics();
		g2d.setFont(fnt);

		int x = 0;
		int y = h;
		g2d.setColor(Color.BLACK);
		g2d.drawString(code, x, y);
		g2d.dispose();
		return img;
	}

//    public static String StringToBarcode(String value)
//    {
//        // Parameters : a string
//        // Return     : a string which give the bar code when it is dispayed with CODE128.TTF font
//        //              : an empty string if the supplied parameter is no good
//        int charPos, minCharPos;
//        int currentChar, checksum;
//        Boolean isTableB = true, isValid = true;
//        String returnValue = "";
//
//        if (value.length() > 0)
//        {
//            // Check for valid characters
//            for (int charCount = 0; charCount < value.length(); charCount++)
//            {
//                //currentChar = char.GetNumericValue(value, charPos);
//                //            (int)char.Parse(value.Substring(charCount, 1));
//                System.out.println("count "+charCount);
//                currentChar = value.charAt(charCount);
//                if (!(currentChar >= 32 && currentChar <= 126))
//                {
//                    isValid = false;
//                    break;
//                }
//            }
//
//            // Barcode is full of ascii characters, we can now process it
//            if (isValid)
//            {
//                charPos = 0;
//                while (charPos < value.length())
//                {
//                    if (isTableB)
//                    {
//                        // See if interesting to switch to table C
//                        // yes for 4 digits at start or end, else if 6 digits
//                        if (charPos == 0 || charPos + 4 == value.length())
//                            minCharPos = 4;
//                        else
//                            minCharPos = 6;
//
//                        minCharPos = IsNumber(value, charPos, minCharPos);
//
//                        if (minCharPos < 0)
//                        {
//                            // Choice table C
//                            if (charPos == 0)
//                            {
//                                // Starting with table C
//                                returnValue =   String.valueOf((char) 205); // char.ConvertFromUtf32(205);
//                            }
//                            else
//                            {
//                                // Switch to table C
//                                returnValue = returnValue + String.valueOf((char) 199);
//                            }
//                            isTableB = false;
//                        }
//                        else
//                        {
//                            if (charPos == 0)
//                            {
//                                // Starting with table B
//                                returnValue = String.valueOf((char) 204); // char.ConvertFromUtf32(204);
//                            }
//
//                        }
//                    }
//
//                    if (!isTableB)
//                    {
//                        // We are on table C, try to process 2 digits
//                        minCharPos = 2;
//                        minCharPos = IsNumber(value, charPos, minCharPos);
//                        if (minCharPos < 0) // OK for 2 digits, process it
//                        {
//
//                            currentChar = Integer.parseInt(value.substring(charPos, charPos+2));
//                            currentChar = currentChar < 95 ? currentChar + 32 : currentChar + 100;
//                            returnValue = returnValue + String.valueOf((char) currentChar);
//                            charPos += 2;
//                        }
//                        else
//                        {
//                            // We haven't 2 digits, switch to table B
//                            returnValue = returnValue + String.valueOf((char) 200);
//                            isTableB = true;
//                        }
//                    }
//                    if (isTableB)
//                    {
//                        // Process 1 digit with table B
//                        returnValue = returnValue + value.substring(charPos, charPos+1);
//                        charPos++;
//                    }
//                }
//
//                // Calculation of the checksum
//                checksum = 0;
//                for (int loop = 0; loop < returnValue.length(); loop++)
//                {
//                    currentChar = returnValue.charAt(loop);
//                    currentChar = currentChar < 127 ? currentChar - 32 : currentChar - 100;
//                    if (loop == 0)
//                        checksum = currentChar;
//                    else
//                        checksum = (checksum + (loop * currentChar)) % 103;
//                }
//
//                // Calculation of the checksum ASCII code
//                checksum = checksum < 95 ? checksum + 32 : checksum + 100;
//                // Add the checksum and the STOP
//                returnValue = returnValue +
//                        String.valueOf((char) checksum) +
//                        String.valueOf((char) 206);
//            }
//        }
//
//        return returnValue;
//    }
//
//    private static int IsNumber(String InputValue, int CharPos, int MinCharPos)
//    {
//        // if the MinCharPos characters from CharPos are numeric, then MinCharPos = -1
//        MinCharPos--;
//        if (CharPos + MinCharPos < InputValue.length())
//        {
//            while (MinCharPos >= 0)
//            {
//                if ((int)(InputValue.charAt(CharPos + MinCharPos)) < 48
//                    ||(int)(InputValue.charAt(CharPos + MinCharPos)) > 57)
//                {
//                    break;
//                }
//                MinCharPos--;
//            }
//        }
//        return MinCharPos;
//    }
}
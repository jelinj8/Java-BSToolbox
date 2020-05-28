package cz.bliksoft.javautils;

public class Code128//  implements XDOBarcodeEncoder
// The class implements the XDOBarcodeEncoder interface
{
//// This is the barcode vendor id that is used in the register vendor field and format-barcode fields
//  public static final String BARCODE_VENDOR_ID = "XMLPBarVendor";
//// The hastable is used to store references to the encoding methods
//  public static final Hashtable ENCODERS = new Hashtable(10);
//// The BarcodeUtil class needs to be instantiated
//  public static final Code128 mUtility = new Code128();
//// This is the main code that is executed in the class, it is loading the methods for the encoding into the hashtable. In this case we are loading the three code128 encoding methods we have created.



// The encode method is called to then call the appropriate encoding method, in this example the code128a/b/c methods.



//   public final String encode(String s, String s1)
//    {
//        if(s != null && s1 != null)
//        {
//            try
//            {
//                Method method = (Method)ENCODERS.get(s1.trim().toLowerCase());
//                if(method != null)
//                    return (String)method.invoke(this, new Object[] {
//                        s
//                    });
//                else
//                    return s;
//            }
//            catch(Exception exception)
//            {
//            	exception.printStackTrace();
//            }
//            return s;
//        } else
//        {
//            return s;
//        }
//    }



  /** This is the complete method for Code128a */



  public static final String code128a( String DataToEncode )
  {
    char C128_Start = (char)203;
    char C128_Stop = (char)206;
    String Printable_string = "";
    char CurrentChar;
    int CurrentValue=0;
    int weightedTotal=0;
    int CheckDigitValue=0;
    char C128_CheckDigit='w';
 
    DataToEncode = DataToEncode.trim();
    weightedTotal = ((int)C128_Start) - 100;
    for( int i = 1; i <= DataToEncode.length(); i++ )
      {
    //get the value of each character
    CurrentChar = DataToEncode.charAt(i-1);
    if( ((int)CurrentChar) < 135 )
      CurrentValue = ((int)CurrentChar) - 32;
    if( ((int)CurrentChar) > 134 )
      CurrentValue = ((int)CurrentChar) - 100;
 
    CurrentValue = CurrentValue * i;
    weightedTotal = weightedTotal + CurrentValue;
      }
    //divide the WeightedTotal by 103 and get the remainder,
    //this is the CheckDigitValue
    CheckDigitValue = weightedTotal % 103;
    if( (CheckDigitValue < 95) && (CheckDigitValue > 0) )
      C128_CheckDigit = (char)(CheckDigitValue + 32);
    if( CheckDigitValue > 94 )
      C128_CheckDigit = (char)(CheckDigitValue + 100);
    if( CheckDigitValue == 0 ){
      C128_CheckDigit = (char)194;
    }
 
    Printable_string = C128_Start + DataToEncode + C128_CheckDigit + C128_Stop + " ";
    return Printable_string;
  }




  /** This is the complete method for Code128b ***/



  public static final String code128b( String DataToEncode )
  {
    char C128_Start = (char)204;
    char C128_Stop = (char)206;
    String Printable_string = "";
    char CurrentChar;
    int CurrentValue=0;
    int weightedTotal=0;
    int CheckDigitValue=0;
    char C128_CheckDigit='w';

    DataToEncode = DataToEncode.trim();
    weightedTotal = ((int)C128_Start) - 100;
    for( int i = 1; i <= DataToEncode.length(); i++ )
      {
    //get the value of each character
    CurrentChar = DataToEncode.charAt(i-1);
    if( ((int)CurrentChar) < 135 )
      CurrentValue = ((int)CurrentChar) - 32;
    if( ((int)CurrentChar) > 134 )
      CurrentValue = ((int)CurrentChar) - 100;
 
    CurrentValue = CurrentValue * i;
    weightedTotal = weightedTotal + CurrentValue;
      }
    //divide the WeightedTotal by 103 and get the remainder,
    //this is the CheckDigitValue
    CheckDigitValue = weightedTotal % 103;
    if( (CheckDigitValue < 95) && (CheckDigitValue > 0) )
      C128_CheckDigit = (char)(CheckDigitValue + 32);
    if( CheckDigitValue > 94 )
      C128_CheckDigit = (char)(CheckDigitValue + 100);
    if( CheckDigitValue == 0 ){
      C128_CheckDigit = (char)194;
    }
 
    Printable_string = C128_Start + DataToEncode + C128_CheckDigit + C128_Stop + " ";
    return Printable_string;
  }

  /** This is the complete method for Code128c **/
  // chybí přecházení mezi znakovými sadami
//  public static final String code128c( String s )
//  {
//    char C128_Start = (char)205;
//    char C128_Stop = (char)206;
//    String Printable_string = "";
//    String DataToPrint = "";
//    String OnlyCorrectData = "";
//    int i=1;
//    int CurrentChar=0;
//    int CurrentValue=0;
//    int weightedTotal=0;
//    int CheckDigitValue=0;
//    char C128_CheckDigit='w';
//    DataToPrint = "";
//    s = s.trim();
//    for(i = 1; i <= s.length(); i++ )
//      {
//    //Add only numbers to OnlyCorrectData string
//    CurrentChar = (int)s.charAt(i-1);
//    if((CurrentChar < 58) && (CurrentChar > 47))
//      {
//        OnlyCorrectData = OnlyCorrectData + (char)s.charAt(i-1);
//      }
//      }
//    s = OnlyCorrectData;
//    //Check for an even number of digits, add 0 if not even
//    if( (s.length() % 2) == 1 )
//      {
//    s = "0" + s;
//      }
//    //<<<< Calculate Modulo 103 Check Digit and generate DataToPrint >>>>
//    //Set WeightedTotal to the Code 128 value of the start character
//    weightedTotal = ((int)C128_Start) - 100;
//    int WeightValue = 1;
//    for( i = 1; i <= s.length(); i += 2 )
//      {
//    //Get the value of each number pair (ex: 5 and 6 = 5*10+6 =56)
//    //And assign the ASCII values to DataToPrint
//    CurrentChar = ((((int)s.charAt(i-1))-48)*10) + (((int)s.charAt(i))-48);
//    if((CurrentChar < 95) && (CurrentChar  > 0))
//      DataToPrint = DataToPrint + (char)(CurrentChar + 32);
//    if( CurrentChar > 94 )
//      DataToPrint = DataToPrint + (char)(CurrentChar + 100);
//    if( CurrentChar == 0)
//      DataToPrint = DataToPrint + (char)194;
//    //multiply by the weighting character
//    //add the values together to get the weighted total
//    weightedTotal = weightedTotal + (CurrentChar * WeightValue);
//    WeightValue = WeightValue + 1;
//      }
//    //divide the WeightedTotal by 103 and get the remainder,
//    //this is the CheckDigitValue
//    CheckDigitValue = weightedTotal % 103;
//    if((CheckDigitValue < 95) && (CheckDigitValue > 0))
//      C128_CheckDigit = (char)(CheckDigitValue + 32);
//    if( CheckDigitValue > 94 )
//      C128_CheckDigit = (char)(CheckDigitValue + 100);
//    if( CheckDigitValue == 0 ){
//      C128_CheckDigit = (char)194;
//    }
//    Printable_string = C128_Start + DataToPrint + C128_CheckDigit + C128_Stop + " ";
//    return Printable_string;
//  }
}
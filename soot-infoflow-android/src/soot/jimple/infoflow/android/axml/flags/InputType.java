package soot.jimple.infoflow.android.axml.flags;

import java.util.Collection;

/**
 * Support for EditText's input type
 */
public class InputType {
	private static final BitwiseFlagSystem<Integer> FLAG_SYSTEM = new BitwiseFlagSystem<>();
	public static int textFilter = 177;
	public static int textPostalAddress = 113;
	public static int textWebEmailAddress = 209;
	public static int textWebPassword = 225;
	public static int textEmailSubject = 49;
	public static int textLongMessage = 81;
	public static int textPersonName = 97;
	public static int textPhonetic = 193;
	public static int textVisiblePassword = 145;
	public static int textWebEditText = 161;
	public static int date = 20;
	public static int numberDecimal = 8194;
	public static int numberPassword = 225;
	public static int numberSigned = 4098;
	public static int phone = 3;
	public static int textAutoComplete = 65537;
	public static int textAutoCorrect = 32769;
	public static int textCapCharacters = 4097;
	public static int textCapWords = 8193;
	public static int textEmailAddress = 33;
	public static int textCapSentences = 16385;
	public static int textImeMultiLine = 262145;
	public static int textMultiLine = 131073;
	public static int textNoSuggestions = 524289;
	public static int textPassword = 129;
	public static int textShortMessage = 65;
	public static int textUri = 27;
	public static int time = 36;
	public static int datetime = 4;
	public static int number = 2;
	public static int text = 1;

	static {
		//The order can be determined via the Android Framework and is highly relevant
		//See ResFlagsAttr.convertToResXmlFormat method to get the order
		FLAG_SYSTEM.register(textFilter, 177);
		FLAG_SYSTEM.register(textPostalAddress, 113);
		FLAG_SYSTEM.register(textWebEmailAddress, 209);
		FLAG_SYSTEM.register(textWebPassword, 225);
		FLAG_SYSTEM.register(textEmailSubject, 49);
		FLAG_SYSTEM.register(textLongMessage, 81);
		FLAG_SYSTEM.register(textPersonName, 97);
		FLAG_SYSTEM.register(textPhonetic, 193);
		FLAG_SYSTEM.register(textVisiblePassword, 145);
		FLAG_SYSTEM.register(textWebEditText, 161);
		FLAG_SYSTEM.register(date, 20);
		FLAG_SYSTEM.register(numberDecimal, 8194);
		FLAG_SYSTEM.register(numberPassword, 18);
		FLAG_SYSTEM.register(numberSigned, 4098);
		FLAG_SYSTEM.register(phone, 3);
		FLAG_SYSTEM.register(textAutoComplete, 65537);
		FLAG_SYSTEM.register(textAutoCorrect, 32769);
		FLAG_SYSTEM.register(textCapCharacters, 4097);
		FLAG_SYSTEM.register(textCapSentences, 16385);
		FLAG_SYSTEM.register(textCapWords, 8193);
		FLAG_SYSTEM.register(textEmailAddress, 33);
		FLAG_SYSTEM.register(textImeMultiLine, 262145);
		FLAG_SYSTEM.register(textMultiLine, 131073);
		FLAG_SYSTEM.register(textNoSuggestions, 524289);
		FLAG_SYSTEM.register(textPassword, 129);
		FLAG_SYSTEM.register(textShortMessage, 65);
		FLAG_SYSTEM.register(textUri, 17);
		FLAG_SYSTEM.register(time, 36);
		FLAG_SYSTEM.register(datetime, 4);
		FLAG_SYSTEM.register(number, 2);
		FLAG_SYSTEM.register(text, 1);
	}

	/**
	 * Checks whether all the given flags are set
	 * @param inputValue input value
	 * @param flag the flags to check
	 */
	public static boolean isSet(int inputValue, Integer... flag) {
		return FLAG_SYSTEM.isSet(inputValue, flag);
	}

	/**
	 * Returns all matching flags
	 * @param inputValue input value
	 */
	public static Collection<Integer> getFlags(int inputValue) {
		return FLAG_SYSTEM.getFlags(inputValue);
	}
}

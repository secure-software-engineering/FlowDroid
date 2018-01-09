package soot.jimple.infoflow.android.iccta;

import java.nio.ByteBuffer;

import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

public class JimpleIndexNumberTag implements Tag
{
	public static final String JIMPLE_INDEX_NUMBER_TAG = "JimpleIndexNumberTag";
	private final int indexNumber;
	
	public JimpleIndexNumberTag(int indexNumber)
	{
		this.indexNumber = indexNumber;
	}
	
	public int getIndexNumber() {
		return indexNumber;
	}

	@Override
	public String getName() 
	{
		return JIMPLE_INDEX_NUMBER_TAG;
	}

	@Override
	public byte[] getValue() throws AttributeValueException 
	{
		return ByteBuffer.allocate(4).putInt(indexNumber).array();
	}

	@Override
	public String toString() 
	{
		return "" + indexNumber;
	}
}

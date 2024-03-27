package soot.jimple.infoflow.cfg;

import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

/**
 * Marks a class that is identified as a user class by FlowDroid or the user
 *
 * @author Tim Lange
 */
public class FlowDroidUserClass implements Tag {
    public static final String TAG_NAME = "fd_userclass";

    private static final FlowDroidUserClass INSTANCE = new FlowDroidUserClass();

    public static FlowDroidUserClass v() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return TAG_NAME;
    }

    @Override
    public byte[] getValue() throws AttributeValueException {
        return null;
    }
}

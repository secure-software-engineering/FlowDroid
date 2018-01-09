package soot.jimple.infoflow.test.methodSummary;

public class GapClass implements IGapClass {

	private String stringField;
	private Data dataField;
	
	@Override
	public String callTheGap(String in) {
		return in;
	}

	@Override
	public void fillDataString(String in, Data d) {
		d.stringField = in;
	}

	@Override
	public Data dataThroughGap(Data d) {
		return d;
	}

	@Override
	public void storeString(String in) {
		this.stringField = in;
	}

	@Override
	public String retrieveString() {
		return this.stringField;
	}

	@Override
	public void storeData(Data in) {
		this.dataField = in;
	}

	@Override
	public Data retrieveData() {
		return this.dataField;
	}

}

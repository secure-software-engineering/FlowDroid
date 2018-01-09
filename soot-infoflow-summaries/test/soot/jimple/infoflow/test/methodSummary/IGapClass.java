package soot.jimple.infoflow.test.methodSummary;

public interface IGapClass {

	public String callTheGap(String in);
	public void fillDataString(String in, Data d);
	public Data dataThroughGap(Data d);
	
	public void storeString(String in);
	public String retrieveString();

	public void storeData(Data in);
	public Data retrieveData();

}

package report_downloader;

public class AttributeElement {

	private String name;
	private String value;
	
	public AttributeElement(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	public String getName() {
		return name;
	}
	public String getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return name + "=" + value;
	}
}

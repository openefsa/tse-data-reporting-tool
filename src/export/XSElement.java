package export;

public class XSElement {

	private String name;
	private String ref;
	
	public void setName(String name) {
		this.name = name;
	}
	public void setRef(String ref) {
		this.ref = ref;
	}
	public String getName() {
		return name;
	}
	public String getRef() {
		return ref;
	}
	
	@Override
	public String toString() {
		return "xs:element name=" + name + ";ref=" + ref;
	}
}

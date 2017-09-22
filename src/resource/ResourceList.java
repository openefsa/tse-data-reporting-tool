package resource;

import java.util.ArrayList;

import webservice.GetResourceList;

/**
 * Result of {@link GetResourceList} call.
 * @author avonva
 *
 */
public class ResourceList extends ArrayList<ResourceReference> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Get the list of resources that match the {@code resourceType}
	 * @param resourceType
	 * @return
	 */
	public ResourceList getByType(String resourceType) {
		
		ResourceList resources = new ResourceList();
		
		for (ResourceReference ref : this) {
			if (ref.getType().equals(resourceType))
				resources.add(ref);
		}

		return resources;
	}
}

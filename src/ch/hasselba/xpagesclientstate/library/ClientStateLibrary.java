package ch.hasselba.xpagesclientstate.library;

import ch.hasselba.xpagesclientstate.Activator;

import com.ibm.xsp.library.AbstractXspLibrary;
/**
 * XspLibrary
 * 
 * @author Sven Hasselbach
 */
public class ClientStateLibrary extends AbstractXspLibrary{

	private final static String LIBRARY_ID = ClientStateLibrary.class.getName();

	static {
		if (Activator._debug) {
			System.out.println(ClientStateLibrary.class.getName() + " loaded");
		}
	}

	public ClientStateLibrary() {
		if (Activator._debug) {
			System.out.println(ClientStateLibrary.class.getName() + " created");
		}
	}

	public String getLibraryId() {
		return LIBRARY_ID;
	}

	@Override
	public String getPluginId() {
		return Activator.PLUGIN_ID;
	}
	
	@Override
	public String[] getDependencies() {
		return new String[] { "com.ibm.xsp.core.library",
				"com.ibm.xsp.extsn.library", "com.ibm.xsp.domino.library",
				"com.ibm.xsp.designer.library" };
	}


	@Override
	public String[] getFacesConfigFiles() {
		String[] files = new String[] { "META-INF/xpagesclientstate-faces-config.xml" };
		return files;
	}

	@Override
	public boolean isGlobalScope() {
		return false;
	}
}

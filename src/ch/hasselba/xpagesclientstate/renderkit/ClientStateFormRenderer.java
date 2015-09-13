package ch.hasselba.xpagesclientstate.renderkit;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.ibm.xsp.component.UIViewRootEx2;
import com.ibm.xsp.renderkit.html_basic.FormRenderer;
/**
 * ClientStateFormRenderer
 * adds ClientState to every form
 * 
 * @author Sven Hasselbach
 */
public class ClientStateFormRenderer extends FormRenderer {
	private final String FIELD_NAME_CLIENTSTATE = "$$clientstate";
	private final String VIEW_CLIENTSTATE = "XSP_CLIENTSTATE";

	@SuppressWarnings("rawtypes")
	public void encodeEnd(FacesContext fc, UIComponent uiComponent)
			throws IOException {
		
		ResponseWriter rw = fc.getResponseWriter();

		rw.startElement("input", uiComponent);
		rw.writeAttribute("type", "hidden", null);
		rw.writeAttribute("name", FIELD_NAME_CLIENTSTATE, null);
		rw.writeAttribute("id", FIELD_NAME_CLIENTSTATE, null);
				
		Map viewMap = ((UIViewRootEx2) fc.getViewRoot()).getViewMap();
		String clientState = (String) viewMap.get(VIEW_CLIENTSTATE);
		if (clientState != null)
			rw.writeAttribute("value", clientState, null);
		rw.endElement("input");

		super.encodeEnd(fc, uiComponent);
	}
}

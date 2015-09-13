package ch.hasselba.xpagesclientstate.application;


import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.faces.FacesException;
import javax.faces.application.FacesMessage;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

import ch.hasselba.xpagesclientstate.Activator;

import com.ibm.xsp.application.NavigationRule;
import com.ibm.xsp.application.ViewHandlerExImpl;

/**
 * ClientStateViewHandlerExImpl
 * modified ViewHandler
 * 
 * @author Sven Hasselbach
 */
public class ClientStateViewHandlerExImpl extends ViewHandlerExImpl {

	public ClientStateViewHandlerExImpl(ViewHandler viewHandler) {
		super(viewHandler);
		if( Activator._debug )
			System.out.println("ClientStateViewHandlerExImpl - constructor");
		
	}

	@Override
	public void renderView(FacesContext fc, UIViewRoot uiViewRoot)
			throws IOException, FacesException {
		if( Activator._debug )
			System.out.println("ClientStateViewHandlerExImpl::renderView start.");
		if (fc.getResponseComplete()) {
			return;
		}

		if (hasErrorMessage(fc)) {
			NavigationRule navRule = getFailureNavigationRule(uiViewRoot);
			if (navRule != null) {
				UIViewRoot errorUIViewRoot = createView(fc, navRule.getViewId());
				if (errorUIViewRoot != null) {
					fc.setViewRoot(errorUIViewRoot);
					uiViewRoot = errorUIViewRoot;
				}
			}
		}

		doInitRender(fc);
		saveViewState(fc, uiViewRoot);
		doRender(fc, uiViewRoot);
		if( Activator._debug )
			System.out.println("ClientStateViewHandlerExImpl::renderView end.");
	}

	@SuppressWarnings("unchecked")
	private boolean hasErrorMessage(FacesContext fc) {
		for (Iterator<FacesMessage> it = fc.getMessages(); it.hasNext();) {
			FacesMessage msg = it.next();
			FacesMessage.Severity severity = msg.getSeverity();
			if ((severity == FacesMessage.SEVERITY_ERROR)
					|| (severity == FacesMessage.SEVERITY_FATAL)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("rawtypes")
	private NavigationRule getFailureNavigationRule(UIViewRoot uiViewRoot) {
		List navRules = (List) uiViewRoot.getAttributes()
				.get("navigationRules");
		if (navRules != null) {
			for (Iterator it = navRules.iterator(); it.hasNext();) {
				NavigationRule navRule = (NavigationRule) it.next();
				if ("xsp-failure".equals(navRule.getOutcome())) {
					return navRule;
				}
			}
		}
		return null;
	}
}

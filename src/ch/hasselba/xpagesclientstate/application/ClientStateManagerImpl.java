package ch.hasselba.xpagesclientstate.application;


import java.io.*;
import java.util.*;
import java.net.URI;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.faces.FacesException;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.application.StateManager;

import ch.hasselba.xpagesclientstate.Activator;
import ch.hasselba.xpagesclientstate.utils.AES128Encryptor;

import com.ibm.commons.util.StringUtil;
import com.ibm.xsp.application.ComponentNode;
import com.ibm.xsp.application.IComponentNode;
import com.ibm.xsp.application.StateManagerImpl;
import com.ibm.xsp.application.UniqueViewIdManager;
import com.ibm.xsp.component.UIViewRootEx;
import com.ibm.xsp.context.FacesContextExImpl;
import com.ibm.xsp.designer.context.XSPContext;

import com.ibm.xsp.renderkit.html_basic.FormRenderer;
import com.ibm.xsp.util.Delegation;
import com.ibm.xsp.util.FacesUtil;
import com.ibm.xsp.util.ClassLoaderUtil;
import com.ibm.xsp.util.TypedUtil;
import org.apache.commons.codec.binary.Base64;

import lotus.domino.*;

/**
 * ClientStateManagerImpl
 * the state manager implemenation for the client state
 * 
 * includes encryption and randomization
 * 
 * @author Sven Hasselbach
 */
@SuppressWarnings("unused")
public class ClientStateManagerImpl extends StateManagerImpl {
	private final StateManager delegated;
	private final String FIELD_NAME_CLIENTSTATE = "$$clientstate";
	private final String VIEW_CLIENTSTATE = "XSP_CLIENTSTATE";
	private final String AES_SECRET_KEY = "ThisIsASecretKey"; // 128 bit key
	private final String AES_IV_VECTOR = "ThisIsASecretIV!"; // 128 bit IV
	private final static String UTF8 = "UTF-8";
	public ClientStateManagerImpl(final StateManager delegate) {
		super(delegate);
		this.delegated = delegate;
		if( Activator._debug )
			System.out.println("ClientStateManagerImpl constructor called.");
	}
	
	public ClientStateManagerImpl() throws FacesException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		StateManager priorStateManager = (StateManager) Delegation.getImplementation("state-manager");
		this.delegated = priorStateManager;
		if( Activator._debug )
			System.out.println("ClientStateManagerImpl constructor called.");
	}

	@SuppressWarnings("unchecked")
	@Override
	public SerializedView saveSerializedView(final FacesContext fc) {
		try {
			if( Activator._debug )
				System.out.println("ClientStateManagerImpl::saveSerializedView start.");
			
			UIViewRootEx view = (UIViewRootEx)fc.getViewRoot();

			view._xspCleanTransientData();
		
			Object treeStructure = getTreeStructureToSave(fc, view);
			Object componentStructure = getComponentStateToSave(fc, view);
			SerializedView serializedView = new SerializedView(treeStructure, componentStructure);

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			Class<? extends ObjectOutputStream> outputClass = (Class<? extends ObjectOutputStream>)Class.forName("com.ibm.xsp.application.AbstractSerializingStateManager$FastObjectOutputStream");
			Constructor<?> outputConstructor = outputClass.getConstructor(OutputStream.class);
			ObjectOutputStream oos = (ObjectOutputStream)outputConstructor.newInstance(bos);

			oos.writeObject(serializedView.getStructure());
			Method writeObjectEx = outputClass.getMethod("writeObjectEx", Object.class);
			writeObjectEx.invoke(oos, serializedView.getState());

			oos.flush();
			bos.flush();

			// add the state data to the response or the viewMap
			// let's add four random bytes at the beginning of the state data to prevent 
			byte[] viewStateBytes = bos.toByteArray();		
			byte[] randomBytes = new byte[4];
			new Random().nextBytes(randomBytes);
			byte[] toEncrypt = new byte[randomBytes.length + viewStateBytes.length];
			System.arraycopy(randomBytes, 0, toEncrypt, 0, randomBytes.length);
			System.arraycopy(viewStateBytes, 0, toEncrypt, randomBytes.length, viewStateBytes.length);

			String data = new String(Base64.encodeBase64(AES128Encryptor.encrypt(AES_SECRET_KEY, AES_IV_VECTOR, toEncrypt)));
		
			if( ((FacesContextExImpl) fc).isAjaxPartialRefresh() ) {
				view.postScript( "dojo.query('#" + FIELD_NAME_CLIENTSTATE + "').forEach( function(dom){ dom.value = '" + data + "' } );");
			}else{
				view.getViewMap().put(VIEW_CLIENTSTATE, data);
			}
		
			return serializedView;
		} catch(Exception e) { throw new RuntimeException(e); } finally{
			if( Activator._debug )
				System.out.println("ClientStateManagerImpl::saveSerializedView end.");
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public UIViewRoot restoreView(final FacesContext fc, final String viewId, final String renderKitId) {
		
		if( Activator._debug )
			System.out.println("ClientStateManagerImpl::restoreView start.");

		try {
			String uniqueViewId = UniqueViewIdManager.getRequestUniqueViewId(fc);
			Map localMap = fc.getExternalContext().getRequestParameterMap();
			String data = (String)localMap.get( FIELD_NAME_CLIENTSTATE );		
			byte[] bytes = Base64.decodeBase64(data.getBytes(UTF8));
			
			if(bytes != null) {
				byte[] decrypted = AES128Encryptor.decrypt(AES_SECRET_KEY, AES_IV_VECTOR, bytes);
				byte[] slice = Arrays.copyOfRange(decrypted, 4, decrypted.length);
				
				//bytes = unzip( bytes );
				ByteArrayInputStream bis = new ByteArrayInputStream(slice);
				Class<? extends ObjectInputStream> inputClass = (Class<? extends ObjectInputStream>)Class.forName("com.ibm.xsp.application.AbstractSerializingStateManager$FastObjectInputStream");
				Constructor<?> inputConstructor = inputClass.getConstructor(FacesContext.class, InputStream.class);
				ObjectInputStream ois = (ObjectInputStream)inputConstructor.newInstance(fc, bis);

				Object treeStructure = ois.readObject();
				Method readObjectEx = inputClass.getMethod("readObjectEx");
				Object componentStructure = readObjectEx.invoke(ois);

				SerializedView serializedView = new SerializedView(treeStructure, componentStructure);

				// Reconstruct the view
				IComponentNode localIComponentNode = (IComponentNode)serializedView.getStructure();
				UIViewRootEx view = (UIViewRootEx)localIComponentNode.restore(fc);
				Object viewState = serializedView.getState();
				FacesUtil.setRestoreRoot(fc, view);

				view.processRestoreState(fc, viewState);
				FacesUtil.setRestoreRoot(fc, null);
				view.setUniqueViewId(uniqueViewId);

				return view;
			} else {
				return null;
			}
		} catch(Exception e) { throw new RuntimeException(e); }finally{
			if( Activator._debug )
				System.out.println("ClientStateManagerImpl::restoreView end.");
		}
	}

	protected Object getTreeStructureToSave(final FacesContext fc, final UIViewRoot view) {
		if (view.isTransient()) {
			return null;
		}
		IComponentNode localIComponentNode = createComponentNode(fc, view);
		return localIComponentNode;
	}
	protected IComponentNode createComponentNode(final FacesContext facesContext, final UIViewRoot view)
	{
		Class<ComponentNode> holderClass = ComponentNode.class;
		Constructor<?> cons = holderClass.getDeclaredConstructors()[0];
		cons.setAccessible(true);
		try {
			return (ComponentNode)cons.newInstance(view);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}
	protected Object getComponentStateToSave(final FacesContext fc, final UIViewRoot view) {
		return view.processSaveState(fc);
	}

}
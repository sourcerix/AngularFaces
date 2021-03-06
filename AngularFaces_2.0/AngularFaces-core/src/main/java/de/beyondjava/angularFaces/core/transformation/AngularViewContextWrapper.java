/**
 *  (C) 2013-2015 Stephan Rauh http://www.beyondjava.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.beyondjava.angularFaces.core.transformation;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialResponseWriter;
import javax.faces.context.PartialViewContext;
import javax.faces.context.PartialViewContextWrapper;
import javax.faces.context.ResponseWriter;
import javax.faces.event.PhaseId;
import javax.faces.lifecycle.ClientWindow;
import javax.faces.render.ResponseStateManager;

import de.beyondjava.angularFaces.components.puiModelSync.PuiModelSync;
import de.beyondjava.angularFaces.components.puiSync.PuiSync;

/** This class generate the optimized AngularFaces AJAX response. */
public class AngularViewContextWrapper extends PartialViewContextWrapper {

	private static final String ORIGINAL_WRITER = "com.sun.faces.ORIGINAL_WRITER";

	private PartialViewContext wrapped;

	public AngularViewContextWrapper(PartialViewContext wrapped) {
		this.wrapped = wrapped;

	}

	/**
	 * Copied from com.sun.faces.util.Util, Mojarra 2.2.7
	 */
	public static String getViewStateId(FacesContext context) {
		String result = null;
		final String viewStateCounterKey = "com.sun.faces.util.ViewStateCounterKey";
		Map<Object, Object> contextAttrs = context.getAttributes();
		Integer counter = (Integer) contextAttrs.get(viewStateCounterKey);
		if (null == counter) {
			counter = Integer.valueOf(0);
		}

		char sep = UINamingContainer.getSeparatorChar(context);
		UIViewRoot root = context.getViewRoot();
		result = root.getContainerClientId(context) + sep + ResponseStateManager.VIEW_STATE_PARAM + sep + +counter;
		contextAttrs.put(viewStateCounterKey, ++counter);

		return result;
	}

	/**
	 * Copied from com.sun.faces.util.Util, Mojarra 2.2.7
	 */
	public static String getClientWindowId(FacesContext context) {
		String result = null;
		final String clientWindowIdCounterKey = "com.sun.faces.util.ClientWindowCounterKey";
		Map<Object, Object> contextAttrs = context.getAttributes();
		Integer counter = (Integer) contextAttrs.get(clientWindowIdCounterKey);
		if (null == counter) {
			counter = Integer.valueOf(0);
		}

		char sep = UINamingContainer.getSeparatorChar(context);
		result = context.getViewRoot().getContainerClientId(context) + sep + ResponseStateManager.CLIENT_WINDOW_PARAM + sep + counter;
		contextAttrs.put(clientWindowIdCounterKey, ++counter);

		return result;
	}

	@Override
	public PartialViewContext getWrapped() {
		return wrapped;
	}

	@Override
	public void processPartial(PhaseId phaseId) {
		if (phaseId == PhaseId.APPLY_REQUEST_VALUES) {
			if (isNGSyncRequest()) {
				applyNGSyncModelValues();
				return;
			}
		}
		if (phaseId == PhaseId.RENDER_RESPONSE) {
			// UIViewRoot viewRoot = ctx.getViewRoot();
			// PuiELTransformer.eliminateDuplicatePuiModelSyncTags(viewRoot);

			if (isAjaxRequest()) {
				if (isAngularFacesRequest()) {
					renderAngularResponse();
					return;
				}
			}
		}
		getWrapped().processPartial(phaseId);
		return;
	}

	private boolean isAngularFacesRequest() {
		FacesContext ctx = FacesContext.getCurrentInstance();
		PartialViewContext pvc = ctx.getPartialViewContext();
		Collection<String> myRenderIds = pvc.getRenderIds();
		boolean isAngularFacesRequest = false;
		if (null != myRenderIds) {
			if (myRenderIds.contains("angular")) {
				isAngularFacesRequest = true;
			} else {
				for (Object id : myRenderIds) {
					if (id instanceof String) {
						if (((String) id).endsWith(":angular")) {
							isAngularFacesRequest = true;
							break;
						}
					}
				}
			}
		}
		return isAngularFacesRequest;
	}

	private void applyNGSyncModelValues() {
		FacesContext ctx = FacesContext.getCurrentInstance();
		PartialViewContext pvc = ctx.getPartialViewContext();
		Collection<String> executeIDs = pvc.getExecuteIds();
		if (null != executeIDs) {
			for (String clientID : executeIDs) {
				UIViewRoot viewRoot = ctx.getViewRoot();  
				UIComponent c = viewRoot.findComponent(clientID);
				if (c==null) {
					c=findByID(viewRoot, clientID);
					if (null==c)
						throw new FacesException("Couldn't find the component with the id " + clientID + ". Possibly this is a configuration error.");
				}
				if (c instanceof PuiSync) {
					if (c.isInView()) {
						// PuiSync syncElement = (PuiSync) c;
						c.decode(ctx);
					}
				}
			}
		}
	}
	
	private UIComponent findByID(UIComponent root, String id) {
		List<UIComponent> children = root.getChildren();
		for (UIComponent child:children) {
			if (id.equals(child.getId())) {
				return child;
			}
			UIComponent grandchild = findByID(child, id);
			if (null != grandchild)
				return grandchild;
		}
		return null;
	}

	private boolean isNGSyncRequest() {
		FacesContext ctx = FacesContext.getCurrentInstance();
		boolean isAngularFacesRequest = false;
		String action = ctx.getExternalContext().getRequestParameterMap().get("de.beyondjava.angularfaces.behavior.event");
		if ("ngsync".equals(action)) {
			isAngularFacesRequest = true;
		}
		return isAngularFacesRequest;
	}

	private void renderAngularResponse() {
		try {
			FacesContext ctx = FacesContext.getCurrentInstance();
			PartialViewContext pvc = ctx.getPartialViewContext();
			UIViewRoot viewRoot = ctx.getViewRoot();
			//
			// We re-enable response writing.
			//
			PartialResponseWriter writer = pvc.getPartialResponseWriter();
			ResponseWriter orig = ctx.getResponseWriter();
			ctx.getAttributes().put(ORIGINAL_WRITER, orig);

			ctx.setResponseWriter(writer);

			ExternalContext exContext = ctx.getExternalContext();
			exContext.setResponseContentType("text/xml");
			exContext.addResponseHeader("Cache-Control", "no-cache");

			String encoding = writer.getCharacterEncoding();
			if (encoding == null) {
				encoding = "UTF-8";
			}
			writer.writePreamble("<?xml version='1.0' encoding='" + encoding + "'?>");
			writer.startDocument();
			writer.startEval();
			encodeAngularScript(writer, ctx);
			writer.endEval();
			renderState(ctx);

			writer.endDocument();
		} catch (IOException ex) {
			this.cleanupAfterView();
		} catch (RuntimeException ex) {
			this.cleanupAfterView();
			// Throw the exception
			throw ex;
		}
	}

	/** Copied from com.sun.faces.context.PartialViewContextImpl. */
	private void cleanupAfterView() {
		FacesContext ctx = FacesContext.getCurrentInstance();
		ResponseWriter orig = (ResponseWriter) ctx.getAttributes().get(ORIGINAL_WRITER);
		assert (null != orig);
		// move aside the PartialResponseWriter
		ctx.setResponseWriter(orig);
	}

	/**
	 * Copied from com.sun.faces.context.PartialViewContextImpl. May have to be adapted to future Mojarra or JSF versions.
	 */
	private void renderState(FacesContext context) throws IOException {
		// Get the view state and write it to the response..
		PartialViewContext pvc = context.getPartialViewContext();
		PartialResponseWriter writer = pvc.getPartialResponseWriter();
		String viewStateId = getViewStateId(context);

		writer.startUpdate(viewStateId);
		String state = context.getApplication().getStateManager().getViewState(context);
		writer.write(state);
		writer.endUpdate();

		ClientWindow window = context.getExternalContext().getClientWindow();
		if (null != window) {
			String clientWindowId = getClientWindowId(context);
			writer.startUpdate(clientWindowId);
			writer.write(window.getId());
			writer.endUpdate();
		}
	}

	public void encodeAngularScript(ResponseWriter writer, FacesContext context) throws IOException {
		List<String> beansAsJSon = PuiModelSync.getFacesModel();
		for (String bean : beansAsJSon) {
			writer.write("puiUpdateModel(" + bean + ");");
		}
	}
}

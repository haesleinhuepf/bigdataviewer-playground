/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2023 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package sc.fiji.bdvpg.scijava.services;

import bdv.viewer.SourceAndConverter;
import bvv.vistools.BvvHandle;
import com.google.gson.Gson;
import ij.Prefs;
import net.imglib2.util.Pair;
import org.scijava.plugin.Plugin;
import org.scijava.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.bvv.BvvHandleHelper;
import sc.fiji.bdvpg.bvv.supplier.DefaultBvvSupplier;
import sc.fiji.bdvpg.bvv.supplier.IBvvSupplier;
import sc.fiji.bdvpg.bvv.supplier.SerializableBvvOptions;
import sc.fiji.bdvpg.scijava.services.ui.BvvHandleFilterNode;
import sc.fiji.bdvpg.scijava.services.ui.SourceFilterNode;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.viewer.ViewerHelper;
import sc.fiji.persist.ScijavaGsonHelper;

import javax.swing.tree.DefaultTreeModel;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static sc.fiji.bdvpg.bvv.BvvHandleHelper.LAST_ACTIVE_BVVH_KEY;

/**
 * SciJava Service which handles the Display of SourceAndConverters in one
 * or multiple BVV Windows Pairs with SourceAndConverterService, but this
 * service is optional Handling multiple Sources displayed in potentially
 * multiple BVV Windows Make its best to keep in synchronizations all of this,
 * without creating errors nor memory leaks
 */

@SuppressWarnings("unused") // Because SciJava parameters are filled through
														// reflection
@Plugin(type = Service.class, headless = false)
public class BvvService extends ViewerService<BvvHandle>
{

	protected static final Logger logger = LoggerFactory.getLogger(
		BvvService.class);

	Supplier<BvvHandle> bvvSupplier;

	@Override
	public void setDefaultViewerSupplier(Supplier<BvvHandle> bvvSupplier) {
		if (bvvSupplier instanceof IBvvSupplier) {
			setDefaultViewerSupplier((IBvvSupplier) bvvSupplier);
		} else {
			throw new UnsupportedOperationException(bvvSupplier+" is not a "+IBvvSupplier.class.getSimpleName()+" object");
		}
	}

	@Override
	public Class<BvvHandle> getViewerType() {
		return BvvHandle.class;
	}

	/**
	 * Can be used to change how Bdv Windows are created
	 * 
	 * @param bvvSupplier supplier of bdv window
	 */
	public void setDefaultViewerSupplier(IBvvSupplier bvvSupplier) {
		this.bvvSupplier = bvvSupplier;

		logger.info(" --- Serializing to save default bdv window of class " +
			bvvSupplier.getClass().getSimpleName());
		Gson gson = ScijavaGsonHelper.getGson(ctx, true);
		String bvvSupplierSerialized = gson.toJson(bvvSupplier, IBvvSupplier.class);
		logger.info("Bdv Supplier serialized into : " + bvvSupplierSerialized);
		// Saved in prefs for next session
		Prefs.set("bigvolumeviewer.playground.supplier", bvvSupplierSerialized);
	}

	@Override
	public BvvHandle getNewViewer() {

		if (bvvSupplier == null) {
			logger.debug(" --- Fetching or generating default bdv window");
			Gson gson = ScijavaGsonHelper.getGson(ctx);
			String defaultBvvViewer = gson.toJson(new DefaultBvvSupplier(
				new SerializableBvvOptions()), IBvvSupplier.class);
			String bvvSupplierJson = Prefs.get("bigvolumeviewer.playground.supplier",
					defaultBvvViewer);
			try {
				bvvSupplier = gson.fromJson(bvvSupplierJson, IBvvSupplier.class);
			}
			catch (Exception e) {
				e.printStackTrace();
				logger.info("Restoring default bvv supplier");
				bvvSupplier = new DefaultBvvSupplier(new SerializableBvvOptions());
				String bvvSupplierSerialized = gson.toJson(bvvSupplier,
					IBvvSupplier.class);
				logger.debug("Bvv Supplier serialized into : " + bvvSupplierSerialized);
				// Saved in prefs for next session
				Prefs.set("bigvolumeviewer.playground.supplier", bvvSupplierSerialized);
			}
		}

		BvvHandle bvvh = bvvSupplier.get();
		this.registerViewer(bvvh); // We always want it to be registered
		return bvvh;
	}

	/**
	 * @return the last active BDV or create a new one
	 */
	@Override
	public BvvHandle getActiveViewer() {
		List<BvvHandle> bvvhs = os.getObjects(getViewerType());
		if ((bvvhs == null) || (bvvhs.size() == 0)) {
			return getNewViewer();
		}

		if (bvvhs.size() == 1) {
			return bvvhs.get(0);
		}
		else {
			// Get the one with the most recent focus ?
			Optional<BvvHandle> bvvh = bvvhs.stream().filter(b -> b.getViewerPanel()
				.hasFocus()).findFirst();
			if (bvvh.isPresent()) {
				return bvvh.get();
			}
			else {
				if (cacheService.get(LAST_ACTIVE_BVVH_KEY) != null) {
					WeakReference<BvvHandle> wr_bdv_h =
						(WeakReference<BvvHandle>) cacheService.get(LAST_ACTIVE_BVVH_KEY);
					return wr_bdv_h.get();
				}
				else {
					return null;
				}
			}
		}
	}

	/**
	 * Makes visible or invisible a source, applies this to all bdvs according to
	 * BvvhReferences
	 * 
	 * @param sac source
	 * @param visible whether to set it visible
	 */
	@Override
	public void setVisible(SourceAndConverter<?> sac, boolean visible) {
		getViewersOf(sac).forEach(bvvh -> bvvh.getViewerPanel().state()
			.setSourceActive(sac, visible));
	}

	/**
	 * @param sac source to display
	 * @param bvvh bdv window where to check this property
	 * @return true if the source is visible
	 */
	@Override
	public boolean isVisible(SourceAndConverter<?> sac, BvvHandle bvvh) {
		return bvvh.getViewerPanel().state().isSourceActive(sac);
	}

	/**
	 * Displays a BDV SourceAndConverter into the specified BvvHandle This
	 * function really is the core of this service It mimics or copies the
	 * functions of BdvVisTools because it is responsible to create converter,
	 * volatiles, converter setups and so on
	 * 
	 * @param sacs sources to display
	 * @param visible whether to make the source active (=visible)
	 * @param bvvh bvvhandle to append the sources
	 */
	@Override
	public void show(BvvHandle bvvh, boolean visible,
		SourceAndConverter<?>... sacs)
	{

		List<SourceAndConverter<?>> sacsToDisplay = new ArrayList<>();

		for (SourceAndConverter<?> sac : sacs) {
			if (!sourceAndConverterService.isRegistered(sac)) {
				sourceAndConverterService.register(sac);
			}

			boolean escape = false;

			if (bvvh.getViewerPanel().state().getSources().contains(sac)) {
				escape = true;
			}

			// Do not display 2 times the same source and converter
			if (sacsToDisplay.contains(sac)) {
				escape = true;
			}

			if (!escape) {
				sacsToDisplay.add(sac);
				bvvh.getConverterSetups().put(sac, sourceAndConverterService
					.getConverterSetup(sac));
			}
		}

		// Actually display the sources -> repaint called only once!
		bvvh.getViewerPanel().state().addSources(sacsToDisplay);
		// And make them active
		bvvh.getViewerPanel().state().setSourcesActive(sacsToDisplay, visible);
	}

	/**
	 * Removes a SourceAndConverter from all BvvHandle displaying this
	 * SourceAndConverter Updates all references of other Sources present
	 * 
	 * @param sacs sources to remove
	 */
	@Override
	public void removeFromAllViewers(SourceAndConverter<?>... sacs) {
		getViewersOf(sacs).forEach(bvv -> bvv.getViewerPanel().state()
			.removeSources(Arrays.asList(sacs)));
	}

	/**
	 * Removes a SourceAndConverter from the active Bdv Updates all references of
	 * other Sources present
	 * 
	 * @param sacs sources to remove from active bdv
	 */
	@Override
	public void removeFromActiveViewer(SourceAndConverter<?>... sacs) {
		// This condition avoids creating a window for nothing
		if (os.getObjects(getViewerType()).size() > 0) {
			remove(getActiveViewer(), sacs);
		}
	}

	/**
	 * Removes a SourceAndConverter from a BvvHandle Updates all references of
	 * other Sources present
	 * 
	 * @param bvvh bvvhandle
	 * @param sacs Array of SourceAndConverter
	 */
	@Override
	public void remove(BvvHandle bvvh, SourceAndConverter<?>... sacs) {
		bvvh.getViewerPanel().state().removeSources(Arrays.asList(sacs));
		bvvh.getViewerPanel().requestRepaint();
	}

	/**
	 * Closes appropriately a BvvHandle which means that it updates the callbacks
	 * for ConverterSetups and updates the ObjectService
	 * 
	 * @param bvvh bvvhandle to close
	 */
	@Override
	public void closeViewer(BvvHandle bvvh) {
		os.removeObject(bvvh);
		displayToMetadata.invalidate(bvvh); // enables memory release on GC - even
																				// if it bdv was weekly referenced

		// Fix BigWarp closing issue
		boolean isPaired = pairedViewers.stream().anyMatch(p -> (p.getA() == bvvh) ||
			(p.getB() == bvvh));
		if (isPaired) {
			Pair<BvvHandle, BvvHandle> pair = pairedViewers.stream().filter(p -> (p
				.getA() == bvvh) || (p.getB() == bvvh)).findFirst().get();
			pairedViewers.remove(pair);
			if (pair.getA() == bvvh) {
				closeViewer(pair.getB());
			}
			else {
				closeViewer(pair.getA());
			}
		}
	}

	/**
	 * Registers a SourceAndConverter which has originated from a BvvHandle Useful
	 * for BigWarp where the grid and the deformation magnitude SourceAndConverter
	 * are created into BigWarp
	 * 
	 * @param bvvh bvvhandle fetched for registration
	 */
	@Override
	public void registerSourcesFromViewer(BvvHandle bvvh) {
		bvvh.getViewerPanel().state().getSources().forEach(sac -> {
			if (!sourceAndConverterService.isRegistered(sac)) {
				sourceAndConverterService.register(sac);
			}
		});
	}

	/**
	 * Updates bvvHandles which are displaying at least one of these sacs
	 * Potentially improvement is to check whether the timepoint need an update ?
	 * 
	 * @param sacs sources to update
	 */
	public void updateViewersOf(SourceAndConverter<?>... sacs) {
		getViewersOf(sacs).forEach(bvvHandle -> bvvHandle.getViewerPanel()
			.requestRepaint());
	}

	/**
	 * Returns the list of sacs held within a BvvHandle ( whether they are visible
	 * or not ) List is ordered by index in the BvvHandle - complexification to
	 * implement the mixed projector TODO : Avoid duplicates by returning a Set
	 * 
	 * @param bvvHandle the bvvhandle
	 * @return all sources present in a bvvhandle
	 */
	public List<SourceAndConverter<?>> getSourceAndConverterOf(
		BvvHandle bvvHandle)
	{
		return bvvHandle.getViewerPanel().state().getSources();
	}

	/**
	 * Returns a List of BvvHandle which are currently displaying a sac Returns an
	 * empty set in case the sac is not displayed
	 * 
	 * @param sacs the sources queried
	 * @return all bvvhandles which contain the source
	 */
	@Override
	public Set<BvvHandle> getViewersOf(SourceAndConverter<?>... sacs) {
		if (sacs == null) {
			return new HashSet<>();
		}

		List<SourceAndConverter<?>> sacList = Arrays.asList(sacs);

		return os.getObjects(getViewerType()).stream().filter(bvv -> {
			synchronized (bvv.getViewerPanel().state()) {
				return bvv.getViewerPanel().state().getSources().stream().anyMatch(
					sacList::contains);
			}
		}).collect(Collectors.toSet());

	}

	@Override
	public void registerViewer(BvvHandle bvvh) {
		// ------------ Register BvvHandle in ObjectService
		if (!os.getObjects(getViewerType()).contains(bvvh)) { // adds it only if not
																													// already present in
			// ObjectService
			os.addObject(bvvh);

			// ------------ Renames window to ensure unicity
			String windowTitle = ViewerHelper.getViewerTitle(bvvh.getViewerPanel());
			windowTitle = BvvHandleHelper.getUniqueWindowTitle(os, windowTitle);
			ViewerHelper.setViewerTitle(bvvh.getViewerPanel(), windowTitle);

			// ------------ Event handling in bdv sourceandconverterserviceui
			final SourceAndConverterService sacService =
				(SourceAndConverterService) SourceAndConverterServices
					.getSourceAndConverterService();
			DefaultTreeModel model = sacService.getUI().getTreeModel();
			BvvHandleFilterNode node = new BvvHandleFilterNode(model, windowTitle,
				bvvh);
			node.add(new SourceFilterNode(model, "All Sources", (sac) -> true, true));

			// ------------ Allows to remove the BvvHandle from the objectService when
			// closed by the user
			BvvHandleHelper.setBvvHandleCloseOperation(bvvh, cacheService, this, true,
				() -> sacService.getUI().removeBvvHandleNodes(bvvh));

			((SourceFilterNode) sacService.getUI().getTreeModel().getRoot()).insert(
				node, 0);
		}
	}

}

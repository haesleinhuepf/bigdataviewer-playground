/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2025 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

package sc.fiji.bdvpg.services;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.scijava.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.scijava.adapter.AbstractSpimdataAdapter;
import sc.fiji.persist.ScijavaGsonHelper;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class SourceAndConverterAdapter {

	protected static final Logger logger = LoggerFactory.getLogger(
		SourceAndConverterAdapter.class);

	final File basePath;
	final boolean useRelativePaths; // For ABBA : store everything (spimdata xml and json files) in the same folder

	/*public SourceAndConverterAdapter(Context ctx, File basePath) {
		this.ctx = ctx;
		this.basePath = basePath;
		useRelativePaths = false;
	}*/

	public SourceAndConverterAdapter(Context ctx, File basePath, boolean useRelativePaths) {
		this.ctx = ctx;
		this.basePath = basePath;
		this.useRelativePaths = useRelativePaths;
	}

	public boolean useRelativePaths() {
		return useRelativePaths;
	}

	public File getBasePath() {
		return basePath;
	}

	Map<Integer, SourceAndConverter<?>> idToSac;
	Map<SourceAndConverter<?>, Integer> sacToId;
	Map<Integer, Source<?>> idToSource;
	Map<Source<?>, Integer> sourceToId;

	public final Set<Integer> alreadyDeSerializedSacs = new HashSet<>();
	public final Map<Integer, JsonElement> idToJsonElement = new HashMap<>();

	final Context ctx;

	public Context getScijavaContext() {
		return ctx;
	}

	public static Consumer<String> log = logger::debug;

	public Gson getGson() {
		GsonBuilder builder = ScijavaGsonHelper.getGsonBuilder(ctx, true);

		builder.registerTypeHierarchyAdapter(SourceAndConverter.class,
			new sc.fiji.bdvpg.scijava.adapter.SourceAndConverterAdapter(this))
			.registerTypeHierarchyAdapter(AbstractSpimData.class,
				new AbstractSpimdataAdapter(this));

		return builder.create();

	}

	public synchronized Map<Integer, SourceAndConverter<?>> getIdToSac() {
		return idToSac;
	}

	public synchronized Map<SourceAndConverter<?>, Integer> getSacToId() {
		return sacToId;
	}

	public synchronized Map<Integer, Source<?>> getIdToSource() {
		return idToSource;
	}

	public synchronized Map<Source<?>, Integer> getSourceToId() {
		return sourceToId;
	}

}

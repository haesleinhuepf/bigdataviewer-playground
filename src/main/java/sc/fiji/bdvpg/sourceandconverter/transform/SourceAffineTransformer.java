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

package sc.fiji.bdvpg.sourceandconverter.transform;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.SourceAndConverter;
import net.imglib2.Volatile;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.function.Function;

/**
 * This action applies an AffineTransform onto a SourceAndConverter Both the non
 * volatile and the volatile spimsource, if present, are wrapped Another option
 * could be to check whether the spimsource are already wrapped, and then
 * concatenate the transforms TODO : write this alternative action, or set a
 * transform in place flag in this action Limitation : the affine transform is
 * identical for all timepoints Note : the converters are cloned during this
 * wrapping Another option could have been to use the same converters the
 * transform is passed by value, not by reference, so it cannot be updated later
 * on
 */

public class SourceAffineTransformer<T> implements Runnable,
	Function<SourceAndConverter<T>, SourceAndConverter<T>>
{

	SourceAndConverter<T> sourceIn;

	final AffineTransform3D at3D;

	public SourceAffineTransformer(SourceAndConverter<T> src,
		AffineTransform3D at3D)
	{
		this.sourceIn = src;
		this.at3D = at3D;
	}

	/**
	 * Constructor without any source argument in order to use the functional
	 * interface only
	 * 
	 * @param at3D affine transform 3d
	 */
	public SourceAffineTransformer(AffineTransform3D at3D) {
		this.at3D = at3D;
	}

	@Override
	public void run() {

	}

	public SourceAndConverter<T> get() {
		return apply(sourceIn);
	}

	public SourceAndConverter<T> apply(SourceAndConverter<T> in) {
		SourceAndConverter<T> sac;
		TransformedSource<T> src = new TransformedSource<>(in.getSpimSource());
		src.setFixedTransform(at3D);
		if (in.asVolatile() != null) {
			TransformedSource<? extends Volatile<T>> vsrc = new TransformedSource<>(in
				.asVolatile().getSpimSource(), src);
			SourceAndConverter<? extends Volatile<T>> vout = new SourceAndConverter(
				vsrc, SourceAndConverterHelper.cloneConverter(in.asVolatile()
					.getConverter(), in.asVolatile()));
			sac = new SourceAndConverter<>(src, SourceAndConverterHelper
				.cloneConverter(in.getConverter(), in), vout);
		}
		else {
			sac = new SourceAndConverter<>(src, SourceAndConverterHelper
				.cloneConverter(in.getConverter(), in));
		}
		return sac;
	}

}

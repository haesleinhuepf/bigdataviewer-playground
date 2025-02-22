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

package sc.fiji.bdvpg.bdv.navigate;

import bdv.util.BdvHandle;
import net.imglib2.RealPoint;
import sc.fiji.bdvpg.log.Logger;
import sc.fiji.bdvpg.log.Logs;
import sc.fiji.bdvpg.log.SystemLogger;

/**
 * BigDataViewer Playground Action -- Action which logs the position of the
 * mouse in a {@link BdvHandle} TODO : Apparently, linking resources in test is
 * not good practice
 * ?https://stackoverflow.com/questions/45160647/include-link-to-unit-test-classes-in-javadoc
 * Then separate repo for examples or tests ? Looks painful TODO fix javadoc : "
 * See sc.fiji.bdvpg.bdv.navigate.LogMousePositionDemo for a usage example "
 *
 * @author Robert Haase, MPI CBG
 */
public class PositionLogger implements Runnable {

	private final BdvHandle bdvHandle;
	private final Logger logger;

	public PositionLogger(BdvHandle bdvHandle) {
		this(bdvHandle, new SystemLogger());
	}

	public PositionLogger(BdvHandle bdvHandle, Logger logger) {
		this.bdvHandle = bdvHandle;
		this.logger = logger;
	}

	@Override
	public void run() {
		final RealPoint realPoint = new RealPoint(3);
		bdvHandle.getViewerPanel().getGlobalMouseCoordinates(realPoint);
		logger.out(Logs.BDV + ": Position at Mouse: " + realPoint);
	}
}
